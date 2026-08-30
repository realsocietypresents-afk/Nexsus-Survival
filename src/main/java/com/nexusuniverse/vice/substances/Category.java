package com.nexusuniverse.vice.substances;

/** Every substance falls into one of these -- effect logic lives per-category, not per-substance, so adding a new substance is one enum entry, not new mechanics. */
public enum Category {
    DEPRESSANT,
    STIMULANT,
    HALLUCINOGEN,
    MELLOW,
    DISSOCIATIVE,
    PERFORMANCE,
    EUPHORIC
}
