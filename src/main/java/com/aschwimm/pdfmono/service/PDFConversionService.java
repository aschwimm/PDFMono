package com.aschwimm.pdfmono.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.color.ColorSpace;
import java.awt.image.ColorConvertOp;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

public class PDFConversionService {
    public boolean convertToBlackAndWhite(String inputFile, String outputFile) {
        File input = new File(inputFile);
        File output = new File(outputFile);

        if (!input.exists() || !input.isFile()) {
            System.err.println("Input file does not exist or is not a valid file.");
            return false;
        }

        if (!output.getParentFile().exists()) {
            System.err.println("Output directory does not exist.");
            return false;
        }

        try (PDDocument document = loadDocument(inputFile)) {
            for(PDPage page : document.getPages()) {
                convertPageToGrayscale(document, page);
                convertEmbeddedImagesToGrayscale(document, page);
            }
            saveDocument(document, outputFile);
            return true;
        } catch (IOException e) {
            System.err.println("Error during conversion: " + e.getMessage());
            return false;
        }
    }
    // Update how PDF files are loaded in PDFBox 3.0.5
    private PDDocument loadDocument(String inputFile) throws IOException {

        try{
            File file = new File(inputFile);
            return Loader.loadPDF(new RandomAccessReadBufferedFile(file));
        } catch(IOException e) {
            System.err.println("Error loading PDF: " + e.getMessage());
            throw e;
        }
    }
    // This will only convert vector-based content like text, not inline vector paths like vector images in the content stream
   private void convertPageToGrayscale(PDDocument document, PDPage page) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(page);
        List<Object> tokens = parser.parse();
        List<Object> newTokens = new ArrayList<>();
        PDResources resources = page.getResources();
        COSDictionary colorSpace = resources.getCOSObject().getCOSDictionary(COSName.COLORSPACE);
        for (COSName colorName : colorSpace.keySet()) {
           COSBase value = colorSpace.getItem(colorName);
           if (value instanceof COSObject) {
               value = ((COSObject) value).getObject();
           }

           if (value instanceof COSArray array) {
               System.out.println("Color space name: " + colorName);
               for (int i = 0; i < array.size(); i++) {
                   COSBase element = array.get(i);
                   if (element instanceof COSObject obj) {
                       element = obj.getObject();
                   }

                   System.out.println("  Index " + i + ": " + element.getClass().getSimpleName() + " -> " + element);
               }
           }
       }



       for (int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);
            if(token instanceof Operator operator) {
                String name = operator.getName();

                if(name.equals("rg") || name.equals("RG")) {
                    float r = ((COSNumber) newTokens.remove(newTokens.size() - 3)).floatValue();
                    float g = ((COSNumber) newTokens.remove(newTokens.size() - 2)).floatValue();
                    float b = ((COSNumber) newTokens.remove(newTokens.size() - 1)).floatValue();

                    float gray = 0.299f * r + 0.587f * g + 0.114f * b;

                    newTokens.add(COSFloat.get(String.valueOf(gray)));
                    newTokens.add(Operator.getOperator(name.equals("rg") ? "g" : "G"));
                }
                else if (name.equals("k") || name.equals("K")) {
                    if (i >= 4 &&
                            tokens.get(i - 4) instanceof COSNumber c &&
                            tokens.get(i - 3) instanceof COSNumber m &&
                            tokens.get(i - 2) instanceof COSNumber y &&
                            tokens.get(i - 1) instanceof COSNumber k) {

                        float cVal = c.floatValue();
                        float mVal = m.floatValue();
                        float yVal = y.floatValue();
                        float kVal = k.floatValue();

                        boolean isWhite = cVal <= 0.001f && mVal <= 0.001f && yVal <= 0.001f && kVal <= 0.001f;

                        if (isWhite) {
                            newTokens.addAll(List.of(c, m, y, k));
                            newTokens.add(operator);
                            continue;
                        }

                        float[] rgb = PDDeviceCMYK.INSTANCE.toRGB(new float[] { cVal, mVal, yVal, kVal });
                        float gray = 0.3f * rgb[0] + 0.59f * rgb[1] + 0.11f * rgb[2];

                        newTokens.add(COSFloat.get(String.valueOf(gray)));
                        newTokens.add(Operator.getOperator(name.equals("k") ? "g" : "G"));
                    }
                }
                else {
                    newTokens.add(token);
                }
            } else {
                newTokens.add(token);
            }
        }
       PDStream newStream = new PDStream(document);
        try (OutputStream out = newStream.createOutputStream()) {
            ContentStreamWriter writer = new ContentStreamWriter(out);
            writer.writeTokens(newTokens);
        }

        page.setContents(newStream);

   }
    private void convertEmbeddedImagesToGrayscale(PDDocument document, PDPage page) throws IOException {
        PDResources resources = page.getResources();
        if(resources != null) {
            processResourcesForImages(resources, document);
        }
    }

    private void processResourcesForImages(PDResources resources, PDDocument document) throws IOException {
        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);

            if(xObject instanceof PDImageXObject imageXObject) {
                BufferedImage colorImage = imageXObject.getImage();
                BufferedImage grayImage = new BufferedImage(
                        colorImage.getWidth(),
                        colorImage.getHeight(),
                        BufferedImage.TYPE_BYTE_GRAY
                );
                Graphics2D graphics = grayImage.createGraphics();
                graphics.drawImage(colorImage, 0, 0, null);
                graphics.dispose();

                PDImageXObject grayImageXObject = LosslessFactory.createFromImage(document,grayImage);
                resources.put(xObjectName, grayImageXObject);
            }
            if(xObject instanceof PDFormXObject formXObject) {
                PDResources nestedResources = formXObject.getResources();
                if(nestedResources != null) {
                    processResourcesForImages(nestedResources, document);
                }

            }
        }
    }
    private void saveDocument(PDDocument document, String outputPath) throws IOException {
        try (document) {
            document.save(outputPath);
        }
    }

}
