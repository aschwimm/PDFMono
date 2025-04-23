import com.aschwimm.pdfmono.service.PDFConversionService;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class PDFConversionServiceTest {
    private PDFConversionService pdfConversionService;

    @BeforeEach
    public void setUp() {
        pdfConversionService = new PDFConversionService();
    }


    @Test
    void shouldThrowIOExceptionWhenLoadingInvalidPdfPath() {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void shouldConvertPageToBlackAndWhiteSuccessfully() {
        // Arrange
        String inputPath = Paths.get("src/test/resources/input/color_sample_input.pdf").toAbsolutePath().toString();
        String outputPath = Paths.get("src/test/resources/output/black_white_sample_output.pdf").toAbsolutePath().toString();

        // Act
        boolean result = pdfConversionService.convertToBlackAndWhite(inputPath, outputPath);

        // Assert
        assertThat(result).isTrue();
        assertThat(Files.exists(Paths.get(outputPath))).isTrue();
    }

    @Test
    void shouldReturnGrayscaleImageFromColorInput() {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void shouldCreatePdfDocumentFromImages() {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void shouldSavePdfDocumentToDisk() {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void shouldConvertEntirePdfToBlackAndWhiteSuccessfully() {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void shouldReturnFalseWhenInputPdfDoesNotExist() {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void shouldReturnFalseWhenOutputDirectoryIsInvalid() {
        // Arrange

        // Act

        // Assert
    }
}
