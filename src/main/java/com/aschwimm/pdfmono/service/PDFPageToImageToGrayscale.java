package com.aschwimm.pdfmono.service;

import com.aschwimm.pdfmono.util.ColorConverter;
import com.aschwimm.pdfmono.util.PDFDocumentIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;





public class PDFPageToImageToGrayscale {

    private final PDFDocumentIO pdfDocumentIO;
    // Constructor injection of document loader dependency
    public PDFPageToImageToGrayscale(PDFDocumentIO pdfDocumentIO) {
        this.pdfDocumentIO = pdfDocumentIO;
    }

    /*
    Converts pages in a document to RGB images, applies grayscale conversion, then constructs a new PDDocument from these images that's created at specified output path
     */
    public void convertToGrayscalePDF(String inputPdfPath, String outputPdfPath, float dpi) throws Exception {
        File input = new File(inputPdfPath);
        File output = new File(outputPdfPath);
        if (!input.exists() || !input.isFile()) {
            throw new IllegalArgumentException("Input file does not exist or is not a valid file: " + inputPdfPath);
        }

        if (!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            throw new IOException("Output directory does not exist and could not be created: " + output.getParentFile().getAbsolutePath());
        }

        try (PDDocument document = pdfDocumentIO.loadDocument(inputPdfPath); PDDocument outputDocument = new PDDocument()) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {

                System.out.printf("\rProcessing page %d of %d...          ", (i + 1), pageCount);
                System.out.flush();
                    // 1. Assigned the current page rendered as an RGB image
                    BufferedImage image = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                    // 2. Convert image to grayscale
                    BufferedImage grayScaleImage = convertToGrayscaleWithGamma(image);
                    // 3. Get current page and retrieve its dimensions
                    PDPage originalPage = document.getPage(i);
                    PDRectangle originalPageSize = originalPage.getMediaBox();
                    // 4. Create new page of a size that matches original page and add it to output PDDocument document
                    PDPage newPage = new PDPage(originalPageSize);
                    outputDocument.addPage(newPage);
                    // 5. Now that a page has been created and added to the document, an ImageXObject is created from the BufferedImage then linked to output document's Resource dictionary
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                            outputDocument,
                            bufferedImageToByteArray(grayScaleImage, "JPEG"),
                            "grayscale_page_" + (i + 1)
                    );

                    // 6. Image is added to page with a reference to the image in document's Resource dictionary
                    try (PDPageContentStream contentStream = new PDPageContentStream(outputDocument, newPage)) {
                        // Scale image to fit the page exactly
                        contentStream.drawImage(pdImage, 0, 0,
                                originalPageSize.getWidth(),
                                originalPageSize.getHeight());
                    }

                    // Clean up memory
                    image.flush();
                    grayScaleImage.flush();
            }
            System.out.println();
            outputDocument.save(outputPdfPath);
            System.out.println("All pages processed. Output document saved.");
        } catch (IOException e) {
            throw new IOException("Failed to convert PDF due to I/O error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new Exception("An error occurred during page rendering or image processing: " + e.getMessage(), e);
        }
    }

    // Helper method to convert BufferedImage to byte array
    private static byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
        try (java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, format, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }

    /*
    Images are converted to grayscale with gamma correction to prevent colors converted to grayscale from becoming too dark
    Conversion is done pixel by pixel in parallel
     */
    private static BufferedImage convertToGrayscaleWithGamma(BufferedImage originalImage) {
        BufferedImage grayscaleImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );
        // Default gamma value
        double gamma = 2.2;
        // Inverse gamma used to linearize the luminance by removing gamma intensity from grayscale values
        double invGamma = 1.0 / gamma;

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Process rows in parallel for better performance on large images
        IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                // RGB is a packed 32-bit ARGB pixel (alpha-red-green-blue)
                int rgb = originalImage.getRGB(x, y);
                // Shift right 16 bits placing red value in lowest 8 bits, then mask lowest 8 bits discarding alpha value
                int red = (rgb >> 16) & 0xFF;
                // Shift right 8 bits placing green value in lowest 8 bits, then mask lowest bits discarding alpha and red values
                int green = (rgb >> 8) & 0xFF;
                // Blue is already in the lowest 8 bits, masking discards alpha, red and green values
                int blue = rgb & 0xFF;
                // Apply standard grayscale conversion and normalize to 0-1 for normalized intensity
                double gray = (ColorConverter.rgbToGray(red,green,blue)) / 255.0;
                // Removes gamma encoding from the intensity by raising to the power of
                gray = Math.pow(gray, invGamma);
                // Convert back into 8 bit range with bounds for overflow and underflow
                int grayValue = (int) Math.min(255, Math.max(0, gray * 255));
                // Convert gray values back into RGB integer by shifting the gray value into each 8 bit position, alpha channel is ignored and is set to 0
                int grayRGB = (grayValue << 16) | (grayValue << 8) | grayValue;
                // Grayscale RGB pixel replaces colored RGB pixel in pixel array
                grayscaleImage.setRGB(x, y, grayRGB);
            }
        });

        return grayscaleImage;
    }
}