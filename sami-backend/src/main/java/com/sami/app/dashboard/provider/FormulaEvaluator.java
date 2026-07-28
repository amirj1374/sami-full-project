package com.sami.app.dashboard.provider;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.Function;

/**
 * A small, safe arithmetic evaluator for KPI formulas. Supports {@code + - * /},
 * parentheses, unary minus, decimal literals and identifiers (other KPI codes,
 * resolved via a supplied function). Recursive-descent; throws
 * {@link IllegalArgumentException} on any syntax/resolution problem, which the
 * KPI engine surfaces as a validation error.
 *
 * <p>Deliberately not a general scripting engine — no method calls, no state —
 * so formulas can be authored by non-developers without security concerns.
 */
public final class FormulaEvaluator {

    private final String input;
    private final Function<String, BigDecimal> resolver;
    private int pos;

    private FormulaEvaluator(String input, Function<String, BigDecimal> resolver) {
        this.input = input;
        this.resolver = resolver;
    }

    /** Evaluates {@code formula}, resolving identifiers via {@code resolver}. */
    public static BigDecimal evaluate(String formula, Function<String, BigDecimal> resolver) {
        if (formula == null || formula.isBlank()) {
            throw new IllegalArgumentException("Formula is empty");
        }
        FormulaEvaluator e = new FormulaEvaluator(formula, resolver);
        BigDecimal result = e.parseExpression();
        e.skipSpaces();
        if (e.pos < e.input.length()) {
            throw new IllegalArgumentException("Unexpected character at position " + e.pos);
        }
        return result;
    }

    private BigDecimal parseExpression() {
        BigDecimal value = parseTerm();
        while (true) {
            skipSpaces();
            char op = peek();
            if (op == '+' || op == '-') {
                pos++;
                BigDecimal rhs = parseTerm();
                value = op == '+' ? value.add(rhs) : value.subtract(rhs);
            } else {
                return value;
            }
        }
    }

    private BigDecimal parseTerm() {
        BigDecimal value = parseFactor();
        while (true) {
            skipSpaces();
            char op = peek();
            if (op == '*' || op == '/') {
                pos++;
                BigDecimal rhs = parseFactor();
                if (op == '/') {
                    if (rhs.signum() == 0) {
                        throw new IllegalArgumentException("Division by zero");
                    }
                    value = value.divide(rhs, MathContext.DECIMAL64);
                } else {
                    value = value.multiply(rhs);
                }
            } else {
                return value;
            }
        }
    }

    private BigDecimal parseFactor() {
        skipSpaces();
        char c = peek();
        if (c == '(') {
            pos++;
            BigDecimal value = parseExpression();
            skipSpaces();
            if (peek() != ')') {
                throw new IllegalArgumentException("Missing closing parenthesis");
            }
            pos++;
            return value;
        }
        if (c == '-') {
            pos++;
            return parseFactor().negate();
        }
        if (c == '+') {
            pos++;
            return parseFactor();
        }
        if (Character.isDigit(c) || c == '.') {
            return parseNumber();
        }
        if (Character.isLetter(c) || c == '_') {
            return resolveIdentifier(parseIdentifier());
        }
        throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + pos);
    }

    private BigDecimal parseNumber() {
        int start = pos;
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
            pos++;
        }
        try {
            return new BigDecimal(input.substring(start, pos));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + input.substring(start, pos));
        }
    }

    private String parseIdentifier() {
        int start = pos;
        while (pos < input.length()
                && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_'
                    || input.charAt(pos) == '-' || input.charAt(pos) == '.')) {
            pos++;
        }
        return input.substring(start, pos);
    }

    private BigDecimal resolveIdentifier(String name) {
        BigDecimal value = resolver.apply(name);
        if (value == null) {
            throw new IllegalArgumentException("Unknown KPI reference '" + name + "'");
        }
        return value;
    }

    private char peek() {
        return pos < input.length() ? input.charAt(pos) : '\0';
    }

    private void skipSpaces() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }
}
