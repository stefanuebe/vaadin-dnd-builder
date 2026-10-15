package com.example.application.dndbuilder.events;

/**
 * @author Stefan Uebe
 */
public class PreviewModeChanged {

    private final boolean active;

    public PreviewModeChanged(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
