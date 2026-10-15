package com.example.application.dndbuilder.placeholders;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * @author Stefan Uebe
 */
public class HorizontalLayoutPlaceholder extends HorizontalLayout implements Placeholder{

    @Override
    public Class<? extends Component> getRepresentedType() {
        return HorizontalLayout.class;
    }
}
