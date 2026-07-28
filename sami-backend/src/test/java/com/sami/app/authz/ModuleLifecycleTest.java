package com.sami.app.authz;

import com.sami.app.authz.domain.AppModule;
import com.sami.app.authz.domain.ModuleStatus;
import com.sami.app.authz.service.ModuleLifecycle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The overall-status derivation.
 *
 * <p>This rule decides what every user sees on every module, so it is tested
 * against the seeded stages directly rather than through the service. The
 * ranks mirror V25's seed.
 */
class ModuleLifecycleTest {

    private final ModuleLifecycle lifecycle = new ModuleLifecycle();

    private static ModuleStatus status(String code, int rank, boolean terminal,
                                       boolean placeholder, boolean navigable) {
        return ModuleStatus.builder()
                .code(code).name(code)
                .lifecycleRank(rank)
                .isTerminal(terminal)
                .showsPlaceholder(placeholder)
                .isNavigable(navigable)
                .appliesToBackend(true).appliesToFrontend(true)
                .build();
    }

    private static final ModuleStatus PLANNED = status("PLANNED", 10, false, true, true);
    private static final ModuleStatus IN_DEV = status("IN_DEVELOPMENT", 20, false, true, true);
    private static final ModuleStatus BACKEND_READY = status("BACKEND_READY", 40, false, true, true);
    private static final ModuleStatus FRONTEND_READY = status("FRONTEND_READY", 40, false, false, true);
    private static final ModuleStatus BETA = status("BETA", 60, false, false, true);
    private static final ModuleStatus ACTIVE = status("ACTIVE", 80, false, false, true);
    private static final ModuleStatus DEPRECATED = status("DEPRECATED", 90, true, false, true);
    private static final ModuleStatus RETIRED = status("RETIRED", 100, true, true, false);

    private static AppModule module(ModuleStatus backend, ModuleStatus frontend) {
        return AppModule.builder()
                .code("test").name("Test")
                .backendStatus(backend).frontendStatus(frontend)
                .isAvailable(true)
                .build();
    }

    @Nested
    @DisplayName("derivation from the two axes")
    class Derivation {

        /**
         * The case this whole change exists for: a finished backend with no
         * screens must not report itself as complete, nor as un-started.
         */
        @Test
        void reportsTheLessAdvancedAxis() {
            AppModule m = module(BACKEND_READY, PLANNED);
            assertThat(lifecycle.overallStatus(m).getCode()).isEqualTo("PLANNED");
        }

        @Test
        void worksInTheOppositeDirectionToo() {
            AppModule m = module(IN_DEV, FRONTEND_READY);
            assertThat(lifecycle.overallStatus(m).getCode()).isEqualTo("IN_DEVELOPMENT");
        }

        @Test
        void reportsActiveOnlyWhenBothAxesAre() {
            assertThat(lifecycle.overallStatus(module(ACTIVE, ACTIVE)).getCode()).isEqualTo("ACTIVE");
            assertThat(lifecycle.overallStatus(module(ACTIVE, BETA)).getCode()).isEqualTo("BETA");
        }

        @Test
        void isSymmetric() {
            assertThat(lifecycle.derive(BACKEND_READY, PLANNED))
                    .isSameAs(lifecycle.derive(PLANNED, BACKEND_READY));
        }

        @Test
        void tolerantOfAMissingAxis() {
            assertThat(lifecycle.derive(null, ACTIVE)).isSameAs(ACTIVE);
            assertThat(lifecycle.derive(ACTIVE, null)).isSameAs(ACTIVE);
        }
    }

    @Nested
    @DisplayName("end-of-life dominates")
    class Terminal {

        /**
         * Without this, min-rank would report a retired module as ACTIVE
         * because ACTIVE(80) ranks below RETIRED(100) — the module would look
         * healthy while being withdrawn.
         */
        @Test
        void aRetiredAxisOutranksAHealthyOne() {
            assertThat(lifecycle.overallStatus(module(RETIRED, ACTIVE)).getCode()).isEqualTo("RETIRED");
            assertThat(lifecycle.overallStatus(module(ACTIVE, RETIRED)).getCode()).isEqualTo("RETIRED");
        }

        @Test
        void deprecationDominatesAnUnfinishedAxis() {
            assertThat(lifecycle.overallStatus(module(DEPRECATED, PLANNED)).getCode())
                    .isEqualTo("DEPRECATED");
        }

        @Test
        void whenBothAreTerminalTheFurtherGoneWins() {
            assertThat(lifecycle.derive(DEPRECATED, RETIRED).getCode()).isEqualTo("RETIRED");
            assertThat(lifecycle.derive(RETIRED, DEPRECATED).getCode()).isEqualTo("RETIRED");
        }
    }

    @Nested
    @DisplayName("explicit override")
    class Override {

        @Test
        void aPinnedStatusBeatsDerivation() {
            AppModule m = module(BACKEND_READY, PLANNED);
            m.setOverallStatus(BETA);

            assertThat(lifecycle.overallStatus(m).getCode()).isEqualTo("BETA");
            assertThat(lifecycle.isDerived(m)).isFalse();
        }

        @Test
        void derivationResumesWhenTheOverrideIsCleared() {
            AppModule m = module(BACKEND_READY, PLANNED);
            m.setOverallStatus(BETA);
            m.setOverallStatus(null);

            assertThat(lifecycle.isDerived(m)).isTrue();
            assertThat(lifecycle.overallStatus(m).getCode()).isEqualTo("PLANNED");
        }
    }

    @Nested
    @DisplayName("placeholder and navigability")
    class Presentation {

        /** A finished backend does not make a missing screen renderable. */
        @Test
        void aMissingFrontendShowsThePlaceholderDespiteAFinishedBackend() {
            assertThat(lifecycle.showsPlaceholder(module(ACTIVE, PLANNED))).isTrue();
        }

        /**
         * The mirror case: screens exist but nothing serves them. Rendering
         * the real view would produce a page that fails every request, so the
         * placeholder is both safer and more truthful.
         */
        @Test
        void finishedScreensWithNoBackendStillShowThePlaceholder() {
            assertThat(lifecycle.showsPlaceholder(module(PLANNED, FRONTEND_READY))).isTrue();
        }

        @Test
        void bothAxesActiveRendersRealContent() {
            assertThat(lifecycle.showsPlaceholder(module(ACTIVE, ACTIVE))).isFalse();
        }

        /** A retired module must not present a working-looking UI. */
        @Test
        void aRetiredModuleShowsThePlaceholderEvenWithAFinishedFrontend() {
            assertThat(lifecycle.showsPlaceholder(module(RETIRED, ACTIVE))).isTrue();
        }

        @Test
        void unavailableModulesAreNotNavigable() {
            AppModule m = module(ACTIVE, ACTIVE);
            m.setAvailable(false);
            assertThat(lifecycle.isNavigable(m)).isFalse();
        }

        @Test
        void retiredModulesAreNotNavigable() {
            assertThat(lifecycle.isNavigable(module(RETIRED, RETIRED))).isFalse();
        }

        @Test
        void activeModulesAreNavigable() {
            assertThat(lifecycle.isNavigable(module(ACTIVE, ACTIVE))).isTrue();
        }
    }
}
