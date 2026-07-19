package com.sami.app.dataquality.matcher;

import com.sami.app.dataquality.spi.DuplicateMatcher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Phonetic matching: Soundex for Latin text, plus normalisation of the Persian
 * characters that are routinely typed inconsistently (ی/ي, ک/ك, ه/ة, ZWNJ),
 * which is the dominant cause of "same person entered twice" in Iranian data.
 */
@Component
public class PhoneticMatcher implements DuplicateMatcher {

    @Override public String strategy() { return "phonetic"; }

    @Override public String label() { return "Phonetic match"; }

    @Override
    public double similarity(String left, String right, Map<String, Object> config) {
        if (left == null || right == null) {
            return 0;
        }
        String a = key(left);
        String b = key(right);
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        return a.equals(b) ? 1.0 : 0.0;
    }

    private String key(String value) {
        String normalized = normalizePersian(ExactMatcher.normalize(value));
        return normalized.matches(".*[a-z].*") ? soundex(normalized) : normalized;
    }

    /** Folds the interchangeable Arabic/Persian glyph variants onto one form. */
    static String normalizePersian(String value) {
        return value
                .replace('ي', 'ی')   // Arabic yeh  -> Persian yeh
                .replace('ك', 'ک')   // Arabic kaf  -> Persian keheh
                .replace('ة', 'ه')   // teh marbuta -> heh
                .replace("‌", "")          // zero-width non-joiner
                .replaceAll("[ً-ْ]", ""); // harakat
    }

    private String soundex(String value) {
        String letters = value.replaceAll("[^a-z]", "");
        if (letters.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder().append(Character.toUpperCase(letters.charAt(0)));
        char previous = code(letters.charAt(0));
        for (int i = 1; i < letters.length() && out.length() < 4; i++) {
            char code = code(letters.charAt(i));
            if (code != '0' && code != previous) {
                out.append(code);
            }
            previous = code;
        }
        while (out.length() < 4) {
            out.append('0');
        }
        return out.toString();
    }

    private char code(char c) {
        return switch (c) {
            case 'b', 'f', 'p', 'v' -> '1';
            case 'c', 'g', 'j', 'k', 'q', 's', 'x', 'z' -> '2';
            case 'd', 't' -> '3';
            case 'l' -> '4';
            case 'm', 'n' -> '5';
            case 'r' -> '6';
            default -> '0';
        };
    }
}
