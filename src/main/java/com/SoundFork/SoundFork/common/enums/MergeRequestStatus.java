package com.SoundFork.SoundFork.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MergeRequestStatus {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    private final String value;

    MergeRequestStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
