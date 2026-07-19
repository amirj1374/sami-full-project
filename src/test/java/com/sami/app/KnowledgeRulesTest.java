package com.sami.app;

import com.sami.app.common.exception.ApiException;
import com.sami.app.knowledge.domain.KbApproval;
import com.sami.app.knowledge.domain.KbApprovalStage;
import com.sami.app.knowledge.domain.KbStepType;
import com.sami.app.knowledge.domain.Sop;
import com.sami.app.knowledge.domain.SopStep;
import com.sami.app.knowledge.service.ApprovalChain;
import com.sami.app.knowledge.service.RelationGraph;
import com.sami.app.knowledge.service.SopValidator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the knowledge module's rule classes. Pure: no Spring context,
 * no database. These cover the rules that protect published procedures — an
 * out-of-sequence sign-off, an incomplete SOP reaching the shop floor, and a
 * circular prerequisite chain.
 */
class KnowledgeRulesTest {

    private static KbApprovalStage stage(String code, int order,
                                         boolean optional, boolean isFinal, boolean signature) {
        // No id: BaseEntity ids are generated, and the sequencing rules are
        // expressed in terms of stage order rather than identity.
        return KbApprovalStage.builder()
                .code(code).name(code).stageOrder(order)
                .isOptional(optional).isFinal(isFinal).requiresSignature(signature).build();
    }

    private static KbApproval approval(KbApprovalStage stage, String decision) {
        return KbApproval.builder().articleVersionId(1L).stage(stage).decision(decision).build();
    }

    // =====================================================================
    @Nested
    class ApprovalSequencing {

        private final ApprovalChain chain = new ApprovalChain();

        private final KbApprovalStage author = stage("author", 10, false, false, false);
        private final KbApprovalStage reviewer = stage("reviewer", 20, false, false, false);
        private final KbApprovalStage compliance = stage("compliance", 30, true, false, true);
        private final KbApprovalStage finalStage = stage("final", 40, false, true, true);

        @Test
        void theNextPendingStageIsTheLowestOrderedUndecidedOne() {
            List<KbApproval> approvals = List.of(
                    approval(author, "approved"),
                    approval(reviewer, "pending"),
                    approval(finalStage, "pending"));

            assertThat(chain.nextPending(approvals)).isPresent()
                    .get().extracting(a -> a.getStage().getCode()).isEqualTo("reviewer");
        }

        @Test
        void anOutOfSequenceDecisionIsRefused() {
            // Without this, a final approver could sign off a procedure the
            // technical reviewer never saw, while the trail looked complete.
            KbApproval finalApproval = approval(finalStage, "pending");
            List<KbApproval> approvals = List.of(
                    approval(author, "approved"), approval(reviewer, "pending"), finalApproval);

            assertThatThrownBy(() -> chain.assertCanDecide(approvals, finalApproval))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("cannot be decided before");
        }

        @Test
        void decidingTheCorrectNextStageIsAllowed() {
            KbApproval reviewerApproval = approval(reviewer, "pending");
            List<KbApproval> approvals = List.of(
                    approval(author, "approved"), reviewerApproval, approval(finalStage, "pending"));

            assertThatCode(() -> chain.assertCanDecide(approvals, reviewerApproval))
                    .doesNotThrowAnyException();
        }

        @Test
        void aStageCannotBeDecidedTwice() {
            KbApproval already = approval(reviewer, "approved");

            assertThatThrownBy(() -> chain.assertCanDecide(List.of(already), already))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("already been decided");
        }

        @Test
        void fullApprovalIgnoresOptionalStages() {
            // Compliance is optional and still pending; the chain is complete.
            List<KbApproval> approvals = List.of(
                    approval(author, "approved"),
                    approval(reviewer, "approved"),
                    approval(compliance, "pending"),
                    approval(finalStage, "approved"));

            assertThat(chain.isFullyApproved(approvals)).isTrue();
        }

        @Test
        void aSkippedStageCountsAsApproved() {
            assertThat(chain.isFullyApproved(List.of(
                    approval(author, "approved"), approval(reviewer, "skipped")))).isTrue();
        }

        @Test
        void aPendingMandatoryStageBlocksFullApproval() {
            assertThat(chain.isFullyApproved(List.of(
                    approval(author, "approved"), approval(reviewer, "pending")))).isFalse();
        }

        @Test
        void anyRejectionMarksTheChainRejected() {
            assertThat(chain.isRejected(List.of(
                    approval(author, "approved"), approval(reviewer, "rejected")))).isTrue();
        }

        @Test
        void aStageRequiringASignatureRefusesApprovalWithoutOne() {
            assertThatThrownBy(() -> chain.assertSignatureSupplied(finalStage, "  "))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("requires an electronic signature");

            assertThatCode(() -> chain.assertSignatureSupplied(finalStage, "sig-abc"))
                    .doesNotThrowAnyException();
            // A stage that does not require one is unaffected.
            assertThatCode(() -> chain.assertSignatureSupplied(reviewer, null))
                    .doesNotThrowAnyException();
        }
    }

    // =====================================================================
    @Nested
    class SopCompleteness {

        private final SopValidator validator = new SopValidator();

        private static KbStepType type(String code, boolean decision, boolean conditional) {
            return KbStepType.builder().code(code).name(code)
                    .isDecision(decision).isConditional(conditional).build();
        }

        private static SopStep step(int number, String instruction, KbStepType type,
                                    Map<String, Object> branch, Map<String, Object> condition) {
            return SopStep.builder().stepNumber(number).title("Step " + number)
                    .instruction(instruction).stepType(type)
                    .branchConfig(branch == null ? Map.of() : branch)
                    .conditionConfig(condition == null ? Map.of() : condition)
                    .build();
        }

        private static Sop sop() {
            return Sop.builder().sopNumber("SOP-00001")
                    .purpose("Receive devices for repair").scope("All service centres").build();
        }

        @Test
        void acceptsACompleteProcedure() {
            assertThatCode(() -> validator.assertPublishable(sop(), List.of(
                    step(1, "Verify customer identity", type("sequential", false, false), null, null),
                    step(2, "Record IMEI", type("sequential", false, false), null, null))))
                    .doesNotThrowAnyException();
        }

        @Test
        void refusesAProcedureWithNoSteps() {
            assertThatThrownBy(() -> validator.assertPublishable(sop(), List.of()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("at least one step is required");
        }

        @Test
        void refusesMissingPurposeAndScopeTogether() {
            Sop bare = Sop.builder().sopNumber("SOP-00002").build();

            // Both problems are reported in one message so the author fixes one round.
            assertThatThrownBy(() -> validator.assertPublishable(bare, List.of(
                    step(1, "Do the thing", type("sequential", false, false), null, null))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("purpose is required")
                    .hasMessageContaining("scope is required");
        }

        @Test
        void refusesAStepWithNoInstruction() {
            assertThatThrownBy(() -> validator.assertPublishable(sop(), List.of(
                    step(1, "  ", type("sequential", false, false), null, null))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("step 1 has no instruction");
        }

        @Test
        void refusesDuplicateStepNumbers() {
            assertThatThrownBy(() -> validator.assertPublishable(sop(), List.of(
                    step(1, "First", type("sequential", false, false), null, null),
                    step(1, "Also first", type("sequential", false, false), null, null))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("step number 1 is duplicated");
        }

        @Test
        void refusesADecisionStepWithNoBranches() {
            // A decision with nowhere to go strands the reader exactly where they
            // most need guidance.
            assertThatThrownBy(() -> validator.assertPublishable(sop(), List.of(
                    step(1, "Is the device under warranty?", type("decision", true, false), null, null))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("decision step 1 has no branches");
        }

        @Test
        void refusesABranchPointingAtAMissingStep() {
            assertThatThrownBy(() -> validator.assertPublishable(sop(), List.of(
                    step(1, "Under warranty?", type("decision", true, false),
                            Map.of("yes", 2, "no", 99), null),
                    step(2, "Process warranty claim", type("sequential", false, false), null, null))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("branch 'no' points at missing step 99");
        }

        @Test
        void acceptsADecisionStepWhoseBranchesAllResolve() {
            assertThatCode(() -> validator.assertPublishable(sop(), List.of(
                    step(1, "Under warranty?", type("decision", true, false),
                            Map.of("yes", 2, "no", 3), null),
                    step(2, "Process warranty claim", type("sequential", false, false), null, null),
                    step(3, "Quote paid repair", type("sequential", false, false), null, null))))
                    .doesNotThrowAnyException();
        }

        @Test
        void refusesAConditionalStepWithNoCondition() {
            assertThatThrownBy(() -> validator.assertPublishable(sop(), List.of(
                    step(1, "Apply discount", type("conditional", false, true), null, null))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("conditional step 1 has no condition");
        }
    }

    // =====================================================================
    @Nested
    class CycleDetection {

        private final RelationGraph graph = new RelationGraph();

        @Test
        void allowsAnEdgeThatDoesNotCloseACycle() {
            // 1 -> 2 exists; adding 2 -> 3 is fine.
            assertThatCode(() -> graph.assertNoCycle(Map.of(1L, List.of(2L)), 2L, 3L))
                    .doesNotThrowAnyException();
        }

        @Test
        void refusesADirectCycle() {
            // 1 -> 2 exists; adding 2 -> 1 closes it.
            assertThatThrownBy(() -> graph.assertNoCycle(Map.of(1L, List.of(2L)), 2L, 1L))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("circular dependency");
        }

        @Test
        void refusesAnIndirectCycleThroughAChain() {
            // 1 -> 2 -> 3 exists; adding 3 -> 1 closes a three-hop cycle.
            Map<Long, List<Long>> edges = Map.of(1L, List.of(2L), 2L, List.of(3L));

            assertThatThrownBy(() -> graph.assertNoCycle(edges, 3L, 1L))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("circular dependency");
        }

        @Test
        void refusesASelfReference() {
            assertThatThrownBy(() -> graph.assertNoCycle(Map.of(), 5L, 5L))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("cannot reference itself");
        }

        @Test
        void reachabilityTerminatesOnAnAlreadyCyclicGraph() {
            // Defensive: pre-existing bad data must not hang the traversal.
            Map<Long, List<Long>> cyclic = Map.of(1L, List.of(2L), 2L, List.of(1L));

            assertThat(graph.reaches(cyclic, 1L, 99L)).isFalse();
            assertThat(graph.reaches(cyclic, 1L, 2L)).isTrue();
        }

        @Test
        void handlesABranchingGraphWithoutFalsePositives() {
            Map<Long, List<Long>> edges = Map.of(
                    1L, List.of(2L, 3L),
                    2L, List.of(4L),
                    3L, List.of(4L));

            // 4 does not reach 1, so 4 -> 1 is a legitimate new edge... it is not:
            // adding 4 -> 1 would close 1->2->4->1. Verify the traversal sees that.
            assertThatThrownBy(() -> graph.assertNoCycle(edges, 4L, 1L))
                    .isInstanceOf(ApiException.class);
            // But 4 -> 5 is genuinely safe.
            assertThatCode(() -> graph.assertNoCycle(edges, 4L, 5L)).doesNotThrowAnyException();
        }
    }
}
