package com.example.application.dndbuilder.placeholders;

import com.example.application.dndbuilder.DesignNode;
import com.example.application.dndbuilder.DndBuilder;
import com.example.application.dndbuilder.DndUtils;
import com.example.application.dndbuilder.events.EventBus;
import com.example.application.dndbuilder.events.StructureHasChangedEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Stefan Uebe
 */
@SpringComponent
@UIScope
public class PlaceholderFactory {

    private static final Map<Class<? extends Component>, Supplier<Component>> CONTAINERS = Map.of(
            VerticalLayout.class, VerticalLayoutPlaceholder::new,
            HorizontalLayout.class, HorizontalLayoutPlaceholder::new,
            FormLayout.class, FormLayoutPlaceholder::new);

    private static final DesignNode NEW_GRID = new DesignNode(Grid.class, null,
            new DesignNode(Grid.Column.class, "Column 1"), new DesignNode(Grid.Column.class, "Column 2"));

    private final DndUtils dndUtils;
    private final DesignerTexts designerTexts;
    private final EventBus eventBus;

    public PlaceholderFactory(DndUtils dndUtils, DesignerTexts designerTexts, EventBus eventBus) {
        this.dndUtils = dndUtils;
        this.designerTexts = designerTexts;
        this.eventBus = eventBus;
    }

    /**
     * Creates a designer component for a palette entry; leaf components get their readable name as text.
     */
    public Component createPlaceholder(Class<? extends Component> componentType) {
        if (componentType == Grid.class) {
            return fromDesignTree(NEW_GRID);
        }

        String text = isContainer(componentType) ? null : dndUtils.getReadableName(componentType);

        return createDesignerComponent(componentType, text, null);
    }

    /**
     * Creates the designer components for the given design tree, nested like the tree.
     */
    public Component fromDesignTree(DesignNode node) {
        Component component = createDesignerComponent(node.type(), node.text(), node.placeholder());
        if (component instanceof Grid<?> grid) {
            designerTexts.setColumnHeaders(grid, node.children().stream().map(DesignNode::text).toList());
            return component;
        }

        for (DesignNode child : node.children()) {
            component.getElement().appendChild(fromDesignTree(child).getElement());
        }

        return component;
    }

    public boolean isContainer(Class<? extends Component> componentType) {
        return CONTAINERS.containsKey(componentType);
    }

    private Component createDesignerComponent(Class<? extends Component> componentType, String text, String placeholder) {
        Supplier<Component> container = CONTAINERS.get(componentType);
        if (container != null) {
            return applySettingsToPlaceholder(container.get(), true);
        }

        if (componentType == Grid.class) {
            Grid<String> grid = new Grid<>();
            grid.setItems("", "", ""); // blank rows, so the designed grid looks like one
            designerTexts.enableEditing(grid);
            DropTarget.create(grid).addDropListener(event -> {
                grid.getUI().ifPresent(dndUtils::endDrag);
                // other components go into the zones around the grid; a drop on the grid itself ignores them
                if (event.getDragData().filter(Grid.Column.class::equals).isPresent()) {
                    designerTexts.addColumn(grid, "Column " + (grid.getColumns().size() + 1));
                    eventBus.fireEvent(new StructureHasChangedEvent(grid));
                }
            });

            return applySettingsToPlaceholder(grid, false);
        }

        try {
            Component component = text == null
                    ? componentType.getConstructor().newInstance()
                    : componentType.getConstructor(String.class).newInstance(text);
            designerTexts.rememberText(component, text);
            designerTexts.setPlaceholder(component, placeholder);
            designerTexts.enableEditing(component);

            return applySettingsToPlaceholder(component, false);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(componentType + " needs a public constructor taking its text or label", e);
        }
    }

    /**
     * Builds the design tree of the given designer component and all designer components nested in it.
     */
    public DesignNode toDesignTree(Component component) {
        Class<? extends Component> type = component instanceof Placeholder placeholder
                ? placeholder.getRepresentedType()
                : component.getClass();
        if (component instanceof Grid<?> grid) {
            List<DesignNode> columns = designerTexts.columnHeadersOf(grid).stream()
                    .map(header -> new DesignNode(Grid.Column.class, header))
                    .toList();

            return new DesignNode(Grid.class, null, columns);
        }

        List<DesignNode> children = component.getChildren()
                .filter(dndUtils::isPlaceholder)
                .map(this::toDesignTree)
                .toList();

        return new DesignNode(type, designerTexts.textOf(component), designerTexts.placeholderOf(component), children);
    }

    private Component applySettingsToPlaceholder(Component component, boolean dropTarget) {
        // make component movable to somewhere else or deletable via trash
        Element element = component.getElement();
        element.setProperty(DndUtils.PLACEHOLDER_MARKER, true);
        dndUtils.makeSimpleDraggable(component);

        // make draggable
        ClassList classList = element.getClassList();

        // containers accept drops; decided by the container types, not by HasComponents (headings are HTML containers)
        if (dropTarget) {
            classList.add(DndBuilder.CSS_DROP_TARGET);
            classList.add(DndBuilder.CSS_HAS_COMPONENTS);

            dndUtils.makeSimpleDroppable(component, this::getOrCreatePlaceholder);
        }

        // containers have nothing to edit, so their tooltip gets no Edit button
        element.executeJs("window._dndbuilder.applyTippy(this, $0, $1);", dndUtils.getReadableName(component.getClass()),
                !dropTarget);

        return component;
    }

    /**
     * Puts a drop zone in front of every component of the given designer container and its nested ones, and one at
     * the end, so a drop can go between any two components; old zones are replaced. The zones show only while
     * dragging (styles.css).
     */
    public void refreshDropZones(Component component) {
        if (!(component instanceof Placeholder container) || !isContainer(container.getRepresentedType())) {
            return;
        }

        Element element = component.getElement();
        element.getChildren()
                .filter(child -> child.getClassList().contains(DndBuilder.CSS_DROP_ZONE))
                .toList()
                .forEach(element::removeChild);

        for (Component child : component.getChildren().toList()) {
            element.insertChild(element.indexOfChild(child.getElement()), createDropZone(component).getElement());
            refreshDropZones(child);
        }
        element.appendChild(createDropZone(component).getElement());
    }

    private Component createDropZone(Component container) {
        Div zone = new Div();
        zone.addClassName(DndBuilder.CSS_DROP_ZONE);
        if (container instanceof FormLayout) {
            zone.getElement().setAttribute("colspan", "99"); // a row of its own; capped to the column count
        }
        dndUtils.makeSimpleDroppable(zone, container, this::getOrCreatePlaceholder);

        return zone;
    }

    /**
     * Returns the designer component a drop brings: a new one for a palette entry, the moved one otherwise, or
     * {@code null} for a grid column, which only a grid takes.
     */
    public Component getOrCreatePlaceholder(DropEvent<?> dropEvent) {
        Object dragData = dropEvent.getDragData().orElseThrow(() -> new IllegalArgumentException("No drag data present"));

        if (dragData == Grid.Column.class) {
            return null; // a column goes only onto a grid, which takes it itself
        }

        if (dragData instanceof Class) {
            return createPlaceholder((Class<? extends Component>) dragData);
        }

        if (!dndUtils.isPlaceholder(dragData)) {
            throw new UnsupportedOperationException("Drag data is not a placeholder nor a placeholder scheme, cannot be handled");
        }

        return (Component) dragData;
    }

}
