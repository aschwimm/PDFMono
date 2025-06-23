package com.aschwimm.pdfmono.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

/*
This class handles PDF file I/O
 */
public class PDFDocumentIO {

    // Update how PDF files are loaded for PDFBox 3.0.5
    public  PDDocument loadDocument(String inputFile) throws IOException {

        try{
            File file = new File(inputFile);
            return Loader.loadPDF(new RandomAccessReadBufferedFile(file));
        } catch(IOException e) {
            System.err.println("Error loading PDF: " + e.getMessage());
            throw e;
        }
    }}
