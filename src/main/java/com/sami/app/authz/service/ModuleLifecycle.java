package com.sami.app.authz.service;

import com.sami.app.authz.domain.AppModule;
import com.sami.app.authz.domain.ModuleStatus;
import org.springframework.stereotype.Component;

/**
 * Derives a module's overall lifecycle status from its two axes.
 *
 * <p>Pure and stateless so the rule can be unit-tested exhaustively without a
 * database — and so there is exactly one place the rule lives.
 *
 * <p><b>The rule.</b> A module is only as usable as its weakest half, so the
 * overall status is the LESS advanced of backend and frontend. The exception is
 * end-of-life: if either axis is terminal (deprecated, retired) that dominates,
 * because a module being withdrawn is not made healthy by its other half still
 * working.
 *
 * <p>Both branches compare {@code lifecycleRank} and read flags. No status code
 * is named anywhere in this class, which is what allows the lifecycle to be
 * reordered or extended from the {@code module_statuses} table alone.
 */
@Component
public class ModuleLifecycle {

    /**
     * The effective overall status.
     *
     * <p>An explicitly pinned {@code overallStatus} always wins: an
     * administrator who has stated the answer is not overridden by a
     * derivation.
     */
    public ModuleStatus overallStatus(AppModule module) {
        if (module.getOverallStatus() != null) {
            return module.getOverallStatus();
        }
        return derive(module.getBackendStatus(), module.getFrontendStatus());
    }

    /** @return true when the overall status was calculated rather than pinned */
    public boolean isDerived(AppModule module) {
        return module.getOverallStatus() == null;
    }

    /**
     * The derivation itself, exposed as a pure function of two statuses so it
     * can be exercised directly across the whole stage matrix.
     */
    public ModuleStatus derive(ModuleStatus backend, ModuleStatus frontend) {
        if (backend == null) {
            return frontend;
        }
        if (frontend == null) {
            return backend;
        }
        // End of life dominates; if both are terminal take the further-gone.
        if (backend.isTerminal() || frontend.isTerminal()) {
            if (backend.isTerminal() && frontend.isTerminal()) {
                return backend.getLifecycleRank() >= frontend.getLifecycleRank() ? backend : frontend;
            }
            return backend.isTerminal() ? backend : frontend;
        }
        return backend.getLifecycleRank() <= frontend.getLifecycleRank() ? backend : frontend;
    }

    /**
     * Whether the UI should render the placeholder instead of real content.
     *
     * <p>True when EITHER the frontend axis says no screens exist, OR the
     * module as a whole is not usable. Both halves are needed:
     *
     * <ul>
     *   <li>the frontend axis, because a finished backend does not make a
     *       missing screen renderable — the common case here;</li>
     *   <li>the overall status, because finished screens with no backend
     *       behind them would render and then fail every request, and because
     *       a retired module must not present a working-looking UI.</li>
     * </ul>
     */
    public boolean showsPlaceholder(AppModule module) {
        ModuleStatus frontend = module.getFrontendStatus();
        ModuleStatus overall = overallStatus(module);
        boolean byFrontend = frontend != null && frontend.isShowsPlaceholder();
        boolean byOverall = overall != null && overall.isShowsPlaceholder();
        return byFrontend || byOverall;
    }

    /** Whether a user may open the module at all. */
    public boolean isNavigable(AppModule module) {
        ModuleStatus overall = overallStatus(module);
        return module.isAvailable() && (overall == null || overall.isNavigable());
    }
}
