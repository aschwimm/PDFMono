package com.aschwimm.pdfmono.service;

import com.aschwimm.pdfmono.util.ColorConverter;
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
import org.apache.pdfbox.pdmodel.common.COSDictionaryMap;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.common.function.PDFunction;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType4;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.*;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorConvertOp;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.IntStream;

public class PDFConversionService {
    private Stack<PDColorSpace> nonStrokingColorSpaceStack;
    private Stack<PDColorSpace> strokingColorSpaceStack;
    int csCount = 0;

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
                convertSeparationColorSpaceToGray(document, page);
            }

            document.save(outputFile);
            return true;
        } catch (IOException e) {
            System.err.println("Error during conversion: " + e.getMessage());
            return false;
        }

    }

    // This will only convert vector-based content like text, not inline vector paths like vector images in the content stream
   private void convertPageToGrayscale(PDDocument document, PDPage page) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(page);
        List<Object> tokens = parser.parse();
        List<Object> newTokens = convertInlineColorsToGray(tokens, page);
        PDStream newStream = new PDStream(document);
        try (OutputStream out = newStream.createOutputStream()) {
            ContentStreamWriter writer = new ContentStreamWriter(out);
            writer.writeTokens(newTokens);
        }
        page.setContents(newStream);

   }
   /*
   Work in progress. The method works by converting alternate colorspaces for /Separation type colorspace objects
   into DeviceGray
    */
   private void convertSeparationColorSpaceToGray(PDDocument document, PDPage page) throws IOException {
        PDResources resources = page.getResources();
        Iterable<COSName> csNames = resources.getColorSpaceNames();
        List<COSName> toDelete = new ArrayList<>();
        Map<COSName, PDColorSpace> colorSpaceMap = new HashMap<>();
        for (COSName csName : csNames) {
            PDColorSpace colorSpace = resources.getColorSpace(csName);
            colorSpaceMap.put(csName, colorSpace);
            toDelete.add(csName);
        }

       for (Map.Entry<COSName, PDColorSpace> entry : colorSpaceMap.entrySet()) {
           COSName name = entry.getKey();
           String strName = name.getName();
           System.out.println(strName);
           PDColorSpace cs = entry.getValue();


           if (cs instanceof PDSeparation) {
               PDSeparation sep = (PDSeparation) cs;


               sep.setAlternateColorSpace(PDDeviceGray.INSTANCE);


               COSDictionary functionDict = new COSDictionary();
               functionDict.setInt(COSName.FUNCTION_TYPE, 4);

               COSArray domain = new COSArray();
               domain.add(COSInteger.ZERO);
               domain.add(COSInteger.ONE);
               functionDict.setItem(COSName.DOMAIN, domain);

               COSArray range = new COSArray();
               range.add(COSInteger.ZERO);
               range.add(COSInteger.ONE);
               functionDict.setItem(COSName.RANGE, range);


               String psCode = "{ 0.755 mul 1 exch sub }";
               byte[] psBytes = psCode.getBytes(StandardCharsets.US_ASCII);
               functionDict.setItem(COSName.LENGTH, COSInteger.get(psBytes.length));

               COSStream stream = document.getDocument().createCOSStream();
               stream.addAll(functionDict);
               try (OutputStream os = stream.createOutputStream(COSName.FLATE_DECODE)) {
                   os.write(psBytes);
               }

               PDFunctionType4 tintTransform = new PDFunctionType4(stream);
               sep.setTintTransform(tintTransform);
           }

       }
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
    private List<Object> convertInlineColorsToGray(List<Object> tokens, PDPage page) throws IOException {
        List<Object> newTokens = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);
            if(token instanceof Operator operator) {
                String name = operator.getName();

                if(name.equals("rg") || name.equals("RG")) {
                    int r = ((COSNumber) newTokens.remove(newTokens.size() - 3)).intValue();
                    int g = ((COSNumber) newTokens.remove(newTokens.size() - 2)).intValue();
                    int b = ((COSNumber) newTokens.remove(newTokens.size() - 1)).intValue();

                    float gray = ColorConverter.rgbToGray(r,g,b);

                    newTokens.add(COSFloat.get(String.valueOf(gray)));
                    newTokens.add(Operator.getOperator(name.equals("rg") ? "g" : "G"));
                }
                else if (name.equals("k") || name.equals("K")) {
                    if (i >= 4 &&
                            tokens.get(i - 4) instanceof COSNumber c &&
                            tokens.get(i - 3) instanceof COSNumber m &&
                            tokens.get(i - 2) instanceof COSNumber y &&
                            tokens.get(i - 1) instanceof COSNumber k) {

                        newTokens.remove(newTokens.size() - 1); // k
                        newTokens.remove(newTokens.size() - 1); // y
                        newTokens.remove(newTokens.size() - 1); // m
                        newTokens.remove(newTokens.size() - 1); // c

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
                        float gray = ColorConverter.rgbToGray(rgb);

                        newTokens.add(COSFloat.get(String.valueOf(gray)));
                        newTokens.add(Operator.getOperator(name.equals("k") ? "g" : "G"));
                    }
                }
                else if (name.equals("scn") && tokens.get(i - 1) instanceof COSNumber tint) {
                    newTokens.remove(newTokens.size() - 1);
                    float tintVal = tint.floatValue();
//                    if(tintVal == 0) {
//                        tintVal = 1f;
//
//                    }
                    newTokens.add(COSInteger.get(String.valueOf(tintVal)));
                    newTokens.add(Operator.getOperator("scn"));
                }
                else {
                    newTokens.add(token);
                }
            } else {
                newTokens.add(token);
            }
        }
        return newTokens;
    }
}
