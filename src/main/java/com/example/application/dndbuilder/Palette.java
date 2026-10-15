package com.example.application.dndbuilder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;

import java.util.List;

/**
 * The component types offered in the builder's palette, by group. Every leaf type needs a public constructor taking
 * its label or text as the only argument; that is how the designer creates it and how the generated code reads. The
 * exceptions are the grid, which gets column headers instead, and its column, which only goes onto a grid.
 */
final class Palette {

    static final List<Group> GROUPS = List.of(
            new Group("Layouts", List.of(VerticalLayout.class, HorizontalLayout.class, FormLayout.class)),
            new Group("Inputs", List.of(TextField.class, PasswordField.class, EmailField.class, IntegerField.class,
                    NumberField.class, TextArea.class, DatePicker.class, TimePicker.class, Checkbox.class,
                    ComboBox.class)),
            new Group("Data", List.of(Grid.class, Grid.Column.class)),
            new Group("Actions", List.of(Button.class)),
            new Group("Text", List.of(H1.class, H2.class, H3.class, Paragraph.class, Span.class)));

    private Palette() {
    }

    record Group(String name, List<Class<? extends Component>> types) {
    }

}
