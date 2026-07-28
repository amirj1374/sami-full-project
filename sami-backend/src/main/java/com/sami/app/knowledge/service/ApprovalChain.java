package com.sami.app.knowledge.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.knowledge.domain.KbApproval;
import com.sami.app.knowledge.domain.KbApprovalStage;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The rules governing the configurable approval chain.
 *
 * <p>Pure logic — no database, no Spring dependencies beyond the bean
 * declaration — so the sequencing rules are unit-testable in isolation. These
 * are the rules that stop a procedure being published without the sign-off it
 * requires, which for a compliance or cash-handling SOP is the whole point.
 */
@Component
public class ApprovalChain {

    /**
     * The stage that must be decided next: the lowest-ordered stage still
     * pending. Empty when every stage has been decided.
     */
    public Optional<KbApproval> nextPending(List<KbApproval> approvals) {
        return approvals.stream()
                .filter(a -> !a.isDecided())
                .min(Comparator.comparingInt(a -> a.getStage().getStageOrder()));
    }

    /**
     * Rejects an out-of-sequence decision.
     *
     * <p>Without this, a final approver could sign off a procedure that a
     * technical reviewer had not yet seen — the approval trail would look
     * complete while the review never happened.
     */
    public void assertCanDecide(List<KbApproval> approvals, KbApproval target) {
        if (target.isDecided()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Stage '%s' has already been decided (%s)"
                            .formatted(target.getStage().getName(), target.getDecision()));
        }
        // Compared by stage ORDER, not by id: order is what "sequence" actually
        // means here, and it works for a stage that has not been persisted yet.
        Optional<KbApproval> expected = nextPending(approvals);
        if (expected.isPresent()
                && expected.get().getStage().getStageOrder() != target.getStage().getStageOrder()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Stage '%s' cannot be decided before '%s'".formatted(
                            target.getStage().getName(), expected.get().getStage().getName()));
        }
    }

    /** True when every non-optional stage has been approved. */
    public boolean isFullyApproved(List<KbApproval> approvals) {
        return approvals.stream()
                .filter(a -> !a.getStage().isOptional())
                .allMatch(KbApproval::isApproved);
    }

    /** True when any stage was rejected — the chain stops there. */
    public boolean isRejected(List<KbApproval> approvals) {
        return approvals.stream().anyMatch(a -> "rejected".equals(a.getDecision()));
    }

    /**
     * A stage requiring a signature cannot be approved without one. Enforced here
     * rather than at the call site so it cannot be forgotten by a new caller.
     */
    public void assertSignatureSupplied(KbApprovalStage stage, String signatureRef) {
        if (stage.isRequiresSignature() && (signatureRef == null || signatureRef.isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Stage '%s' requires an electronic signature".formatted(stage.getName()));
        }
    }

    /** The stage whose approval permits publication. */
    public Optional<KbApproval> finalStage(List<KbApproval> approvals) {
        return approvals.stream().filter(a -> a.getStage().isFinal()).findFirst();
    }
}
