package com.example.application.dndbuilder;

import com.vaadin.flow.component.Component;

import java.util.List;

/**
 * One component of a design: its type, the text or label passed to its constructor, its placeholder text (both
 * {@code null} for none) and its children in order. The root node stands for the view itself.
 */
public record DesignNode(Class<? extends Component> type, String text, String placeholder, List<DesignNode> children) {

    public DesignNode {
        children = List.copyOf(children);
    }

    public DesignNode(Class<? extends Component> type, String text, List<DesignNode> children) {
        this(type, text, null, children);
    }

    public DesignNode(Class<? extends Component> type, String text, DesignNode... children) {
        this(type, text, null, List.of(children));
    }

}
