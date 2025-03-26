package org.cockroachlabs.simulator;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class TemplateExtensions {
    static String formatNumber(Integer value) {
        return String.format("%,d", value);
    }
    static String formatNumber(Long value) {
        return String.format("%,d", value);
    }
    static String formatNumber(Double value) {
        return String.format("%,.2f", value);
    }
}

