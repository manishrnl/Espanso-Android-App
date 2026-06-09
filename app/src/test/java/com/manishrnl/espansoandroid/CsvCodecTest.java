package com.manishrnl.espansoandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

public class CsvCodecTest {
    @Test
    public void roundTripsQuotedAndMultilineText() throws Exception {
        List<Shortcut> source = Arrays.asList(
                new Shortcut(";email", "support@example.com", false, 1, "", "Contact"),
                new Shortcut(";cover", "Hello, \"team\"\nSecond line", true, 2, "First", "Resume")
        );

        StringWriter output = new StringWriter();
        CsvCodec.write(output, source);
        List<Shortcut> parsed = CsvCodec.read(new StringReader(output.toString()));

        assertEquals(2, parsed.size());
        assertEquals(";cover", parsed.get(1).getKeyword());
        assertEquals("Hello, \"team\"\nSecond line", parsed.get(1).getText());
        assertEquals("First", parsed.get(1).getSelectionStrategy());
        assertEquals("Resume", parsed.get(1).getFolder());
        assertFalse(parsed.get(0).isReplaceAfterSpace());
    }
}
