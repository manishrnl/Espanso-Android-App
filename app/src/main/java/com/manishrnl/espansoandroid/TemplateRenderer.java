package com.manishrnl.espansoandroid;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TemplateRenderer {
    private static final Pattern DATE_TOKEN = Pattern.compile("%([^%]+)%");

    private TemplateRenderer() {
    }

    public static String render(String template, Date now, Locale locale) {
        Matcher matcher = DATE_TOKEN.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String pattern = matcher.group(1);
            String replacement;
            try {
                replacement = new SimpleDateFormat(pattern, locale).format(now);
            } catch (IllegalArgumentException ignored) {
                replacement = matcher.group();
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}

