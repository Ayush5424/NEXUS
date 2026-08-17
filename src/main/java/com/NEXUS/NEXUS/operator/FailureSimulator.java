package com.NEXUS.NEXUS.operator;

import org.springframework.stereotype.Service;

@Service
public class FailureSimulator {

    private volatile FailureMode mode = FailureMode.NORMAL;

    public FailureMode getMode() {
        return mode;
    }

    public void setMode(FailureMode mode) {
        this.mode = mode;
    }

    public boolean shouldFail() {
        return mode == FailureMode.FAIL || mode == FailureMode.DEPENDENCY_DOWN;
    }

    public boolean shouldRunSlowly() {
        return mode == FailureMode.SLOW;
    }

    public boolean isDependencyDown() {
        return mode == FailureMode.DEPENDENCY_DOWN;
    }

    public boolean hasCacheDisagreement() {
        return mode == FailureMode.CACHE_DISAGREE;
    }
}
