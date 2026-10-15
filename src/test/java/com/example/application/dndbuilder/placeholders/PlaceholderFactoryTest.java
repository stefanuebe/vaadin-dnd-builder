package com.example.application.dndbuilder.placeholders;

import com.example.application.dndbuilder.DesignNode;
import com.example.application.dndbuilder.DndBuilder;
import com.example.application.dndbuilder.DndUtils;
import com.example.application.dndbuilder.events.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceholderFactoryTest {

    private final EventBus eventBus = new EventBus();
    private final DndUtils dndUtils = new DndUtils(eventBus);
    private final PlaceholderFactory factory = new PlaceholderFactory(dndUtils, new DesignerTexts(dndUtils, eventBus), eventBus);

    private final DesignNode design = new DesignNode(VerticalLayout.class, null,
            new DesignNode(Button.class, "First"),
            new DesignNode(HorizontalLayout.class, null, new DesignNode(Button.class, "Inner")),
            new DesignNode(Button.class, "Last"));

    @Test
    void refreshDropZones_putsAZoneBeforeEveryComponentAndAtTheEnd() {
        Component root = factory.fromDesignTree(design);

        factory.refreshDropZones(root);

        assertEquals(List.of("zone", "Button", "zone", "HorizontalLayout", "zone", "Button", "zone"), childKinds(root));
        Component row = root.getChildren().filter(child -> child instanceof HorizontalLayout).findFirst().orElseThrow();
        assertEquals(List.of("zone", "Button", "zone"), childKinds(row));
    }

    @Test
    void refreshDropZones_again_replacesTheZonesAndLeavesTheDesignAlone() {
        Component root = factory.fromDesignTree(design);

        factory.refreshDropZones(root);
        factory.refreshDropZones(root);

        assertEquals(7, root.getChildren().count());
        assertEquals(design, factory.toDesignTree(root));
    }

    @Test
    void dropOnZone_movesTheComponentToThatPlace() {
        Component root = attachedDesign();
        Component last = child(root, 5);

        drop(last, child(root, 0));

        assertEquals(new DesignNode(VerticalLayout.class, null,
                new DesignNode(Button.class, "Last"),
                new DesignNode(Button.class, "First"),
                new DesignNode(HorizontalLayout.class, null, new DesignNode(Button.class, "Inner"))),
                factory.toDesignTree(root));
    }

    @Test
    void dropOnZoneFurtherDown_movesTheComponentDown() {
        Component root = attachedDesign();

        drop(child(root, 1), child(root, 6)); // "First" to the end

        assertEquals(new DesignNode(VerticalLayout.class, null,
                new DesignNode(HorizontalLayout.class, null, new DesignNode(Button.class, "Inner")),
                new DesignNode(Button.class, "Last"),
                new DesignNode(Button.class, "First")),
                factory.toDesignTree(root));
    }

    @Test
    void dropOnZoneOfANestedContainer_movesTheComponentIntoIt() {
        Component root = attachedDesign();
        Component row = child(root, 3);

        drop(child(root, 1), child(row, 2)); // "First" behind "Inner"

        assertEquals(new DesignNode(VerticalLayout.class, null,
                new DesignNode(HorizontalLayout.class, null,
                        new DesignNode(Button.class, "Inner"), new DesignNode(Button.class, "First")),
                new DesignNode(Button.class, "Last")),
                factory.toDesignTree(root));
    }

    @Test
    void dropOfAContainerIntoItself_changesNothing() {
        Component root = attachedDesign();
        Component row = child(root, 3);

        drop(row, child(row, 0));

        assertEquals(design, factory.toDesignTree(root));
    }

    @Test
    void dropOfAGridColumnOnAGrid_addsAColumn() {
        Component root = attachedDesign(gridDesign());

        drop(paletteCard(root), Grid.Column.class, child(root, 1));

        assertEquals(new DesignNode(VerticalLayout.class, null, new DesignNode(Grid.class, null,
                new DesignNode(Grid.Column.class, "Name"), new DesignNode(Grid.Column.class, "Column 2"))),
                factory.toDesignTree(root));
    }

    @Test
    void dropOfAGridColumnOutsideAGrid_changesNothing() {
        Component root = attachedDesign(gridDesign());

        drop(paletteCard(root), Grid.Column.class, child(root, 0));

        assertEquals(gridDesign(), factory.toDesignTree(root));
    }

    private static DesignNode gridDesign() {
        return new DesignNode(VerticalLayout.class, null,
                new DesignNode(Grid.class, null, new DesignNode(Grid.Column.class, "Name")));
    }

    /**
     * A palette entry's card, attached to the UI like the drawer; dropping it carries the component type as drag data.
     */
    private static Component paletteCard(Component root) {
        Div card = new Div();
        root.getUI().orElseThrow().add(card);

        return card;
    }

    private Component attachedDesign() {
        return attachedDesign(design);
    }

    private Component attachedDesign(DesignNode design) {
        Component root = factory.fromDesignTree(design);
        factory.refreshDropZones(root);

        // a grid needs a locked session when it is attached
        VaadinSession session = mock(VaadinSession.class);
        when(session.hasLock()).thenReturn(true);
        UI ui = new UI();
        ui.getInternals().setSession(session);
        ui.add(root);

        return root;
    }

    /**
     * Does what the browser and Flow do for a drop: the dragged component is the active drag source carrying itself
     * as drag data (see DndUtils), and the drop target gets a drop event.
     */
    private void drop(Component dragged, Component target) {
        drop(dragged, dragged, target);
    }

    private void drop(Component dragged, Object dragData, Component target) {
        DragSource.configure(dragged).setDragData(dragData);
        dragged.getUI().orElseThrow().getInternals().setActiveDragSourceComponent(dragged);

        ComponentUtil.fireEvent(target, new DropEvent<>(target, true, "move"));
    }

    private static Component child(Component container, int index) {
        return container.getChildren().toList().get(index);
    }

    private static List<String> childKinds(Component container) {
        return container.getChildren().map(PlaceholderFactoryTest::kindOf).toList();
    }

    private static String kindOf(Component component) {
        if (component.getClassNames().contains(DndBuilder.CSS_DROP_ZONE)) {
            return "zone";
        }

        return component instanceof Placeholder placeholder
                ? placeholder.getRepresentedType().getSimpleName()
                : component.getClass().getSimpleName();
    }

}
