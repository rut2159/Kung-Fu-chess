package com.chessgame.integration;

import com.chessgame.texttests.Script;
import com.chessgame.texttests.ScriptParser;
import com.chessgame.texttests.ScriptRunner;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;


class TextScriptsTest {

    private static final Path SCRIPTS_DIR = Paths.get("src", "test", "resources", "scripts");

    @Test
    void allTextScriptsPass() throws IOException {
        ScriptParser parser = new ScriptParser();
        ScriptRunner runner = new ScriptRunner();

        try (Stream<Path> files = Files.list(SCRIPTS_DIR)) {
            List<Path> scriptFiles = files
                    .filter(p -> p.toString().endsWith(".kfc"))
                    .sorted()
                    .toList();

            assertTrue(scriptFiles.size() > 0, "לא נמצאו קבצי .kfc בתיקייה: " + SCRIPTS_DIR);

            for (Path scriptFile : scriptFiles) {
                String text = Files.readString(scriptFile);
                Script script = parser.parse(text);
                List<ScriptRunner.Mismatch> mismatches = runner.run(script);

                assertTrue(mismatches.isEmpty(), describeFailure(scriptFile, mismatches));
            }
        }
    }

    private String describeFailure(Path scriptFile, List<ScriptRunner.Mismatch> mismatches) {
        StringBuilder sb = new StringBuilder();
        sb.append(scriptFile.getFileName()).append(" נכשל:\n");
        for (ScriptRunner.Mismatch m : mismatches) {
            sb.append("  print #").append(m.printIndex)
                    .append(" - צפוי: ").append(m.expected)
                    .append(" בפועל: ").append(m.actual).append("\n");
        }
        return sb.toString();
    }
}
