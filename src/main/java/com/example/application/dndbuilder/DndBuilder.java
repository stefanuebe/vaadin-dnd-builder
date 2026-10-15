package com.example.application.dndbuilder;

import com.example.application.dndbuilder.events.DesignChangedEvent;
import com.example.application.dndbuilder.events.DragStartedEvent;
import com.example.application.dndbuilder.events.EventBus;
import com.example.application.dndbuilder.events.PreviewModeChanged;
import com.example.application.dndbuilder.events.ResetRequestedEvent;
import com.example.application.dndbuilder.events.SaveRequestedEvent;
import com.example.application.dndbuilder.events.StructureHasChangedEvent;
import com.example.application.dndbuilder.events.ViewLoadedEvent;
import com.example.application.dndbuilder.placeholders.PlaceholderFactory;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Stefan Uebe
 */
@CssImport("./styles.css")
@NpmPackage(value = "tippy.js", version = "6.3.7")
@CssImport("tippy.js/themes/translucent.css")
@CssImport("tippy.js/dist/tippy.css") // seems necessary for some browsers / systems
@JsModule("./init-tippy.ts")
@JsModule("./drop-zones.ts")
public class DndBuilder extends AppLayout {

    public static final String CSS_DRAG_ACTIVE = "drag-active";
    public static String CSS_DROP_TARGET = "drop-target";
    public static String CSS_DROP_TARGET_PART = "drop-target-part";
    public static String CSS_DRAG_OVER = "drag-over";
    public static String CSS_HAS_COMPONENTS = "has-components";
    public static final String CSS_DROP_ZONE = "drop-zone";
    /** Body attribute naming the palette type being dragged, so a drop target can light up only for what it takes. */
    public static final String CSS_DRAG_TYPE = "drag-type";

    private final EventBus eventBus;
    private final DndUtils dndUtils;
    private final PlaceholderFactory placeholderFactory;
    private final List<Path> views;

    /** Whether the design mode was entered; from then on Save stays, also while looking at the preview. */
    private boolean designStarted;
    private Path loadedView;

    public DndBuilder(EventBus eventBus, DndUtils dndUtils, PlaceholderFactory placeholderFactory) {
        this.eventBus = eventBus;
        this.dndUtils = dndUtils;
        this.placeholderFactory = placeholderFactory;
        views = new ViewSourceFiles().findViews(placeholderFactory::isContainer);

        setPrimarySection(Section.DRAWER);
        addToNavbar(true, createHeaderContent());
        addToDrawer(createDrawerContent());
    }

    private Component createDrawerContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);

        layout.add(new H3("Vaadin Flow UI Builder"));

        // previewing is about moving between views, designing about the loaded view's structure and what to add
        SideNav navigation = createNavigation();
        VerticalLayout designTools = createDesignTools();
        layout.add(navigation, designTools);

        navigation.setVisible(dndUtils.isPreviewMode());
        designTools.setVisible(!dndUtils.isPreviewMode());
        eventBus.addListener(PreviewModeChanged.class, event -> {
            navigation.setVisible(event.isActive());
            designTools.setVisible(!event.isActive());
        });

        WebBrowser browser = VaadinSession.getCurrent().getBrowser();
        if (browser.isAndroid() || browser.isIPhone() || browser.isWindowsPhone()) {
            eventBus.addListener(DragStartedEvent.class, e -> setDrawerOpened(false));
        }

        return layout;
    }

    private VerticalLayout createDesignTools() {
        VerticalLayout palette = new VerticalLayout();
        palette.setPadding(false);
        palette.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        for (Palette.Group group : Palette.GROUPS) {
            createComponentList(palette, group.name(), group.types());
        }

        Details outlineDetails = new Details("Outline", createOutline());
        outlineDetails.setOpened(true);
        Details paletteDetails = new Details("Palette", palette);
        paletteDetails.setOpened(true);

        VerticalLayout designTools = new VerticalLayout(outlineDetails, paletteDetails);
        designTools.setPadding(false);
        designTools.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);

        return designTools;
    }

    /**
     * The tree of the design on the canvas, one indented line per component, following every change.
     */
    private Component createOutline() {
        Div outline = new Div(new Span("No view loaded"));
        eventBus.addListener(DesignChangedEvent.class, event -> {
            outline.removeAll();
            addOutlineLines(outline, event.design(), 0);
        });

        return outline;
    }

    private void addOutlineLines(Div outline, DesignNode node, int depth) {
        String name = dndUtils.getReadableName(node.type());
        Div line = new Div(node.text() == null ? name : name + " \"" + node.text() + "\"");
        line.getStyle().setPaddingLeft(depth + "em");
        outline.add(line);
        node.children().forEach(child -> addOutlineLines(outline, child, depth + 1));
    }

    private SideNav createNavigation() {
        SideNav navigation = new SideNav("Views");
        for (Path view : views) {
            SideNavItem item = new SideNavItem(viewName(view), DndBuilderView.class);
            item.setQueryParameters(DndBuilderView.queryParametersFor(view));
            navigation.addItem(item);
        }

        return navigation;
    }

    private static String viewName(Path view) {
        return view.getFileName().toString().replace(".java", "");
    }

    private void createComponentList(VerticalLayout layout, String name, List<Class<? extends Component>> componentTypes) {
        VerticalLayout list = new VerticalLayout();
        list.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        list.setSpacing(false);
        list.setPadding(false);

        for (Class<? extends Component> componentType : componentTypes) {
            list.add(createComponentCard(dndUtils.getReadableName(componentType), componentType));
        }

        layout.add(new com.vaadin.flow.component.html.Section(new Span("=== " + name.toUpperCase() + " ==="), list));
    }

    private Component createComponentCard(String name, Class<? extends Component> componentType) {
        Div div = new Div(new Span(name));
        div.addClassName("component-card");
        dndUtils.makeSimpleDraggable(div, componentType);

        return div;
    }

    private Component createHeaderContent() {
        DrawerToggle toggle = initDrawerToggle();
        Select<Path> viewSelect = initViewSelect();
        Button save = initSaveButton();
        Button reset = initResetButton();
        Div trash = initTrash();
        Button modeToggle = initModeToggle(save, reset, trash);

        Header header = new Header(toggle, viewSelect, modeToggle, save, reset, trash);

        header.addClassNames("bg-base", "border-b", "border-contrast-10", "box-border", "flex", "h-xl", "items-center", "w-full");
        return header;
    }

    private DrawerToggle initDrawerToggle() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.addClassName("text-secondary");
        toggle.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        return toggle;
    }

    /**
     * The builder opens in preview mode; once a view is loaded, this button switches to the design mode, where the
     * trash and Save appear, and back.
     */
    private Button initModeToggle(Button save, Button reset, Div trash) {
        Button modeToggle = new Button();
        Runnable showMode = () -> {
            boolean previewMode = dndUtils.isPreviewMode();
            if (!previewMode) {
                modeToggle.setText("Preview");
            } else {
                modeToggle.setText(designStarted ? "Continue editing" : "Design");
            }
            modeToggle.setIcon(previewMode ? VaadinIcon.EDIT.create() : VaadinIcon.EYE.create());
            save.setVisible(designStarted);
            reset.setVisible(designStarted);
            trash.setVisible(!previewMode);
        };
        modeToggle.addClickListener(event -> {
            designStarted = true;
            activatePreviewMode(!dndUtils.isPreviewMode());
            showMode.run();
        });

        modeToggle.setVisible(false);
        eventBus.addListener(ViewLoadedEvent.class, event -> {
            if (!event.file().equals(loadedView)) { // a reset reloads the same view and keeps the toolbar
                designStarted = !dndUtils.isPreviewMode(); // a view loaded while looking at the preview is not designed yet
            }
            loadedView = event.file();
            modeToggle.setVisible(true);
            showMode.run();
        });

        activatePreviewMode(true);
        showMode.run();

        return modeToggle;
    }

    private Select<Path> initViewSelect() {
        Select<Path> viewSelect = new Select<>();
        viewSelect.setPlaceholder("Load a view");
        viewSelect.setItemLabelGenerator(DndBuilder::viewName);
        viewSelect.setItems(views);
        viewSelect.addValueChangeListener(event -> {
            if (event.isFromClient() && event.getValue() != null) {
                DndBuilderView.showView(event.getValue());
            }
        });
        eventBus.addListener(ViewLoadedEvent.class, event -> viewSelect.setValue(event.file()));

        return viewSelect;
    }

    private Button initSaveButton() {
        return new Button("Save", VaadinIcon.DISC.create(), event -> eventBus.fireEvent(new SaveRequestedEvent()));
    }

    private Button initResetButton() {
        return new Button("Reset", VaadinIcon.ROTATE_LEFT.create(), event -> {
            ConfirmDialog confirm = new ConfirmDialog("Discard unsaved changes?",
                    "The designer shows the view as it is saved in its source file again.", "Discard",
                    confirmed -> eventBus.fireEvent(new ResetRequestedEvent()));
            confirm.setCancelable(true);
            confirm.setConfirmButtonTheme("error primary");
            confirm.open();
        });
    }

    private Div initTrash() {
        Div trash = new Div(VaadinIcon.TRASH.create());
        DropTarget.create(trash).addDropListener(event -> {
            // the trashed source never sees its dragend
            trash.getUI().ifPresent(dndUtils::endDrag);
            event.getDragData().ifPresent(o -> {
                if (dndUtils.isPlaceholder(o)) {
                    ((Component) o).getParent().ifPresent(parent -> {
                        if (!dndUtils.isPlaceholder(parent)) { // the canvas root: clear it instead of leaving no canvas
                            ((Component) o).getElement().removeAllChildren();
                        } else {
                            parent.getElement().removeChild(((Component) o).getElement());
                        }

                        eventBus.fireEvent(new StructureHasChangedEvent(this));
                    });
                }
            });
        });
        trash.addClassName("trash");
        trash.addClassName(CSS_DROP_TARGET);

        dndUtils.addClassNameOnDragOver(trash);
        return trash;
    }

    private void activatePreviewMode(boolean active) {
        UI.getCurrent().getElement().setAttribute("preview-mode", active);
        eventBus.fireEvent(new PreviewModeChanged(active));
    }


}
