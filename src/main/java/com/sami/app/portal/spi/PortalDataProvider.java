package com.sami.app.portal.spi;

import java.util.List;
import java.util.Map;

/**
 * THE integration contract for the customer portal.
 *
 * <p>The portal owns no business data. Everything a customer sees about
 * installments, repairs, warranties, invoices or trade-ins is fetched at read
 * time from the module that owns it, through this interface. A provider bean
 * lives in the owning module, never here.
 *
 * <p>A widget whose provider is not registered is reported as unavailable
 * rather than rendered empty — an empty panel looks like "you have no
 * installments", which is a very different statement from "this feature is not
 * installed yet".
 *
 * <p>Implementations MUST scope every query to {@link PortalContext#customerId()}.
 * The portal cannot enforce that for them: it is the owning module that knows
 * how its data relates to a customer.
 */
public interface PortalDataProvider {

    /** Matches {@code portal_widgets.provider_key}. */
    String key();

    /** Human-readable description, surfaced in the widget catalogue. */
    default String description() {
        return key();
    }

    /**
     * @return the widget payload for this customer, scoped to their own data
     */
    WidgetData fetch(PortalContext context);

    /**
     * @param title    heading for the widget
     * @param summary  headline values, e.g. {@code {"outstanding": 3, "nextDue": "2026-08-01"}}
     * @param items    row data, each a flat map for rendering
     * @param actions  action codes the customer may take from this widget
     */
    record WidgetData(String title,
                      Map<String, Object> summary,
                      List<Map<String, Object>> items,
                      List<String> actions) {

        public static WidgetData empty(String title) {
            return new WidgetData(title, Map.of(), List.of(), List.of());
        }

        public static WidgetData of(String title, Map<String, Object> summary,
                                    List<Map<String, Object>> items) {
            return new WidgetData(title, summary, items, List.of());
        }
    }
}
