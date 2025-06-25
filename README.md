# PDFMono

A versatile command-line utility for processing PDF documents. This tool can convert PDFs to grayscale, and generate detailed markdown reports about a PDF's internal structure and content. Built with Java 21 and powered by Apache PDFBox.

## Table of Contents

* [Features](#features)
* [How It Works](#how-it-works)
* [Requirements](#requirements)
* [Usage](#usage)
    * [Grayscale Conversion](#grayscale-conversion-usage)
    * [PDF Internal Report](#pdf-internal-report-usage)
* [Known Limitations and Issues](#known-limitations-and-issues)
* [Contributing](#contributing)
* [License](#license)

## Features

This application provides two primary functionalities:

1.  **PDF to Grayscale Conversion:**
    * Converts all pages of an input PDF to a new grayscale PDF document.
    * Allows specifying the rendering DPI (Dots Per Inch) for controlling image quality and output file size.
    * Optimizes output by using true grayscale image formats.

2.  **PDF Internal Structure Report Generation:**
    * Generates a human-readable Markdown report detailing various internal aspects of a PDF.
    * Reports on:
        * **Color Spaces:** Detects and lists color spaces used within the document.
        * **Image XObjects:** Identifies and provides details about embedded images.
        * **Form XObjects:** Lists and describes reusable content blocks (forms).
       

## How It Works

### Grayscale Conversion

The grayscale conversion process involves:
1.  Loading the input PDF document using Apache PDFBox.
2.  Iterating through each page of the PDF.
3.  Rendering each page into an in-memory `BufferedImage` at the specified DPI.
4.  Converting the `BufferedImage`'s RGB pixels to grayscale using a standard luminosity formula.
5.  Embedding the processed grayscale `BufferedImage` back into a new PDF document object.
6.  Saving the new grayscale PDF document to the specified output path.

### PDF Internal Report

The report generation involves:
1.  Parsing the PDF document structure using Apache PDFBox.
2.  Traversing the PDF's object hierarchy to extract information about:
    * Pages and their properties.
    Objects like Images and Forms are parsed by iterating through the page's Resource dictionary, then logging the type of object (`ImageXObject`, `FormXObject` etc.) and the object's name followed by a list of its properties.  
    
    <pre>
    **Example output**
    
    - Image XObject: Im0
    * Width: 2596
    * Height: 1697
    * ColorSpace: DeviceRGB
    * BitsPerComponent: 8
    * IsStencil: false
    * Suffix: jpg
    </pre>
    
    * Resources (images, fonts, color spaces, forms). The page's content stream is parsed, tokens associated with drawing graphics and coloring them are checked against a map of colorspace and fill/stroke operators before then parsing their associated operands. CMYK colorspace output also includes its RGB equivalent and an approximation of the color being rendered (red, black, white etc.);
    <pre>**Example output**
    
    - Inline Vector Graphic
    * ColorSpace: DeviceCMYK (nonstroking)
      * Colors: 
        - (0.07, 0.94, 0.65, 0.25)
          - Approximate Color: Red (RGB: 177, 11, 66)
        - (0.69, 0.67, 0.64, 0.74)
          - Approximate Color: Black (RGB: 20, 21, 24)
        - (0.00, 0.00, 0.00, 0.00)
          - Approximate Color: White (RGB: 255, 255, 255)
        - (0.00, 0.00, 0.00, 1.00)
          - Approximate Color: Black (RGB: 0, 0, 0)
    * Paint Operator: Filled vector path (nonzero rule)</pre>

## Requirements

* **Java Development Kit (JDK) 21 or newer.**

## Usage

Once you have downloaded the executable JAR file, you can run the application from your terminal. The application supports two primary commands: 
1. **Grayscale Conversion**
2. **PDF Internals Report Generation**
### Grayscale Conversion Usage
This command converts an input PDF document into a new PDF where all pages are rendered in grayscale.
```
  java -jar path/to/PDFMono-X.Y.Z.jar <input-path> <output-path> --grayscale [--dpi <value>]
```
* `<input-path>`: **(Required)** The path to the source PDF file to be converted.
* `<output-path>`: **(Required)** The path where the new grayscale PDF will be saved
* `--grayscale`: **(Required)** Required argument for grayscale conversion
* `--dpi <value>`: **(Optional Flag)** Specify the Dots Per Inch (DPI) to use for rendering pages to images. Higher DPI results in better image quality at the cost of increased file size and processing time.
    * **Default**: 150 DPI, a good balance between image quality and performance.
### PDF Internal Report Usage
This command generates a Markdown file containing detailed information about the internal structure of a PDF document.
```
  java -jar path/to/PDFMono-X.Y.Z.jar <input-path> <output-path> --inspect
```
* `<input-path>`: **(Required)** The path to the source PDF file to be converted.
* `<output-path>`: **(Required)** The path where the new grayscale PDF will be saved
* `--inspect`: **(Required)** Required argument for inspection and report creation

## Known Limitations and Issues
* **Text Unsearchable:** Converting pages with text to images and applying grayscale conversion removes a PDF reader's ability to search within the document for text.
* **Increased File Size:** Generating a PDF where each page is an image increases file size by around 50 to 60 percent.
* **Accessibility Issues:** Rendering pages as images prevents screen readers from parsing text and alternative text for images in original PDF is lost.

## Contributing

We welcome contributions to this project! If you find a bug, have an idea for an improvement, or want to add a new feature, please feel free to:

1.  **Fork** the repository.
2.  **Create a new branch** for your changes (e.g., `git checkout -b feature/your-feature-name` or `bugfix/issue-description`).
3.  **Make your changes**, ensuring they align with the project's coding style.
4.  **Write clear and concise commit messages.**
5.  **Push your branch** to your fork.
6.  **Open a Pull Request** to the `main` branch of this repository.

For major changes or new features, it's recommended to open an [issue](https://github.com/aschwimm/PDFMono/issues) first to discuss the proposed changes before starting work.

## License

This project is open-source and available under the [**MIT License**](LICENSE). You are free to use, modify, and distribute this software, provided the original copyright and license notice are included.