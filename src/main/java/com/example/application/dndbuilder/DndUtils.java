package com.example.application.dndbuilder;

import com.example.application.dndbuilder.events.DragStartedEvent;
import com.example.application.dndbuilder.events.EventBus;
import com.example.application.dndbuilder.events.PreviewModeChanged;
import com.example.application.dndbuilder.events.StructureHasChangedEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Stefan Uebe
 */
@SpringComponent
@UIScope
public class DndUtils {

    /** Element property marking designer components; not "placeholder", which fields use for their placeholder text. */
    public static final String PLACEHOLDER_MARKER = "dndBuilderPlaceholder";

    private final EventBus eventBus;
    private boolean previewMode = true;

    public DndUtils(EventBus eventBus) {
        this.eventBus = eventBus;
        eventBus.addListener(PreviewModeChanged.class, event -> previewMode = event.isActive());
    }

    public void addClassNameOnDragOver(Component component) {
        addClassNameOnDragOver(component, DndBuilder.CSS_DRAG_OVER);

    }

    public void addClassNameOnDragOver(Component component, String className) {
        component.getElement().executeJs("" +
                "this.addEventListener('dragenter', e => e.target.classList.add($0));" +
                "this.addEventListener('dragleave', e => e.target.classList.remove($0));" +
                "this.addEventListener('drop', e => e.target.classList.remove($0));" +
                "", className);
    }

    /**
     * Whether the builder only shows the design; dragging, the trash and saving belong to the design mode.
     */
    public boolean isPreviewMode() {
        return previewMode;
    }

    public boolean isPlaceholder(Object object) {
        return object instanceof Component && ((Component) object).getElement().getProperty(PLACEHOLDER_MARKER, false);
    }

    public void makeSimpleDraggable(Component component) {
        makeSimpleDraggable(component, component);
    }

    public void makeSimpleDraggable(Component component, Object dragData) {
        DragSource<Component> dragSource = DragSource.create(component);
        dragSource.setDraggable(!previewMode);
        dragSource.addDragStartListener(event -> {
            event.setDragData(dragData);
            UI.getCurrent().getElement().setAttribute(DndBuilder.CSS_DRAG_ACTIVE, true);
            if (dragData instanceof Class<?> type) {
                UI.getCurrent().getElement().setAttribute(DndBuilder.CSS_DRAG_TYPE, type.getSimpleName());
            }
            eventBus.fireEvent(new DragStartedEvent());
        });

        dragSource.addDragEndListener(event -> {
            event.clearDragData();
            endDrag(UI.getCurrent());
        });

        // follow the mode while attached (callers pass components not attached yet); moving one detaches and
        // re-attaches it, so the subscription is renewed on every attach
        component.addAttachListener(attach -> {
            dragSource.setDraggable(!previewMode);
            Registration registration = eventBus.addListener(PreviewModeChanged.class,
                    event -> dragSource.setDraggable(!event.isActive()));
            component.addDetachListener(detach -> {
                registration.remove();
                detach.unregisterListener();
            });
        });
    }

    public void makeSimpleDroppable(Component component, Function<DropEvent<Component>, Component> getOrCreatePlaceholderCallback) {
        makeSimpleDroppable(component, component, getOrCreatePlaceholderCallback);
    }

    public void makeSimpleDroppable(Component target, Component owner, Function<DropEvent<Component>, Component> getOrCreatePlaceholderCallback) {
        DropTarget.create(target).addDropListener(event -> {
            // the moved source may miss its dragend after being re-attached, which would leave the drop zones showing
            target.getUI().ifPresent(this::endDrag);

            Component placeholder = getOrCreatePlaceholderCallback.apply(event);
            if (placeholder == null) {
                return; // nothing that can go here, like a grid column outside a grid
            }

            boolean droppedOnOwnContainer = owner == target && placeholder.getParent().filter(p -> p == owner).isPresent();
            if (droppedOnOwnContainer || contains(placeholder, owner)) {
                return; // a container cannot go into itself; moving within one goes through its drop zones
            }

            placeholder.getElement().removeFromParent();
            // a drop zone stands where the component goes; the container itself appends
            int index = owner == target
                    ? owner.getElement().getChildCount()
                    : owner.getElement().indexOfChild(target.getElement());
            owner.getElement().insertChild(index, placeholder.getElement());

            eventBus.fireEvent(new StructureHasChangedEvent(event.getSource()));
        });
    }

    /**
     * Removes the body attributes a drag sets.
     */
    public void endDrag(UI ui) {
        ui.getElement().removeAttribute(DndBuilder.CSS_DRAG_ACTIVE);
        ui.getElement().removeAttribute(DndBuilder.CSS_DRAG_TYPE);
    }

    private boolean contains(Component ancestor, Component component) {
        return Stream.iterate(component.getElement(), Objects::nonNull, Element::getParent)
                .anyMatch(element -> element.equals(ancestor.getElement()));
    }

    public String getReadableName(Class<?> type) {
        // a nested type like Grid.Column reads "Grid Column"
        String name = type.isMemberClass() ? type.getEnclosingClass().getSimpleName() + type.getSimpleName() : type.getSimpleName();
        String[] strings = Stream.of(StringUtils.splitByCharacterTypeCamelCase(name))
                .filter(s -> !"Placeholder".equals(s))
                .toArray(String[]::new);

        return String.join(" ", strings);
    }

}
