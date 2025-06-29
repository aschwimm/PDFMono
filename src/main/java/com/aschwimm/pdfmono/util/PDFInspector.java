package com.aschwimm.pdfmono.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.cos.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class PDFInspector {
    private final PDFDocumentIO pdfDocumentIO;

    // Constructor instantiates loader dependency
    public PDFInspector(PDFDocumentIO pdfDocumentIO) {
        this.pdfDocumentIO = pdfDocumentIO;
    }
    /*
    Unused: Set of draw operators that can appear in the content stream
     */
    private static final Set<String> VECTOR_OPERATORS = Set.of(
            "m", "l", "re", "c", "v", "y", "h",
            "S", "s", "f", "F", "f*", "B", "B*", "b", "b*"
    );
    /*
    Map of color space operators that appear in content stream as keys with their associated colorspace names
     */
    private static final Map<String, String> colorOperatorToColorSpace = Map.ofEntries(
            Map.entry("rg", "DeviceRGB (nonstroking)"),
            Map.entry("RG", "DeviceRGB (stroking)"),
            Map.entry("g",  "DeviceGray (nonstroking)"),
            Map.entry("G",  "DeviceGray (stroking)"),
            Map.entry("k",  "DeviceCMYK (nonstroking)"),
            Map.entry("K",  "DeviceCMYK (stroking)")
    );
    /*
    Maps of currently identifiable fill, stroke, and fill/stroke operators that appear in content stream
     */
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
    /*
    This Map functions as a main registry of all currently identifiable colorspace and paint operators
     */
    private static final Map<String, String> ALL_PAINT_OPERATORS;
    static {
        Map<String, String> all = new HashMap<>();
        all.putAll(FILL_OPERATORS);
        all.putAll(STROKE_OPERATORS);
        all.putAll(FILL_AND_STROKE_OPERATORS);
        ALL_PAINT_OPERATORS = Collections.unmodifiableMap(all);
    }


    /*
    Handles core logic, PDDocument is loaded and its pages iterated over, resources and contents of each page and written to output file in loop
     */
    public void inspect(String inputPath, String outputLogPath) {

        try (PDDocument document = pdfDocumentIO.loadDocument(inputPath);
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputLogPath))) {

            writer.write("# PDF Inspection Report\n\n");
            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                System.out.printf("\rInspecting page %d of %d...          ", (i + 1), pageCount);
                System.out.flush();
                PDPage page = document.getPage(i);
                writer.write("## Page " + (i + 1) + "\n");


                PDResources resources = page.getResources();



                inspectResources(resources, writer, 1);
                inspectContents(page, writer, 1);
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
    Recursively traverse page resources, identifying images and forms and writing them to file
     */
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
    /*
    Traverses list of tokens in page's content stream and identifies tokens that are associated with colorspaces and drawing operators
    to identify graphics drawn in page's content stream
     */
    private void inspectContents(PDPage page, BufferedWriter writer, int indentLevel) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(page);
        List<Object> tokens = parser.parse();
        VectorGraphicInfo vectorGraphicObj = new VectorGraphicInfo();
        boolean insideFigure = false;
        for(int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);
            if(!insideFigure && token instanceof COSName name && name.getName().equals("Figure")) {
                insideFigure = true;

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

                            float m = ((COSNumber) tokens.get(i - 3)).floatValue();

                            float y = ((COSNumber) tokens.get(i - 2)).floatValue();

                            float kVal = ((COSNumber) tokens.get(i - 1)).floatValue();


                            List<Float> CMYKValues = List.of(c, m, y, kVal);

                            vectorGraphicObj.setCMYKValues(CMYKValues);
                        } catch (ClassCastException e) {
                            System.err.println("One of the CMYK tokens is not a COSNumber at index " + i + ": " + e.getMessage());
                        }
                    }
                }
                else if(name.equals("EMC")) {
                    insideFigure = false;
                    // Found the end of a Figure tag
                    logVectorPathInfo(writer, vectorGraphicObj, indentLevel);
                }

            }
        }
    }
    /*
    Retrieves the information about an image found on a page, then formats the information to be written to output file
     */
    private void logImageInfo(BufferedWriter writer, String name, PDImageXObject image, int indentLevel) throws IOException {
        writer.write(indent(indentLevel) + "- Image XObject: " + name + "\n");
        writer.write(indent(indentLevel + 1) + "* Width: " + image.getWidth() + "\n");
        writer.write(indent(indentLevel + 1) + "* Height: " + image.getHeight() + "\n");
        writer.write(indent(indentLevel + 1) + "* ColorSpace: " + image.getColorSpace().getName() + "\n");
        writer.write(indent(indentLevel + 1) + "* BitsPerComponent: " + image.getBitsPerComponent() + "\n");
        writer.write(indent(indentLevel + 1) + "* IsStencil: " + image.isStencil() + "\n");
        writer.write(indent(indentLevel + 1) + "* Suffix: " + image.getSuffix() + "\n");
    }
    /*
    Makes uses of a VectorGraphicInfo object to retrieve properties of a graphic drawn in page's content stream, then formats that information to be written to output file
     */
    private void logVectorPathInfo(BufferedWriter writer, VectorGraphicInfo vectorGraphicInfo, int indentLevel) throws IOException {
        writer.write(indent(indentLevel) + "- Inline Vector Graphic\n");
        writer.write(indent(indentLevel + 1) + "* ColorSpace: " + vectorGraphicInfo.getColorSpace() + "\n");
        Set<List<Float>> CMYKValues = vectorGraphicInfo.getCMYKValues();
        writer.write(indent(indentLevel + 2) + "* Colors: \n");
        for (List<Float> cmyk : CMYKValues) {
            String closestColor = ColorMapper.getClosestColorName(cmyk);
            String joined = cmyk.stream()
                    .map(v -> String.format("%.2f", v))
                    .collect(Collectors.joining(", "));
            writer.write(indent(indentLevel + 3) + "- (" + joined + ")\n");
            writer.write(indent(indentLevel + 4) + "- Approximate Color: " + closestColor + "\n");
        }

        writer.write(indent(indentLevel + 1) + "* Paint Operator: " + vectorGraphicInfo.getPaintOperator() + "\n");
    }
    // Indents strings of page information to file, makes it easier to set indent level and quickly identify level indentation in logImageInfo and logVectorPathInfo
    private String indent(int level) {
        return "  ".repeat(level);
    }


    /*
    Helper class that represents a graphic drawn in a page's content stream
    Since these graphics don't exist as objects inside the PDF, they have to be constructed by parsing colorspace, paint, draw etc. operators that comprise them
    This object is used to store information about a single graphic that can be retrieved by other methods as needed
     */
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
    /*
    Associates colors in a CMYK colorspace with a color approximation like black, red, green, etc. and also returns the RGB equivalent
    Output is written to file alongside the path drawn graphic in  content stream
     */
    private static class ColorMapper {

        private static final Map<String, int[]> NAMED_COLORS = Map.ofEntries(
                Map.entry("Black", new int[]{0, 0, 0}),
                Map.entry("White", new int[]{255, 255, 255}),
                Map.entry("Red", new int[]{255, 0, 0}),
                Map.entry("Green", new int[]{0, 255, 0}),
                Map.entry("Blue", new int[]{0, 0, 255}),
                Map.entry("Cyan", new int[]{0, 255, 255}),
                Map.entry("Magenta", new int[]{255, 0, 255}),
                Map.entry("Yellow", new int[]{255, 255, 0}),
                Map.entry("Gray", new int[]{128, 128, 128}),
                Map.entry("Orange", new int[]{255, 165, 0})
                // Temporarily disabled to prevent false "brown" positives
//                Map.entry("Dark Brown", new int[]{101, 67, 33})


        );
        /*
        Converts CMYK operands to RGB, then finds closest color in class's NAMED_COLORS field to RGB in Euclidean distance
         */
        public static String getClosestColorName(List<Float> cmyk) {
            float c = cmyk.get(0);
            float m = cmyk.get(1);
            float y = cmyk.get(2);
            float k = cmyk.get(3);
            int r = (int)(255 * (1 - c) * (1 - k));
            int g = (int)(255 * (1 - m) * (1 - k));
            int b = (int)(255 * (1 - y) * (1 - k));

            String closestColor = null;
            double minDistance = Double.MAX_VALUE;

            for (Map.Entry<String, int[]> entry : NAMED_COLORS.entrySet()) {
                int[] rgb = entry.getValue();
                double distance = Math.sqrt(
                        Math.pow(rgb[0] - r, 2) +
                                Math.pow(rgb[1] - g, 2) +
                                Math.pow(rgb[2] - b, 2)
                );

                if (distance < minDistance) {
                    minDistance = distance;
                    closestColor = entry.getKey();
                }
            }

            return closestColor + String.format(" (RGB: %d, %d, %d)", r, g, b);
        }
    }

}
