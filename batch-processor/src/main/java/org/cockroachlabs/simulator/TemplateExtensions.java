package org.cockroachlabs.simulator;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class TemplateExtensions {
    static String formatToTwoDecimalPlaces(Double value) {
        return String.format("%.2f", value);
    }
}

