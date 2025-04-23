package com.aschwimm.pdfmono.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

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
            int pageCount = document.getNumberOfPages();
            List<BufferedImage> processedImages = new ArrayList<>();

            for (int i = 0; i < pageCount; i++) {
                BufferedImage bwImage = convertPageToBlackAndWhite(document, i, 300);
                processedImages.add(bwImage);
            }

            PDDocument outputDocument = createDocumentFromImages(processedImages);
            saveDocument(outputDocument, outputFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error during conversion: " + e.getMessage());
            return false;
        }
    }

    private PDDocument loadDocument(String inputFile) throws IOException {

        try{
            File file = new File(inputFile);
            return PDDocument.load(file);
        } catch(IOException e) {
            System.err.println("Error loading PDF: " + e.getMessage());
            throw e;
        }
    }
    private BufferedImage convertPageToBlackAndWhite(PDDocument document, int pageIndex, float dpi) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage colorImage = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
        return applyBlackAndWhiteConversion(colorImage);
    }
    private BufferedImage applyBlackAndWhiteConversion(BufferedImage colorImage) {
        BufferedImage grayImage = new BufferedImage(
                colorImage.getWidth(),
                colorImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(colorImage, 0, 0, null);
        g2d.dispose();
        return grayImage;
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
