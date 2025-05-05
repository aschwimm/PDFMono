package com.aschwimm.pdfmono.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.color.ColorSpace;
import java.awt.image.ColorConvertOp;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
            }
            saveDocument(document, outputFile);
            return true;
        } catch (IOException e) {
            System.err.println("Error during conversion: " + e.getMessage());
            return false;
        }
    }
    // PDDocument load() method is replaced with Loader class to separate concerns
    private PDDocument loadDocument(String inputFile) throws IOException {

        try{
            File file = new File(inputFile);
            return Loader.loadPDF(file);
        } catch(IOException e) {
            System.err.println("Error loading PDF: " + e.getMessage());
            throw e;
        }
    }
   private void convertPageToGrayscale(PDDocument document, PDPage page) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(page);
        List<Object> tokens = parser.parse();
        List<Object> newTokens = new ArrayList<>();

        for (Object token : tokens) {
            if(token instanceof Operator operator) {
                String name = operator.getName();

                if(name.equals("rg") || name.equals("RG")) {
                    float r = ((COSNumber) newTokens.remove(newTokens.size() - 3)).floatValue();
                    float g = ((COSNumber) newTokens.remove(newTokens.size() - 2)).floatValue();
                    float b = ((COSNumber) newTokens.remove(newTokens.size() - 1)).floatValue();

                    float gray = 0.299f * r + 0.587f * g + 0.114f * b;

                    newTokens.add(COSFloat.get(String.valueOf(gray)));
                    newTokens.add(Operator.getOperator(name.equals("rg") ? "g" : "G"));
                } else {
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
    private PDDocument createDocumentFromImages(List<BufferedImage> images) throws IOException {
        PDDocument document = new PDDocument();
        for(BufferedImage image : images) {
            PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
            document.addPage(page);

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 0,0);
            }
        }
        return document;
    }
    private void saveDocument(PDDocument document, String outputPath) throws IOException {
        try (document) {
            document.save(outputPath);
        }
    }

}
