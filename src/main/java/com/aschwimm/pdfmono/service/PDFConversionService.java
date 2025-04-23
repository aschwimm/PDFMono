package com.aschwimm.pdfmono.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PDFConversionService {
    public boolean convertToBlackAndWhite(String inputFile, String outputFile) {
        return false;
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
