# Changelog

### Unreleased
- **Bug Fix**: Vector elements like text are now properly converted to grayscale, but images are not
  - **Fix**: Embedded images are converted to grayscale using PDImageXObject, but some images still remain colored.
    This may be because of the image type or image color space—needs further investigation.

## [1.0.3] - 2025-05-21
### Added
- The utility class `PDFInspector` can now parse a PDF page's content stream and identify inline graphics as well as ImageXObjects and forms.
  - `PDFInspector` writes a markdown log with each page's contents in a way that's easy to be read and understood.
  - Inline vector-drawn graphics created in the page's content stream are able to be identified and their colorspace and paint operators logged.
## [1.0.2] - 2025-05-08
### Fixed
- Fonts were being replaced with approximations during PDF conversion when the original font was not present.
  - Cause: PDFs were being converted to images and rasterized, which ignored embedded font data.
  - Fix: Replaced rasterization with direct content stream parsing, capturing `rg`, `RG`, `k`, `K` operators and converting operands to grayscale. This preserves original fonts.

## [1.0.1] - 2025-04-25
### Fixed
- Corrected grayscale conversion for PDFs to handle color text correctly.
  - Replaced `Graphics2D.drawImage` method with `ColorConvertOp` to achieve proper luminance-based conversion.
  - Fixed color biases and removed unwanted color tints in the conversion process.

## [1.0.0] - 2025-04-15
### Added
- Initial release of PDF to grayscale conversion.
