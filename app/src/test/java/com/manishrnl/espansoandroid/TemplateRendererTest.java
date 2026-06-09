package com.manishrnl.espansoandroid;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;

public class TemplateRendererTest {
    @Test
    public void expandsDateTokensAndLeavesInvalidTokensAlone() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.JUNE, 9, 14, 5, 0);

        String result = TemplateRenderer.render(
                "%dd%-%MM%-%yyyy% %HH%:%mm% %not-a-pattern%",
                calendar.getTime(),
                Locale.US
        );

        assertEquals("09-06-2026 14:05 %not-a-pattern%", result);
    }
}
