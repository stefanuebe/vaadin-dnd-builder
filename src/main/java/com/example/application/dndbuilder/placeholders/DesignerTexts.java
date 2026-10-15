package com.example.application.dndbuilder.placeholders;

import com.example.application.dndbuilder.DndUtils;
import com.example.application.dndbuilder.events.EventBus;
import com.example.application.dndbuilder.events.StructureHasChangedEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasPlaceholder;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Stream;

/**
 * The label or text, the placeholder text and the grid column headers of designer components, and editing them in
 * design mode: the Edit button of a component's tooltip, or a click on a field's label (or a button's or heading's
 * text), opens a popover; typing into a text field sets its placeholder text.
 */
@SpringComponent
@UIScope
public class DesignerTexts {

    private static final String TEXT_KEY = "dnd-builder-text";

    // a field without a label (has-label is Vaadin's marker) opens the editor on any click, or it could not get one back
    private static final String LABEL_CLICK_FILTER = "!element.hasAttribute('has-label')"
            + " || (event.target.localName === 'label' && event.target.parentElement === element)";

    private final DndUtils dndUtils;
    private final EventBus eventBus;

    public DesignerTexts(DndUtils dndUtils, EventBus eventBus) {
        this.dndUtils = dndUtils;
        this.eventBus = eventBus;
    }

    /**
     * The text or label the component was created with or edited to, {@code null} for none.
     */
    public String textOf(Component component) {
        return (String) ComponentUtil.getData(component, TEXT_KEY);
    }

    public void rememberText(Component component, String text) {
        ComponentUtil.setData(component, TEXT_KEY, text);
    }

    public String placeholderOf(Component component) {
        return component instanceof HasPlaceholder field ? StringUtils.defaultIfEmpty(field.getPlaceholder(), null) : null;
    }

    public void setPlaceholder(Component component, String placeholder) {
        if (placeholder != null && component instanceof HasPlaceholder field) {
            field.setPlaceholder(placeholder);
        }
    }

    public List<String> columnHeadersOf(Grid<?> grid) {
        return grid.getColumns().stream().map(column -> StringUtils.defaultString(column.getHeaderText())).toList();
    }

    public void setColumnHeaders(Grid<?> grid, List<String> headers) {
        grid.removeAllColumns();
        headers.forEach(header -> addColumn(grid, header));
    }

    public void addColumn(Grid<?> grid, String header) {
        grid.addColumn(item -> "").setHeader(header);
    }

    public void enableEditing(Component component) {
        DomEventListener edit = event -> {
            if (!dndUtils.isPreviewMode()) {
                openEditor(component);
            }
        };
        component.getElement().addEventListener("dnd-edit", edit); // sent by the tooltip's Edit button, see init-tippy.ts
        var click = component.getElement().addEventListener("click", edit);

        if (component instanceof HasLabel) {
            click.setFilter(LABEL_CLICK_FILTER); // a click into a labelled field itself is for typing
        }

        if (component instanceof HasPlaceholder && component instanceof HasValue<?, ?> field
                && field.getEmptyValue() instanceof String) {
            @SuppressWarnings("unchecked")
            HasValue<?, String> textField = (HasValue<?, String>) field;
            textField.addValueChangeListener(event -> {
                if (event.isFromClient() && !dndUtils.isPreviewMode() && !event.getValue().isEmpty()) {
                    ((HasPlaceholder) component).setPlaceholder(event.getValue());
                    textField.clear();
                    eventBus.fireEvent(new StructureHasChangedEvent(component));
                }
            });
        }
    }

    private void openEditor(Component component) {
        if (component instanceof Grid<?> grid) {
            openColumnsEditor(grid);
            return;
        }

        // eager, so Enter applies what was typed even before the field fires its change event
        TextField text = new TextField(component instanceof HasLabel ? "Label" : "Text");
        text.setValue(StringUtils.defaultString(textOf(component)));
        text.setValueChangeMode(ValueChangeMode.EAGER);

        TextField placeholder = new TextField("Placeholder");
        placeholder.setValue(StringUtils.defaultString(placeholderOf(component)));
        placeholder.setValueChangeMode(ValueChangeMode.EAGER);
        placeholder.setVisible(component instanceof HasPlaceholder);

        openPopover(component, () -> {
            applyText(component, StringUtils.defaultIfEmpty(text.getValue(), null));
            if (component instanceof HasPlaceholder field) {
                field.setPlaceholder(placeholder.getValue());
            }
        }, text, placeholder);
    }

    /**
     * Opens a popover at the component with the given fields and an Apply button (or Enter) that runs {@code apply}.
     */
    private void openPopover(Component component, Runnable apply, Component... fields) {
        Button applyButton = new Button("Apply");

        Popover popover = new Popover(fields);
        popover.add(applyButton);
        popover.setTarget(component);
        popover.setOpenOnClick(false);
        popover.setAutofocus(true);
        popover.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                popover.removeFromParent();
            }
        });

        applyButton.addClickShortcut(Key.ENTER).listenOn(popover);
        applyButton.addClickListener(event -> {
            apply.run();
            popover.close();
            eventBus.fireEvent(new StructureHasChangedEvent(component));
        });

        popover.open(); // setTarget adds the popover to the target's UI
    }

    private void openColumnsEditor(Grid<?> grid) {
        // shortcut: a header cannot contain a comma; switch to one field per column if a demo needs that
        TextField columns = new TextField("Columns (comma separated)");
        columns.setValue(String.join(", ", columnHeadersOf(grid)));
        columns.setValueChangeMode(ValueChangeMode.EAGER);
        columns.setWidth("20em");

        openPopover(grid, () -> setColumnHeaders(grid, Stream.of(columns.getValue().split(","))
                .map(String::strip)
                .filter(header -> !header.isEmpty())
                .toList()), columns);
    }

    private void applyText(Component component, String text) {
        if (component instanceof HasLabel field) {
            field.setLabel(text);
        } else if (component instanceof HasText textComponent) {
            textComponent.setText(text);
        }

        rememberText(component, text);
    }

}
