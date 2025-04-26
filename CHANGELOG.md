# Changelog

## [Unreleased]
### Fixed
- Fixed bug with grayscale conversion in PDFs, ensuring correct monochrome rendering.

## [1.0.1] - 2025-04-25
### Fixed
- Corrected grayscale conversion for PDFs to handle color text correctly.
    - Replaced Graphics2D.drawImage method with ColorConvertOp to achieve proper luminance-based conversion.
    - Fixed color biases and removed unwanted color tints in the conversion process.

## [1.0.0] - 2025-04-15
### Added
- Initial release of PDF to grayscale conversion.
