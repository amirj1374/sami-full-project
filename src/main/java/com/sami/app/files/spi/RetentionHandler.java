package com.sami.app.files.spi;

/**
 * What happens when a retention period expires. One bean per
 * {@code retention_policies.action_on_expiry} value.
 *
 * <p>Handlers must honour legal hold: the caller filters held files out before
 * dispatch, and a handler must never assume it may destroy data.
 */
public interface RetentionHandler {

    /** Matches {@code retention_policies.action_on_expiry}. */
    String action();

    /** @return a short description of what was done, for the audit trail. */
    String apply(Long fileId);
}
