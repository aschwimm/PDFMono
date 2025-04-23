package com.aschwimm.pdfmono;

import com.aschwimm.pdfmono.service.PDFConversionService;

public class PDFMono {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java -jar pdfmono.jar <input.pdf> <output.pdf>");
            return;
        }
        String inputPath = args[0];
        String outputPath = args[1];
        PDFConversionService conversionService = new PDFConversionService();
        boolean success = conversionService.convertToBlackAndWhite(inputPath, outputPath);
        if(success) {
            System.out.println("PDF converted successfully");
        } else {
            System.out.println("PDF conversion failed");
        }
    }
}
