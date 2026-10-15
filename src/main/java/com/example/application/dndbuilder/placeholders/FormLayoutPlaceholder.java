package com.example.application.dndbuilder.placeholders;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;

public class FormLayoutPlaceholder extends FormLayout implements Placeholder {

    @Override
    public Class<? extends Component> getRepresentedType() {
        return FormLayout.class;
    }
}
