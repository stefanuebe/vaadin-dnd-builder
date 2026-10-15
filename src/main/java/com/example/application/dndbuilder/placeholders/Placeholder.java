package com.example.application.dndbuilder.placeholders;

import com.vaadin.flow.component.Component;

/**
 * @author Stefan Uebe
 */
public interface Placeholder {

    default void onDrop() {

    }

    Class<? extends Component> getRepresentedType();

}
