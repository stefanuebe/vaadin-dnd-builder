package com.example.application.dndbuilder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaletteTest {

    /** Containers have no text; the grid has column headers instead, which its columns bring. */
    private static final Set<Class<?>> WITHOUT_TEXT = Set.of(VerticalLayout.class, HorizontalLayout.class, FormLayout.class,
            Grid.class, Grid.Column.class);

    /**
     * The designer creates leaves with their text as the only constructor argument and edits that text as label or
     * text afterwards; both must address the same property, or an edit would not show in the generated code.
     */
    @Test
    void everyLeafType_showsItsConstructorTextAsLabelOrText() throws ReflectiveOperationException {
        for (Palette.Group group : Palette.GROUPS) {
            for (Class<? extends Component> type : group.types()) {
                if (WITHOUT_TEXT.contains(type)) {
                    continue;
                }

                Component component = type.getConstructor(String.class).newInstance("Caption");
                String shown = component instanceof HasLabel field ? field.getLabel() : ((HasText) component).getText();

                assertEquals("Caption", shown, type.getName());
            }
        }
    }

}
