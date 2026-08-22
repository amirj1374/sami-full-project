package com.sami.app.siminvestment;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SimRondiAnalyzer {
    private SimRondiAnalyzer() {}

    static Result analyze(String raw) {
        String phone = normalize(raw);
        String digits = phone.substring(4);
        Set<String> patterns = new LinkedHashSet<>();
        int score = 0;
        int maxRun = longestRun(digits);
        if (maxRun >= 4) { patterns.add("FOUR_OR_MORE_REPEAT"); score += 35; }
        else if (maxRun == 3) { patterns.add("TRIPLE_REPEAT"); score += 22; }
        else if (maxRun == 2) { patterns.add("PAIR_REPEAT"); score += 8; }

        if (digits.matches(".*(.)(.)\\1\\2.*")) { patterns.add("ABAB"); score += 18; }
        if (digits.matches(".*(.)\\1(.)\\2.*")) { patterns.add("AABB"); score += 16; }
        if (digits.matches(".*(.)(.)\\2\\1.*")) { patterns.add("ABBA"); score += 18; }
        if (hasSequence(digits, 4)) { patterns.add("SEQUENCE"); score += 24; }
        if (isPalindrome(digits)) { patterns.add("SYMMETRY"); score += 30; }
        if (digits.matches(".*(000|111|222|333|444|555|666|777|888|999)$")) { patterns.add("SPECIAL_ENDING"); score += 18; }
        if (distinctDigits(digits) <= 3) { patterns.add("LOW_DIGIT_VARIETY"); score += 18; }
        score = Math.min(score, 100);
        String numberClass = score >= 85 ? "VIP" : score >= 65 ? "SPECIAL" : score >= 45 ? "ROUND" : score >= 22 ? "SEMI_ROUND" : "ORDINARY";
        return new Result(phone, phone.substring(0, 4), numberClass, score, List.copyOf(patterns));
    }

    static String normalize(String raw) {
        if (raw == null) throw invalid();
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("98") && digits.length() == 12) digits = "0" + digits.substring(2);
        if (digits.length() == 10 && digits.startsWith("912")) digits = "0" + digits;
        if (!digits.matches("0912[0-9]{7}")) throw invalid();
        return digits;
    }

    private static int longestRun(String value) {
        int best = 1, current = 1;
        for (int i = 1; i < value.length(); i++) {
            current = value.charAt(i) == value.charAt(i - 1) ? current + 1 : 1;
            best = Math.max(best, current);
        }
        return best;
    }

    private static boolean hasSequence(String value, int length) {
        for (int start = 0; start <= value.length() - length; start++) {
            int up = 1, down = 1;
            for (int i = start + 1; i < value.length(); i++) {
                int previous = value.charAt(i - 1) - '0', current = value.charAt(i) - '0';
                up = current == previous + 1 ? up + 1 : 1;
                down = current == previous - 1 ? down + 1 : 1;
                if (up >= length || down >= length) return true;
                if (up == 1 && down == 1) break;
            }
        }
        return false;
    }

    private static boolean isPalindrome(String value) {
        return new StringBuilder(value).reverse().toString().equals(value);
    }

    private static int distinctDigits(String value) {
        List<Character> found = new ArrayList<>();
        for (char digit : value.toCharArray()) if (!found.contains(digit)) found.add(digit);
        return found.size();
    }

    private static ApiException invalid() {
        return new ApiException(ErrorCode.VALIDATION_FAILED, "Phone must be an Iranian 0912 number");
    }

    record Result(String phone, String prefix, String numberClass, int score, List<String> patterns) {}
}
