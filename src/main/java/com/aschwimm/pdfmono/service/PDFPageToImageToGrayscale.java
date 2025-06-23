package com.aschwimm.pdfmono.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static com.aschwimm.pdfmono.service.PDFConversionService.loadDocument;
import static com.aschwimm.pdfmono.service.PDFConversionService.saveDocument;


public class PDFPageToImageToGrayscale {

    public static boolean convertToGrayscalePDF(String inputPdfPath, String outputPdfPath, float dpi) {
        File input = new File(inputPdfPath);
        File output = new File(outputPdfPath);
        if (!input.exists() || !input.isFile()) {
            System.err.println("Input file does not exist or is not a valid file.");
            return false;
        }

        if (!output.getParentFile().exists()) {
            System.err.println("Output directory does not exist.");
            return false;
        }

        PDDocument outputDocument = null;
        try (PDDocument document = loadDocument(inputPdfPath)) {
            outputDocument = new PDDocument();
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            System.out.println("Converting " + pageCount + " pages to grayscale at " + dpi + " DPI...");

            for (int i = 0; i < pageCount; i++) {
                try {
                    // FIXED: Use renderImageWithDPI instead of renderImage
                    BufferedImage image = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                    BufferedImage grayScaleImage = convertToGrayscaleWithGamma(image);

                    PDPage originalPage = document.getPage(i);
                    PDRectangle originalPageSize = originalPage.getMediaBox();

                    PDPage newPage = new PDPage(originalPageSize);
                    outputDocument.addPage(newPage);

                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                            outputDocument,
                            bufferedImageToByteArray(grayScaleImage, "PNG"),
                            "grayscale_page_" + (i + 1)
                    );

                    // Add image to the page
                    try (PDPageContentStream contentStream = new PDPageContentStream(outputDocument, newPage)) {
                        // Scale image to fit the page exactly
                        contentStream.drawImage(pdImage, 0, 0,
                                originalPageSize.getWidth(),
                                originalPageSize.getHeight());
                    }

                    // Clean up memory
                    image.flush();
                    grayScaleImage.flush();

                    System.out.println("Processed page " + (i + 1) + " of " + pageCount);

                } catch (Exception e) {
                    System.err.println("Error processing page " + (i + 1) + ": " + e.getMessage());
                }
            }

            // FIXED: Save outputDocument instead of input document
            saveDocument(outputDocument, outputPdfPath);
            return true;

        } catch(IOException e) {
            System.err.println("Error during conversion: " + e.getMessage());
            return false;
        } finally {
            // Clean up output document
            if (outputDocument != null) {
                try {
                    outputDocument.close();
                } catch (IOException e) {
                    System.err.println("Error closing output document: " + e.getMessage());
                }
            }
        }
    }

    // Helper method to convert BufferedImage to byte array
    private static byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, format, baos);
            return baos.toByteArray();
        }
    }

    private static BufferedImage convertToGrayscaleLuminance(BufferedImage originalImage) {
        BufferedImage grayscaleImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        // Brightness adjustment factor (1.0 = no change, >1.0 = brighter, <1.0 = darker)
        float brightnessBoost = 1.2f;

        for (int y = 0; y < originalImage.getHeight(); y++) {
            for (int x = 0; x < originalImage.getWidth(); x++) {
                int rgb = originalImage.getRGB(x, y);

                // Extract RGB components
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Calculate luminance using standard formula
                float gray = (0.299f * red + 0.587f * green + 0.114f * blue);

                // Apply brightness boost
                gray *= brightnessBoost;

                // Clamp to valid range
                int grayValue = Math.min(255, Math.max(0, (int) gray));

                // Create grayscale RGB value
                int grayRGB = (grayValue << 16) | (grayValue << 8) | grayValue;
                grayscaleImage.setRGB(x, y, grayRGB);
            }
        }

        return grayscaleImage;
    }

    // Alternative grayscale conversion methods with brightness adjustments
    private static BufferedImage convertToGrayscaleWithGamma(BufferedImage originalImage) {
        BufferedImage grayscaleImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        // Gamma value increases brightness > 1, decreases brightness < 1
        double gamma = 1.0;
        double invGamma = 1.0 / gamma;

        for (int y = 0; y < originalImage.getHeight(); y++) {
            for (int x = 0; x < originalImage.getWidth(); x++) {
                int rgb = originalImage.getRGB(x, y);

                // Extract RGB components
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Calculate luminance
                double gray = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;

                // Apply gamma correction
                gray = Math.pow(gray, invGamma);

                // Convert back to 0-255 range
                int grayValue = (int) Math.min(255, Math.max(0, gray * 255));

                // Create grayscale RGB value
                int grayRGB = (grayValue << 16) | (grayValue << 8) | grayValue;
                grayscaleImage.setRGB(x, y, grayRGB);
            }
        }

        return grayscaleImage;
    }

    // Method 2: Contrast and brightness adjustment
    private static BufferedImage convertToGrayscaleWithContrast(BufferedImage originalImage) {
        BufferedImage grayscaleImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        // Adjustment parameters
        float brightness = 30f;  // Add this value to brightness (-100 to +100)
        float contrast = 1.2f;   // Multiply contrast (0.5 = low contrast, 2.0 = high contrast)

        for (int y = 0; y < originalImage.getHeight(); y++) {
            for (int x = 0; x < originalImage.getWidth(); x++) {
                int rgb = originalImage.getRGB(x, y);

                // Extract RGB components
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Calculate luminance
                float gray = (0.299f * red + 0.587f * green + 0.114f * blue);

                // Apply contrast (center around 128)
                gray = ((gray - 128) * contrast) + 128;

                // Apply brightness
                gray += brightness;

                // Clamp to valid range
                int grayValue = Math.min(255, Math.max(0, (int) gray));

                // Create grayscale RGB value
                int grayRGB = (grayValue << 16) | (grayValue << 8) | grayValue;
                grayscaleImage.setRGB(x, y, grayRGB);
            }
        }

        return grayscaleImage;
    }

    // Method 3: Histogram equalization for better distribution
    private static BufferedImage convertToGrayscaleWithHistogramEqualization(BufferedImage originalImage) {
        BufferedImage grayscaleImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int totalPixels = width * height;

        // First pass: convert to grayscale and build histogram
        int[] histogram = new int[256];
        int[][] grayValues = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);

                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                grayValues[y][x] = gray;
                histogram[gray]++;
            }
        }

        // Build cumulative distribution function
        int[] cdf = new int[256];
        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }

        // Second pass: apply histogram equalization
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalGray = grayValues[y][x];
                int equalizedGray = (int) ((cdf[originalGray] * 255.0) / totalPixels);

                int grayRGB = (equalizedGray << 16) | (equalizedGray << 8) | equalizedGray;
                grayscaleImage.setRGB(x, y, grayRGB);
            }
        }

        return grayscaleImage;
    }

    // Method 4: Simple averaging with brightness boost
    private static BufferedImage convertToGrayscaleAverage(BufferedImage originalImage) {
        BufferedImage grayscaleImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        float brightnessBoost = 1.3f; // Adjust this value

        for (int y = 0; y < originalImage.getHeight(); y++) {
            for (int x = 0; x < originalImage.getWidth(); x++) {
                int rgb = originalImage.getRGB(x, y);

                // Extract RGB components
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Simple average instead of luminance formula
                float gray = (red + green + blue) / 3.0f;

                // Apply brightness boost
                gray *= brightnessBoost;

                // Clamp to valid range
                int grayValue = Math.min(255, Math.max(0, (int) gray));

                int grayRGB = (grayValue << 16) | (grayValue << 8) | grayValue;
                grayscaleImage.setRGB(x, y, grayRGB);
            }
        }

        return grayscaleImage;
    }
}