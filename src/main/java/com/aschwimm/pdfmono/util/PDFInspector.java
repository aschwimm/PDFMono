package com.aschwimm.pdfmono.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.cos.*;

import java.io.*;
import java.util.List;
import java.util.Map;

public class PDFInspector {

    public void inspect(String inputPath, String outputLogPath) {
        try (PDDocument document = loadDocument(inputPath);
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputLogPath))) {

            writer.write("# PDF Inspection Report\n\n");
            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);
                writer.write("## Page " + (i + 1) + "\n");


                PDResources resources = page.getResources();

                inspectResources(resources, writer, 1);
            }

            System.out.println("Inspection complete. Output written to: " + outputLogPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void inspectResources(PDResources resources, BufferedWriter writer, int indentLevel) throws IOException {
        if (resources == null) return;

        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);

            if (xObject instanceof PDImageXObject image) {
                logImageInfo(writer, name.getName(), image, indentLevel);
            } else if (xObject instanceof PDFormXObject form) {
                writer.write(indent(indentLevel) + "- Form XObject: " + name.getName() + "\n");
                inspectResources(form.getResources(), writer, indentLevel + 1);
            }
        }
    }

    private void inspectContents(PDPage page) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(page);
        List<Object> tokens = parser.parse();
        for (Object token : tokens) {
            if (token instanceof Operator operator) {
                String name = operator.getName();
                if(name.equals("q") || name.equals("Q")) {

                }
            }
        }


    }

    private void logImageInfo(BufferedWriter writer, String name, PDImageXObject image, int indentLevel) throws IOException {
        writer.write(indent(indentLevel) + "- Image XObject: " + name + "\n");
        writer.write(indent(indentLevel + 1) + "* Width: " + image.getWidth() + "\n");
        writer.write(indent(indentLevel + 1) + "* Height: " + image.getHeight() + "\n");
        writer.write(indent(indentLevel + 1) + "* ColorSpace: " + image.getColorSpace().getName() + "\n");
        writer.write(indent(indentLevel + 1) + "* BitsPerComponent: " + image.getBitsPerComponent() + "\n");
        writer.write(indent(indentLevel + 1) + "* IsStencil: " + image.isStencil() + "\n");
        writer.write(indent(indentLevel + 1) + "* Suffix: " + image.getSuffix() + "\n");
    }

    private void logVectorPathInfo(BufferedWriter writer, String name, int indentLevel) throws IOException {

    }

    private String indent(int level) {
        return "  ".repeat(level);
    }
    private PDDocument loadDocument(String inputFile) throws IOException {

        try{
            File file = new File(inputFile);
            return Loader.loadPDF(new RandomAccessReadBufferedFile(file));
        } catch(IOException e) {
            System.err.println("Error loading PDF: " + e.getMessage());
            throw e;
        }
    }
}
