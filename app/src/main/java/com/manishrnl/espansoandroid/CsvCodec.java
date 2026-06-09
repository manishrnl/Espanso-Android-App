package com.manishrnl.espansoandroid;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CsvCodec {
    public static final List<String> HEADERS = Arrays.asList(
            "Keyword",
            "Text",
            "Replace after Typing Space",
            "Position",
            "Multiple Templates Selection Strategy",
            "Folder"
    );

    private CsvCodec() {
    }

    public static List<Shortcut> read(Reader source) throws IOException {
        List<List<String>> rows = parseRows(source);
        if (rows.isEmpty()) {
            throw new IOException("The CSV file is empty.");
        }

        List<String> header = rows.get(0);
        if (!header.isEmpty() && header.get(0).startsWith("\uFEFF")) {
            header.set(0, header.get(0).substring(1));
        }

        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            indexes.put(header.get(i).trim(), i);
        }
        for (String required : HEADERS) {
            if (!indexes.containsKey(required)) {
                throw new IOException("Missing CSV column: " + required);
            }
        }

        List<Shortcut> shortcuts = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }

            String keyword = value(row, indexes.get("Keyword"));
            if (keyword.isEmpty()) {
                throw new IOException("Row " + (rowIndex + 1) + " has an empty Keyword.");
            }

            int position = 1;
            String positionText = value(row, indexes.get("Position")).trim();
            if (!positionText.isEmpty()) {
                try {
                    position = Math.max(1, Integer.parseInt(positionText));
                } catch (NumberFormatException error) {
                    throw new IOException("Row " + (rowIndex + 1) + " has an invalid Position.");
                }
            }

            shortcuts.add(new Shortcut(
                    keyword,
                    value(row, indexes.get("Text")),
                    Boolean.parseBoolean(value(row, indexes.get("Replace after Typing Space")).trim()),
                    position,
                    value(row, indexes.get("Multiple Templates Selection Strategy")),
                    value(row, indexes.get("Folder"))
            ));
        }
        return shortcuts;
    }

    public static void write(Writer writer, List<Shortcut> shortcuts) throws IOException {
        writeRow(writer, HEADERS);
        for (Shortcut shortcut : shortcuts) {
            writeRow(writer, Arrays.asList(
                    shortcut.getKeyword(),
                    shortcut.getText(),
                    Boolean.toString(shortcut.isReplaceAfterSpace()),
                    Integer.toString(shortcut.getPosition()),
                    shortcut.getSelectionStrategy(),
                    shortcut.getFolder()
            ));
        }
        writer.flush();
    }

    private static List<List<String>> parseRows(Reader source) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        try (PushbackReader reader = new PushbackReader(source, 1)) {
            int codePoint;
            while ((codePoint = reader.read()) != -1) {
                char character = (char) codePoint;
                if (quoted) {
                    if (character == '"') {
                        int next = reader.read();
                        if (next == '"') {
                            field.append('"');
                        } else {
                            quoted = false;
                            if (next != -1) {
                                reader.unread(next);
                            }
                        }
                    } else {
                        field.append(character);
                    }
                    continue;
                }

                if (character == '"' && field.length() == 0) {
                    quoted = true;
                } else if (character == ',') {
                    row.add(field.toString());
                    field.setLength(0);
                } else if (character == '\r' || character == '\n') {
                    if (character == '\r') {
                        int next = reader.read();
                        if (next != '\n' && next != -1) {
                            reader.unread(next);
                        }
                    }
                    row.add(field.toString());
                    field.setLength(0);
                    rows.add(row);
                    row = new ArrayList<>();
                } else {
                    field.append(character);
                }
            }
        }

        if (quoted) {
            throw new IOException("The CSV contains an unterminated quoted field.");
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }

    private static void writeRow(Writer writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escape(values.get(i)));
        }
        writer.write("\r\n");
    }

    private static String escape(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.indexOf(',') < 0
                && safeValue.indexOf('"') < 0
                && safeValue.indexOf('\r') < 0
                && safeValue.indexOf('\n') < 0) {
            return safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private static String value(List<String> row, int index) {
        return index < row.size() ? row.get(index) : "";
    }

    private static boolean isBlankRow(List<String> row) {
        for (String value : row) {
            if (!value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}

