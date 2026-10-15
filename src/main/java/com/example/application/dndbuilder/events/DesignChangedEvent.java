package com.example.application.dndbuilder.events;

import com.example.application.dndbuilder.DesignNode;

/**
 * The design on the canvas or its saved state changed; listeners show it.
 */
public record DesignChangedEvent(DesignNode design) {
}
