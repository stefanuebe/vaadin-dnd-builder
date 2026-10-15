package com.example.application.dndbuilder;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewSourceFilesTest {

    private final ViewSourceFiles files = new ViewSourceFiles();
    private final ViewSourceCodec codec = new ViewSourceCodec();

    /** The layouts a view may extend to be designed. */
    private static final Set<Class<?>> DESIGNABLE_ROOTS = Set.of(VerticalLayout.class, HorizontalLayout.class,
            FormLayout.class);

    /**
     * The views shipped with the project load in the builder and are in the form a save writes, so loading and saving
     * one without changes leaves its file as it is. Runs from the project directory, like the builder.
     */
    @Test
    void projectViews_loadAndSaveUnchanged() {
        List<Path> views = files.findViews(DESIGNABLE_ROOTS::contains);

        assertTrue(views.stream().map(view -> view.getFileName().toString()).toList()
                .containsAll(List.of("SampleView.java", "ContactFormView.java", "CustomerListView.java")), views.toString());

        for (Path view : views) {
            String source = files.read(view);
            assertEquals(source, codec.write(source, codec.read(source)), view.toString());
        }
    }

    /** The builder URL names a view by its class name, which needs no encoding. */
    @Test
    void classNameOf_isTheViewsFullyQualifiedName() {
        Path file = Path.of("src", "main", "java", "com", "example", "application", "views", "SampleView.java");

        assertEquals("com.example.application.views.SampleView", ViewSourceFiles.classNameOf(file));
    }

}
