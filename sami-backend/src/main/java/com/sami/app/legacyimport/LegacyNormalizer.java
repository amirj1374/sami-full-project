package com.sami.app.legacyimport;

import java.text.Normalizer;

public final class LegacyNormalizer {
    private LegacyNormalizer() {}

    public static String text(Object value) {
        if (value == null) return null;
        String s = Normalizer.normalize(String.valueOf(value), Normalizer.Form.NFKC)
                .replace('\u064A', '\u06CC').replace('\u0649', '\u06CC')
                .replace('\u0643', '\u06A9')
                .replace('\u200C', ' ').replaceAll("[\\s\\u00A0]+", " ").trim();
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= '\u06F0' && c <= '\u06F9') out.append((char) ('0' + c - '\u06F0'));
            else if (c >= '\u0660' && c <= '\u0669') out.append((char) ('0' + c - '\u0660'));
            else out.append(c);
        }
        return out.toString();
    }

    public static String phone(Object value) {
        String s = text(value);
        if (s == null) return null;
        String digits = s.replaceAll("\\D", "");
        if (digits.startsWith("0098")) digits = digits.substring(4);
        else if (digits.startsWith("98") && digits.length() > 10) digits = digits.substring(2);
        if (digits.length() == 10 && digits.startsWith("9")) digits = "0" + digits;
        return digits;
    }

    public static String code(Object value) {
        String s = text(value);
        return s == null ? null : s.replaceAll("[\\s\\-_]", "").toUpperCase();
    }
}
