package com.example.application.dndbuilder;

import com.vaadin.flow.component.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The Java sources of this project's views. Works only while the app runs from the project directory (dev mode).
 */
public class ViewSourceFiles {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    /**
     * Per user, not in the shared system temp directory: drafts end up in source code, so nobody else may plant one.
     */
    private static final Path DRAFT_DIRECTORY = Path.of(System.getProperty("user.home"), ".cache", "dnd-builder-drafts");

    private final ViewSourceCodec codec = new ViewSourceCodec();

    /**
     * The fully qualified class name of the given view source, e.g. {@code com.example.application.views.SampleView}.
     */
    public static String classNameOf(Path file) {
        String name = SOURCE_ROOT.relativize(file).toString().replace(file.getFileSystem().getSeparator(), ".");

        return name.substring(0, name.length() - ".java".length());
    }

    /**
     * Finds the {@code @Route} views whose super type the designer can show as its root.
     */
    public List<Path> findViews(Predicate<Class<? extends Component>> isDesignableRoot) {
        if (!Files.isDirectory(SOURCE_ROOT)) {
            return List.of();
        }

        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            return files
                    .filter(file -> file.toString().endsWith(".java"))
                    .filter(file -> isDesignableView(file, isDesignableRoot))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a sibling file first and moves it over the source, so a failing write never leaves the source half
     * written.
     */
    public void write(Path file, String source) {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, source);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            deleteQuietly(temporary);
            throw new UncheckedIOException(e);
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // the write already failed; that is the error to report
        }
    }

    /**
     * Returns the unsaved draft of the given view's source, if there is one.
     */
    public Optional<String> readDraft(Path file) {
        Path draft = draftOf(file);

        return Files.exists(draft) ? Optional.of(read(draft)) : Optional.empty();
    }

    public void writeDraft(Path file, String source) {
        try {
            Files.createDirectories(DRAFT_DIRECTORY);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        write(draftOf(file), source);
    }

    public void deleteDraft(Path file) {
        try {
            Files.deleteIfExists(draftOf(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * One draft file per view, named after the view's full path so views with the same name don't share one.
     */
    private Path draftOf(Path file) {
        return DRAFT_DIRECTORY.resolve(file.toAbsolutePath().toString().replaceAll("[^A-Za-z0-9.]", "_"));
    }

    private boolean isDesignableView(Path file, Predicate<Class<? extends Component>> isDesignableRoot) {
        String source = read(file);
        if (!source.contains("@Route")) { // cheap pre-check, parsing every source is slow
            return false;
        }

        try {
            return codec.findRouteViewType(source).filter(isDesignableRoot).isPresent();
        } catch (IllegalArgumentException e) {
            return false; // unparsable sources are not offered
        }
    }

}
