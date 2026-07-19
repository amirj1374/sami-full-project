package com.sami.app.demo;

/**
 * Volume presets for the demo-data generator. Counts are per top-level entity;
 * child rows (contacts, addresses, line items, timeline events, …) scale from
 * these. {@code MEDIUM} is the default and matches "a store with a few years of
 * history".
 */
public enum DemoDataSize {

    SMALL(120, 60, 20, 60),
    MEDIUM(800, 300, 80, 350),
    LARGE(2000, 900, 200, 1000);

    private final int products;
    private final int customers;
    private final int suppliers;
    private final int purchases;

    DemoDataSize(int products, int customers, int suppliers, int purchases) {
        this.products = products;
        this.customers = customers;
        this.suppliers = suppliers;
        this.purchases = purchases;
    }

    public int products() {
        return products;
    }

    public int customers() {
        return customers;
    }

    public int suppliers() {
        return suppliers;
    }

    public int purchases() {
        return purchases;
    }

    /** Lenient parse; anything unrecognised falls back to {@link #MEDIUM}. */
    public static DemoDataSize from(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MEDIUM;
        }
    }
}
