package com.example.application.dndbuilder.events;

import java.nio.file.Path;

/**
 * A view's design is on the canvas now and can be saved back to its file.
 */
public record ViewLoadedEvent(Path file) {
}
