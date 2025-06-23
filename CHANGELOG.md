# Changelog

## Unreleased
**Replace alternate colorspaces with DeviceGray**: Replacing alternate colorspace for `/Separation` type colorspaces has shown limited success but needs to handle edge cases before it can be reiably implemented 

## [1.0.5] - 2025-06-21
### Fixed
- Operand tokens preceding `k` and `K` operators are now correctly removed from content stream before being written again, preventing duplicate operands from being written
## [1.0.4] - 2025-06-09
### Fixed
- Graphics created with draw operators in the content stream with the `CMYK colorspace` were not being properly handled. Added logic to identify these graphics
  and apply grayscale transformation.
  - `CMYK colorspace` paint operators had to be converted to `RGB colorspace`, transformation applied while checking for colors very close to white
    to avoid accidentally inverting colors below as certain threshold for whites

## [1.0.3] - 2025-05-21
### Added
- The utility class `PDFInspector` can now parse a PDF page's content stream and identify inline graphics as well as ImageXObjects and forms.
  - `PDFInspector` writes a markdown log with each page's contents in a way that's easy to be read and understood.
  - Inline vector-drawn graphics created in the page's content stream identifiable and their colorspace and paint operators logged.
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
