package com.example.application.dndbuilder.events;

import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Stefan Uebe
 */
@SpringComponent
@UIScope
public class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    public <T> Registration addListener(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<?>> list = listeners.computeIfAbsent(eventType, k -> new ArrayList<>());
        list.add(listener);
        return () -> list.remove(listener);
    }

    public <T> void removeListener(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<?>> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    public void fireEvent(Object event) {
        listeners.getOrDefault(event.getClass(), Collections.emptyList()).forEach(consumer -> ((Consumer) consumer).accept(event));
    }

}
