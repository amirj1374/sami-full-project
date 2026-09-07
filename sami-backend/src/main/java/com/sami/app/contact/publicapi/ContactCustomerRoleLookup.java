package com.sami.app.contact.publicapi;

/**
 * Canonical Contact lookup for modules that still carry a preserved legacy
 * Customer reference. Implementations must resolve within the trusted tenant.
 */
public interface ContactCustomerRoleLookup {

    Long requireContactIdForCustomer(Long customerId);
}
