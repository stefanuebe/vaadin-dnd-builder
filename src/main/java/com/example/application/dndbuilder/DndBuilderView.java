package com.example.application.dndbuilder;

import com.example.application.dndbuilder.events.DesignChangedEvent;
import com.example.application.dndbuilder.events.EventBus;
import com.example.application.dndbuilder.events.PreviewModeChanged;
import com.example.application.dndbuilder.events.ResetRequestedEvent;
import com.example.application.dndbuilder.events.SaveRequestedEvent;
import com.example.application.dndbuilder.events.StructureHasChangedEvent;
import com.example.application.dndbuilder.events.ViewLoadedEvent;
import com.example.application.dndbuilder.placeholders.PlaceholderFactory;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Stefan Uebe
 */
@Route(value = "", layout = DndBuilder.class)
public class DndBuilderView extends SplitLayout implements BeforeEnterObserver {

    /** Query parameter naming the loaded view's source file, so a reload brings the same view back. */
    private static final String VIEW_PARAMETER = "view";

    private final EventBus eventBus;
    private final PlaceholderFactory placeholderFactory;
    private final DndUtils dndUtils;
    private final ViewSourceCodec codec = new ViewSourceCodec();
    private final ViewSourceFiles viewSourceFiles = new ViewSourceFiles();
    private final Div canvas;
    private final Pre output;
    /** The root designer component of the loaded view, {@code null} before a view is loaded. */
    private Component placeholder;
    private Path loadedView;
    /** The designer-owned method as it stands in the loaded view's file, to tell unsaved output lines apart. */
    private String savedMethod = "";

    public DndBuilderView(EventBus eventBus, PlaceholderFactory placeholderFactory, DndUtils dndUtils) {
        this.eventBus = eventBus;
        this.placeholderFactory = placeholderFactory;
        this.dndUtils = dndUtils;
        addClassName("dnd-view");

        setOrientation(Orientation.VERTICAL);

        canvas = new Div(new Paragraph("Select a view in the header to design it."));
        canvas.addClassName("dnd-builder-view-container");

        output = new Pre();
        output.addClassName("dnd-output");

        addToPrimary(canvas);

        VerticalLayout outputContainer = new VerticalLayout(new Span("Output (highlighted lines are not saved yet)"), this.output);
        outputContainer.setPadding(false);
        outputContainer.setSpacing(false);
        outputContainer.setFlexGrow(1, output);
        outputContainer.setSizeFull();
        outputContainer.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);

        addToSecondary(outputContainer);

        setSplitterPosition(50);

        eventBus.addListener(StructureHasChangedEvent.class, event -> onStructureChanged());
        eventBus.addListener(PreviewModeChanged.class, event -> saveDraft());
        eventBus.addListener(SaveRequestedEvent.class, event -> saveView());
        eventBus.addListener(ResetRequestedEvent.class, event -> resetView());
    }

    /**
     * Navigates the builder to the given view, which loads it and puts it into the URL.
     */
    public static void showView(Path file) {
        UI.getCurrentOrThrow().navigate(DndBuilderView.class, queryParametersFor(file));
    }

    /**
     * The query parameters of the builder URL that shows the given view. The view is named by its class name, not its
     * path: SideNavItem encodes query parameters twice (Vaadin 25.3.2), so a path's slashes came back as "%2F".
     */
    public static QueryParameters queryParametersFor(Path file) {
        return QueryParameters.of(VIEW_PARAMETER, ViewSourceFiles.classNameOf(file));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<String> requestedView = event.getLocation().getQueryParameters().getSingleParameter(VIEW_PARAMETER);
        if (requestedView.isEmpty()) {
            return;
        }

        // the parameter comes from the URL: only files the builder itself offers may be read
        viewSourceFiles.findViews(placeholderFactory::isContainer).stream()
                .filter(file -> ViewSourceFiles.classNameOf(file).equals(requestedView.get()))
                .findFirst()
                .ifPresentOrElse(this::loadView, () -> showError("Unknown view " + requestedView.get()));
    }

    /**
     * Replaces the canvas with the design of the given view, taken from its unsaved draft if there is one. On failure
     * the canvas stays as it is.
     */
    private void loadView(Path file) {
        DesignNode saved;
        DesignNode design;
        try {
            saved = codec.read(viewSourceFiles.read(file));
            design = viewSourceFiles.readDraft(file).map(codec::read).orElse(saved);
            showDesign(design);
        } catch (RuntimeException e) {
            showError("Cannot load " + file.getFileName() + ": " + e.getMessage());
            return;
        }

        boolean restored = !design.equals(saved);
        loadedView = file;
        savedMethod = codec.renderMethod(saved);
        updateOutput();
        eventBus.fireEvent(new ViewLoadedEvent(file));
        saveDraft();

        if (restored) {
            Notification.show("Restored unsaved changes of " + file.getFileName(), 5000, Notification.Position.BOTTOM_START);
        }
    }

    /**
     * Replaces the canvas with fresh designer components for the given design, with their drop zones.
     */
    private void showDesign(DesignNode design) {
        Component root = placeholderFactory.fromDesignTree(design);
        placeholderFactory.refreshDropZones(root);
        canvas.removeAll();
        canvas.add(root);
        placeholder = root;
    }

    private void onStructureChanged() {
        // shortcut: rebuilds the whole canvas, as patching moves and drop zones in place left the browser's order wrong
        // (server and reload were right, cause in Flow not pinned down); find that cause if large designs get slow
        showDesign(currentDesign());
        updateOutput();
        saveDraft();
    }

    /**
     * Keeps the unsaved design of the loaded view in its draft while designing, so a crashed or closed browser loses
     * nothing.
     */
    private void saveDraft() {
        if (dndUtils.isPreviewMode() || loadedView == null) {
            return;
        }

        try {
            viewSourceFiles.writeDraft(loadedView, codec.write(viewSourceFiles.read(loadedView), currentDesign()));
        } catch (RuntimeException e) {
            showError("Cannot keep a draft of " + loadedView.getFileName() + ": " + e.getMessage());
        }
    }

    private void saveView() {
        try {
            String source = viewSourceFiles.read(loadedView);
            viewSourceFiles.write(loadedView, codec.write(source, currentDesign()));
            viewSourceFiles.deleteDraft(loadedView);
        } catch (RuntimeException e) {
            showError("Cannot save " + loadedView.getFileName() + ": " + e.getMessage());
            return;
        }

        savedMethod = codec.renderMethod(currentDesign());
        updateOutput();

        Notification.show("Saved " + loadedView.getFileName(), 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.SUCCESS);
    }

    private void resetView() {
        try {
            viewSourceFiles.deleteDraft(loadedView);
        } catch (RuntimeException e) {
            showError("Cannot discard the draft of " + loadedView.getFileName() + ": " + e.getMessage());
            return;
        }

        loadView(loadedView);
    }

    private DesignNode currentDesign() {
        return placeholderFactory.toDesignTree(placeholder);
    }

    private void showError(String message) {
        Notification.show(message, 8000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.ERROR);
    }

    /**
     * Shows the method the current design generates; lines the saved method does not have are highlighted. Tells the
     * rest of the builder (the outline) about the design, too.
     */
    private void updateOutput() {
        DesignNode design = currentDesign();
        eventBus.fireEvent(new DesignChangedEvent(design));

        // shortcut: compares lines regardless of order, so a moved line counts as saved; a real diff if that misleads
        List<String> savedLines = new ArrayList<>(savedMethod.lines().toList()); // each saved line matches once
        output.removeAll();
        for (String line : codec.renderMethod(design).lines().toList()) {
            Span span = new Span(line + "\n");
            if (!savedLines.remove(line)) {
                span.addClassName("unsaved-line");
            }
            output.add(span);
        }
    }

}
