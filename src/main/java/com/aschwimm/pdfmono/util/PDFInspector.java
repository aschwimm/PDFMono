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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PDFInspector {

    private static final Set<String> VECTOR_OPERATORS = Set.of(
            "m", "l", "re", "c", "v", "y", "h",
            "S", "s", "f", "F", "f*", "B", "B*", "b", "b*"
    );
    private static final Map<String, String> colorOperatorToColorSpace = Map.ofEntries(
            Map.entry("rg", "DeviceRGB (nonstroking)"),
            Map.entry("RG", "DeviceRGB (stroking)"),
            Map.entry("g",  "DeviceGray (nonstroking)"),
            Map.entry("G",  "DeviceGray (stroking)"),
            Map.entry("k",  "DeviceCMYK (nonstroking)"),
            Map.entry("K",  "DeviceCMYK (stroking)")
    );


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
                inspectContents(page, writer, 1);
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

    private void inspectContents(PDPage page, BufferedWriter writer, int indentLevel) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(page);
        List<Object> tokens = parser.parse();
        Set<String> colorSpaces = new HashSet<>();
        boolean insideFigure = false;
        for(int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);
            if(!insideFigure && token instanceof COSName name && name.getName().equals("Figure")) {
                insideFigure = true;
                System.out.println("Figure found");
                i++;
                // Found the start of a Figure tag
            }
            else if(insideFigure && token instanceof Operator operator && VECTOR_OPERATORS.contains(operator.getName())) {
                //System.out.println("Vector graphic operator found");
            }
            else if(insideFigure && token instanceof Operator operator && colorOperatorToColorSpace.containsKey(operator.getName())) {
                colorSpaces.add(colorOperatorToColorSpace.get(operator.getName()));
            }
            else if(insideFigure && token instanceof Operator operator && operator.getName().equals("EMC")) {
                insideFigure = false;
                // Found the end of a Figure tag
                String colorSpaceSummary = colorSpaces.stream().sorted().collect(Collectors.joining(" "));
                logVectorPathInfo(writer, colorSpaceSummary, indentLevel);
                colorSpaces.clear();
                System.out.println("EMC found");
            }

        }
//        for(Object token : tokens) {
//
//        }



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

    private void logVectorPathInfo(BufferedWriter writer, String summary, int indentLevel) throws IOException {
        writer.write(indent(indentLevel) + "- Inline Vector Graphic\n");
        writer.write(indent(indentLevel + 1) + "* ColorSpace: " + summary + "\n");
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
