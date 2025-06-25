package com.aschwimm.pdfmono;

import com.aschwimm.pdfmono.service.PDFConversionService;
import com.aschwimm.pdfmono.service.PDFPageToImageToGrayscale;
import com.aschwimm.pdfmono.util.PDFDocumentIO;
import com.aschwimm.pdfmono.util.PDFInspector;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import static java.lang.Float.parseFloat;

public class PDFMono {

    // Set default DPI if arg isn't provided
    private static final float DEFAULT_DPI = 150.0f;

    // Conversion usage reminder message
    private static final String CONVERSION_USAGE =
            "Usage: java -jar PDFMono.jar <input-pdf-path> <output-pdf-path> --grayscale [--dpi <value>]";
    // Inspection usage reminder message
    private static final String INSPECTION_USAGE =
            "Usage: java -jar PDFMono.jar <input-pdf-path> <output-Markdown-report-path>";

    public static void main(String[] args) {
        String inputPath = null;
        String outputPath = null;
        float dpi = DEFAULT_DPI;

        // Parse cmd line arguments
        if (args.length < 2) {
            System.err.println("Error: Missing input and/or output file paths.");
            System.out.println(CONVERSION_USAGE);
            System.exit(1);
        }
        inputPath = args[0];
        outputPath = args[1];
        // Process optional arguments from the third argument onwards
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            // Check for arg to convert to grayscale
            if(arg.equals("--grayscale")) {
                // Check for option --dpi arg
                if(i+1 < args.length) {
                    String dpiArg =  args[i+1];
                    if(dpiArg.equals("--dpi")) {
                        if (i + 2 < args.length) {
                            try {
                                // Increment i to consume the value
                                ++i;
                                dpi = Float.parseFloat(args[++i]);
                                if (dpi <= 0) {
                                    System.err.println("Error: DPI value must be a positive number.");
                                    System.out.println(CONVERSION_USAGE);
                                    System.exit(1);
                                }
                            } catch (NumberFormatException e) {
                                System.err.println("Error: Invalid DPI value. Please provide an integer or float.");
                                System.out.println(CONVERSION_USAGE);
                                System.exit(1);
                            }
                        } else {
                            System.err.println("Error: --dpi option requires a value.");
                            System.out.println(CONVERSION_USAGE);
                            System.exit(1);
                        }
                        try {
                            Paths.get(inputPath);
                            Paths.get(outputPath);
                        } catch (InvalidPathException e) {
                            System.err.println("Error: Invalid file path provided. " + e.getMessage());
                            System.out.println(CONVERSION_USAGE);
                            System.exit(1);
                        }

                        PDFDocumentIO pdfDocumentIO = new PDFDocumentIO();
                        PDFPageToImageToGrayscale converter = new PDFPageToImageToGrayscale(pdfDocumentIO);
                        System.out.println("Converting '" + inputPath + "' to grayscale with DPI " + dpi + "...");
                        try {
                            // Call the method without the gamma boolean
                            converter.convertToGrayscalePDF(inputPath, outputPath, dpi);
                        } catch (IllegalArgumentException e) {
                            System.err.println("\nInput Error: " + e.getMessage());
                            System.out.println(CONVERSION_USAGE);
                            System.exit(1);
                        } catch (IOException e) {
                            System.err.println("\nI/O Error during PDF conversion: " + e.getMessage());
                            e.printStackTrace();
                            System.exit(1);
                        } catch (Exception e) {
                            System.err.println("\nAn unexpected error occurred during PDF conversion: " + e.getMessage());
                            e.printStackTrace();
                            System.exit(1);
                        } finally {
                            System.out.println("Conversion complete! Output saved to: " + outputPath);
                            System.exit(0);
                        }
                    }
                }
                PDFDocumentIO pdfDocumentIO = new PDFDocumentIO();
                PDFPageToImageToGrayscale converter = new PDFPageToImageToGrayscale(pdfDocumentIO);
                System.out.println("Converting '" + inputPath + "' to grayscale with Default DPI: " + dpi + "...");
                try {
                    // Call the method without the gamma boolean
                    converter.convertToGrayscalePDF(inputPath, outputPath, dpi);
                    System.out.println("Conversion complete! Output saved to: " + outputPath);
                } catch (IllegalArgumentException e) {
                    System.err.println("\nInput Error: " + e.getMessage());
                    System.out.println(CONVERSION_USAGE);
                    System.exit(1);
                } catch (IOException e) {
                    System.err.println("\nI/O Error during PDF conversion: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                } catch (Exception e) {
                    System.err.println("\nAn unexpected error occurred during PDF conversion: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            else if (arg.equals("--inspect")) {
                // Temporary solution: concat .md file extension for outputPath arg until PDFInspector class is updated
                // TODO: PDFInspector should handle file extensions not main
                PDFDocumentIO pdfDocumentIO = new PDFDocumentIO();
                PDFInspector inspector = new PDFInspector(pdfDocumentIO);
                outputPath += ".md";
                System.out.println("Inspecting " + inputPath + "...");
                try {
                    inspector.inspect(inputPath, outputPath);
                    System.out.println("Inspection complete! Report saved to: " + outputPath);
                } catch (IllegalArgumentException e) {
                    System.err.println("\nInput Error: " + e.getMessage());
                    System.out.println(CONVERSION_USAGE);
                    System.exit(1);
                } catch (Exception e) {
                    System.err.println("\nAn unexpected error occurred during PDF inspection: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            else {
                System.err.println("Error: Unknown option '" + arg + "'");
                System.out.println(CONVERSION_USAGE);
                System.exit(1);
            }
        }


    }
}
