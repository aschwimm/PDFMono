# Changelog


### Unreleased
- **Bug Fix**: Issue with fonts being replaced during pdf conversion
  - Fonts present in the pdf are replaced with approximations when the original font is not
    present
  - **Fix**: This issue occurs because the PDFs are converted to images and rasterized
    I think this can be solved by parsing each page's content stream and, grabbing the appropriate operators(rg, RG, k, K)
    and converting their operands to grayscale
- **Bug Fix**: Vector elements like text are now properly converted to grayscale, but images are not
  - **Fix**: Embedded images will are converted to grayscale using PDImageXObject, but some images still remain colored
  this may be because of the image type or image color space, not sure.

- **Add**: Utility class PDFInspector created to analyze properties of tester PDF files and make sure conversion is handled appropriately
## [1.0.1] - 2025-04-25
### Fixed
- Corrected grayscale conversion for PDFs to handle color text correctly.
    - Replaced Graphics2D.drawImage method with ColorConvertOp to achieve proper luminance-based conversion.
    - Fixed color biases and removed unwanted color tints in the conversion process.

## [1.0.0] - 2025-04-15
### Added
- Initial release of PDF to grayscale conversion.
