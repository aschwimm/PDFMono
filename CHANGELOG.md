# Changelog

### Unreleased
- **Bug Fix**: Issue with fonts being replaced during pdf conversion
  - Fonts present in the pdf are replaced with approximations when the original font is not
    present
  - **Fix**: This issue occurs because the PDFs are converted to images and rasterized
    I think this can be solved by parsing each page's content stream and, grabbing the appropriate operators(rg, RG, k, K)
    and converting their operands to grayscale
  - **Fix**: Stream-based parsing will solve grayscale conversion for vector elements but not embedded images.
  - **Add**: Embedded images will be converted to grayscale using PDImageXObject
## [1.0.1] - 2025-04-25
### Fixed
- Corrected grayscale conversion for PDFs to handle color text correctly.
    - Replaced Graphics2D.drawImage method with ColorConvertOp to achieve proper luminance-based conversion.
    - Fixed color biases and removed unwanted color tints in the conversion process.

## [1.0.0] - 2025-04-15
### Added
- Initial release of PDF to grayscale conversion.
