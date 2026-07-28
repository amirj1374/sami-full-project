package com.sami.app.knowledge.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.knowledge.domain.Sop;
import com.sami.app.knowledge.domain.SopStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Completeness and consistency rules for procedures.
 *
 * <p>Pure logic, so the rules are unit-testable. The publication check exists
 * because a half-written SOP that reaches the shop floor is worse than none:
 * staff follow it and discover the gap mid-procedure.
 */
@Component
public class SopValidator {

    /**
     * Everything that must be true before a procedure may be published.
     * Collects all problems rather than failing on the first, so an author fixes
     * one round of issues instead of discovering them one at a time.
     */
    public void assertPublishable(Sop sop, List<SopStep> steps) {
        List<String> problems = new ArrayList<>();

        if (sop.getPurpose() == null || sop.getPurpose().isBlank()) {
            problems.add("purpose is required");
        }
        if (sop.getScope() == null || sop.getScope().isBlank()) {
            problems.add("scope is required");
        }
        if (steps == null || steps.isEmpty()) {
            problems.add("at least one step is required");
        } else {
            problems.addAll(stepProblems(steps));
        }

        if (!problems.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "This procedure cannot be published: " + String.join("; ", problems));
        }
    }

    private List<String> stepProblems(List<SopStep> steps) {
        List<String> problems = new ArrayList<>();
        Set<Integer> numbers = new HashSet<>();

        for (SopStep step : steps) {
            if (!numbers.add(step.getStepNumber())) {
                problems.add("step number %d is duplicated".formatted(step.getStepNumber()));
            }
            if (step.getInstruction() == null || step.getInstruction().isBlank()) {
                problems.add("step %d has no instruction".formatted(step.getStepNumber()));
            }
            // A decision step that does not say where each outcome leads leaves the
            // reader stranded at exactly the point they need guidance.
            if (step.getStepType() != null && step.getStepType().isDecision()
                    && (step.getBranchConfig() == null || step.getBranchConfig().isEmpty())) {
                problems.add("decision step %d has no branches".formatted(step.getStepNumber()));
            }
            if (step.getStepType() != null && step.getStepType().isConditional()
                    && (step.getConditionConfig() == null || step.getConditionConfig().isEmpty())) {
                problems.add("conditional step %d has no condition".formatted(step.getStepNumber()));
            }
        }

        problems.addAll(danglingBranches(steps, numbers));
        return problems;
    }

    /** A branch pointing at a step number that does not exist. */
    private List<String> danglingBranches(List<SopStep> steps, Set<Integer> numbers) {
        List<String> problems = new ArrayList<>();
        for (SopStep step : steps) {
            if (step.getBranchConfig() == null) {
                continue;
            }
            for (var entry : step.getBranchConfig().entrySet()) {
                if (entry.getValue() instanceof Number target
                        && !numbers.contains(target.intValue())) {
                    problems.add("step %d branch '%s' points at missing step %s"
                            .formatted(step.getStepNumber(), entry.getKey(), target));
                }
            }
        }
        return problems;
    }
}
