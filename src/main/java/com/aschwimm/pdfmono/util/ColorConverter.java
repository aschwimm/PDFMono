package com.aschwimm.pdfmono.util;
/*
This class includes methods for simple color conversion
 */
public class ColorConverter {

    // Standard grayscale conversion formula to be used with RGB values
    public static float rgbToGray(int R, int G, int B) {
        return 0.299f * R + 0.587f * G + 0.114f * B;
    }

    // Apply standard conversion formula to float array returned by org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK
    public static float rgbToGray(float[] rgb) {
        if (rgb == null || rgb.length < 3) {
            throw new IllegalArgumentException("RGB array must not be null and must contain at least 3 elements.");
        }
        return (float) (0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]);
    }
}
