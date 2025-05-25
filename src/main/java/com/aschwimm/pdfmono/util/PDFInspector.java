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
import java.util.*;
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
    private static final Map<String, String> FILL_OPERATORS = Map.of(
            "f", "Filled vector path (nonzero rule)",
            "f*", "Filled vector path (even-odd rule)"
    );

    private static final Map<String, String> STROKE_OPERATORS = Map.of(
            "S", "Stroked vector path",
            "s", "Stroked vector path"
    );

    private static final Map<String, String> FILL_AND_STROKE_OPERATORS = Map.of(
            "B", "Filled and stroked vector path (nonzero rule)",
            "B*", "Filled and stroked vector path (even-odd rule)",
            "b", "Closed, filled and stroked vector path (nonzero rule)",
            "b*", "Closed, filled and stroked vector path (even-odd rule)"
    );
    private static final Map<String, String> ALL_PAINT_OPERATORS;
    static {
        Map<String, String> all = new HashMap<>();
        all.putAll(FILL_OPERATORS);
        all.putAll(STROKE_OPERATORS);
        all.putAll(FILL_AND_STROKE_OPERATORS);
        ALL_PAINT_OPERATORS = Collections.unmodifiableMap(all);
    }



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
        VectorGraphicInfo vectorGraphicObj = new VectorGraphicInfo();
        boolean insideFigure = false;
        for(int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);
            if(!insideFigure && token instanceof COSName name && name.getName().equals("Figure")) {
                insideFigure = true;
                System.out.println("Figure found");
                i++;
                // Found the start of a Figure tag
            }
            else if(insideFigure && token instanceof Operator operator) {
                String name = operator.getName();
                if(ALL_PAINT_OPERATORS.containsKey(name)) {
                    vectorGraphicObj.setPaintOperator(ALL_PAINT_OPERATORS.get(operator.getName()));
                }
                else if(colorOperatorToColorSpace.containsKey(name)) {
                    vectorGraphicObj.setColorSpace(colorOperatorToColorSpace.get(name));
                    if(name.equals("k") && tokens.get(i - 4) != null) {
                        try {
                            float c = ((COSNumber) tokens.get(i - 4)).floatValue();
                            System.out.println("c: " + c);
                            float m = ((COSNumber) tokens.get(i - 3)).floatValue();
                            System.out.println("m: " + m);
                            float y = ((COSNumber) tokens.get(i - 2)).floatValue();
                            System.out.println("y: " + y);
                            float kVal = ((COSNumber) tokens.get(i - 1)).floatValue();
                            System.out.println("kVal: " + kVal);

                            List<Float> CMYKValues = List.of(c, m, y, kVal);

                            vectorGraphicObj.setCMYKValues(CMYKValues);
                        } catch (ClassCastException e) {
                            System.err.println("One of the CMYK tokens is not a COSNumber at index " + i + ": " + e.getMessage());
                        }
                    }
                    System.out.println("Color space found: " + name);
                }
                else if(name.equals("EMC")) {
                    insideFigure = false;
                    // Found the end of a Figure tag
                    logVectorPathInfo(writer, vectorGraphicObj, indentLevel);
                    System.out.println("EMC found");
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

    private void logVectorPathInfo(BufferedWriter writer, VectorGraphicInfo vectorGraphicInfo, int indentLevel) throws IOException {
        writer.write(indent(indentLevel) + "- Inline Vector Graphic\n");
        writer.write(indent(indentLevel + 1) + "* ColorSpace: " + vectorGraphicInfo.getColorSpace() + "\n");
        Set<List<Float>> CMYKValues = vectorGraphicInfo.getCMYKValues();
        writer.write(indent(indentLevel + 2) + "* Colors: \n");
        for (List<Float> cmyk : CMYKValues) {
            String joined = cmyk.stream()
                    .map(v -> String.format("%.2f", v))
                    .collect(Collectors.joining(", "));
            writer.write(indent(indentLevel + 3) + "- (" + joined + ")\n");
        }

        writer.write(indent(indentLevel + 1) + "* Paint Operator: " + vectorGraphicInfo.getPaintOperator() + "\n");
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

    private static class VectorGraphicInfo {
        private String colorSpace, paintOperator;
        private Set<List<Float>> CMYKValues;
        ArrayList<Float> RGBValues;
        ArrayList<Float> GrayValues;

        VectorGraphicInfo() {
            colorSpace = "Unknown";
            paintOperator = "Unknown";
            CMYKValues = new HashSet<>();
        }

        VectorGraphicInfo(String colorSpace, String paintOperator) {
            this.colorSpace = colorSpace;
            this.paintOperator = paintOperator;
        }

        public String getColorSpace() {
            return colorSpace;
        }
        public String getPaintOperator() {
            return paintOperator;
        }
        public void setColorSpace(String colorSpace) {
            this.colorSpace = colorSpace;
        }
        public void setPaintOperator(String paintOperator) {
            this.paintOperator = paintOperator;
        }
        public void setCMYKValues(List<Float> CMYKValues) {
            this.CMYKValues.add(List.copyOf(CMYKValues));
        }

        public Set<List<Float>> getCMYKValues() {
            return CMYKValues;
        }

        public void setRGBValues(ArrayList<Float> RGBValues) {
            this.RGBValues = new ArrayList<>(RGBValues);
        }
        public void setGrayValues(ArrayList<Float> GrayValues) {
            this.GrayValues = new ArrayList<>(GrayValues);
        }
    }
}
