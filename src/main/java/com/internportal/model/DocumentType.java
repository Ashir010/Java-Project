package com.internportal.model;

public enum DocumentType {
    OFFER_LETTER("Offer Letter", "OL"),
    COMPLETION_CERTIFICATE("Completion Certificate", "CC");

    private final String label;
    private final String prefix;

    DocumentType(String label, String prefix) {
        this.label = label;
        this.prefix = prefix;
    }

    /** The name shown to users. */
    public String getLabel() {
        return label;
    }

    /** The start of the reference number, for example OL in OL-2026-00012. */
    public String getPrefix() {
        return prefix;
    }
}
