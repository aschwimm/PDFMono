import com.aschwimm.pdfmono.util.PDFInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.*;

import static org.assertj.core.api.Assertions.*;

public class PDFInspectorTest {

    private PDFInspector pdfInspector;
    private Path inputPath;
    private Path outputLogPath;

    @BeforeEach
    public void setUp() {
        pdfInspector = new PDFInspector();
        inputPath = Paths.get("src/test/resources/input/inspector_sample.pdf");
        outputLogPath = Paths.get("src/test/resources/output/inspector_report.md");
    }
    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(outputLogPath)) {
            Files.delete(outputLogPath);
        }
    }


    @Test
    void shouldCreateInspectionLogFile() {
        // Act
        pdfInspector.inspect(inputPath.toString(), outputLogPath.toString());

        // Assert
        assertThat(Files.exists(outputLogPath)).isTrue();

        // Optional: check that the log contains expected keywords
        try {
            String content = Files.readString(outputLogPath);
            assertThat(content)
                    .contains("PDF Inspection Report")
                    .contains("Page 1");
        } catch (IOException e) {
            fail("Could not read generated log file", e);
        }
    }

    @Test
    void shouldHandleMissingPdfGracefully() {
        // Act
        Throwable thrown = catchThrowable(() ->
                pdfInspector.inspect("src/test/resources/input/nonexistent.pdf", outputLogPath.toString())
        );

        // Assert
        assertThat(thrown).isInstanceOf(IOException.class);
    }
}
