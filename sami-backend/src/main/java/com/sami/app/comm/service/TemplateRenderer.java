package com.sami.app.comm.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders {@code {{variable}}} templates.
 *
 * <p><b>Fail-closed in both directions.</b> A placeholder not covered by a
 * value, and a value for a variable the template never declared, are both
 * rejected — the first because "Dear {{name}}" must never reach a customer,
 * the second because it is almost always a caller bug (a typo'd variable name
 * that would otherwise silently vanish).
 *
 * <p>Pure and stateless: the whole rendering contract is unit-testable with no
 * database. Deliberately NOT a general expression language — no conditionals,
 * no loops, no method calls. Templates are text with named holes; anything
 * smarter belongs in the caller.
 */
@Component
public class TemplateRenderer {

    /** {{name}} — letters, digits, dot and underscore, trimmed of whitespace. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*}}");

    /** The placeholder names a template body references, in order of first use. */
    public Set<String> referencedVariables(String template) {
        if (template == null || template.isBlank()) {
            return Set.of();
        }
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    /**
     * Validates a template against its declared variable list — used when a
     * template is saved, so a broken template is rejected at authoring time
     * rather than at send time.
     *
     * @throws ApiException when the body references an undeclared variable
     */
    public void validateDeclaration(String template, List<String> declared) {
        Set<String> referenced = referencedVariables(template);
        List<String> declaredSafe = declared == null ? List.of() : declared;
        List<String> undeclared = referenced.stream()
                .filter(name -> !declaredSafe.contains(name))
                .toList();
        if (!undeclared.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Template references undeclared variable(s): %s".formatted(String.join(", ", undeclared)));
        }
    }

    /**
     * Renders the template with the given values.
     *
     * @throws ApiException when a placeholder has no value, or a value names no
     *                      placeholder
     */
    public String render(String template, Map<String, Object> values) {
        if (template == null) {
            return null;
        }
        Map<String, Object> safe = values == null ? Map.of() : values;
        Set<String> referenced = referencedVariables(template);

        List<String> missing = referenced.stream()
                .filter(name -> !safe.containsKey(name) || safe.get(name) == null)
                .toList();
        if (!missing.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Missing template variable(s): %s".formatted(String.join(", ", missing)));
        }

        List<String> surplus = safe.keySet().stream()
                .filter(name -> !referenced.contains(name))
                .toList();
        if (!surplus.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Value(s) supplied for variable(s) the template does not use: %s"
                            .formatted(String.join(", ", surplus)));
        }

        StringBuilder out = new StringBuilder();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            // quoteReplacement: a value containing '$' or '\' must land verbatim.
            matcher.appendReplacement(out,
                    Matcher.quoteReplacement(String.valueOf(safe.get(matcher.group(1)))));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
