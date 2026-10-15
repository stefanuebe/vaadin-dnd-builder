package com.example.application.dndbuilder.events;

import com.vaadin.flow.component.Component;

/**
 * @author Stefan Uebe
 */
public class StructureHasChangedEvent {
    private final Component source;

    public StructureHasChangedEvent(Component source) {
        this.source = source;
    }

    public Component getSource() {
        return source;
    }
}
