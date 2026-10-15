package com.example.application.dndbuilder.placeholders;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * @author Stefan Uebe
 */
public class VerticalLayoutPlaceholder extends VerticalLayout implements Placeholder {

    @Override
    public Class<? extends Component> getRepresentedType() {
        return VerticalLayout.class;
    }
}
