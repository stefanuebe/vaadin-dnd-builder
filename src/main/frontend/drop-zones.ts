/*
 * A drop anywhere in a designer container goes to the container's drop zone nearest to the pointer, which is marked
 * while dragging over the container. Without this, a drop on a component or the space around the zones would go to
 * the container itself, which appends at the end.
 */
// class names set by the server: DndBuilder.CSS_DROP_ZONE and CSS_HAS_COMPONENTS
const ZONE = "drop-zone";
const CONTAINER = "has-components";
const NEAREST = "drop-zone-nearest";
// Vaadin's drop target hover class; a redirected drop never reaches the listener that would remove it
const DRAG_OVER = "v-drag-over-target";

function isColumnDrag(): boolean {
    return document.body.getAttribute("drag-type") === "Column"; // grid columns go onto grids, not into zones
}

/** The innermost container under the pointer and its zone nearest to it, unless the pointer is on a zone. */
function nearestZone(event: DragEvent): Element | undefined {
    const target = event.target as Element;
    if (isColumnDrag() || !target.closest || target.closest("." + ZONE)) {
        return undefined;
    }

    const container = target.closest("." + CONTAINER);
    if (!container) {
        return undefined;
    }

    let nearest: Element | undefined;
    let nearestDistance = Infinity;
    for (const zone of Array.from(container.children)) {
        if (!zone.classList.contains(ZONE) || zone.getClientRects().length === 0) {
            continue; // not a zone, or a zone not shown (yet)
        }

        const rect = zone.getBoundingClientRect();
        const dx = Math.max(rect.left - event.clientX, 0, event.clientX - rect.right);
        const dy = Math.max(rect.top - event.clientY, 0, event.clientY - rect.bottom);
        const distance = Math.hypot(dx, dy);
        if (distance < nearestDistance) {
            nearest = zone;
            nearestDistance = distance;
        }
    }

    return nearest;
}

function clearMarks() {
    document.querySelectorAll("." + NEAREST).forEach(zone => zone.classList.remove(NEAREST));
}

function clearDragOver() {
    document.querySelectorAll("." + DRAG_OVER).forEach(target => target.classList.remove(DRAG_OVER));
}

function onDragOver(event: DragEvent) {
    const zone = nearestZone(event);
    if (zone?.classList.contains(NEAREST)) {
        return; // already marked
    }

    clearMarks();
    zone?.classList.add(NEAREST);
}

function onDrop(event: DragEvent) {
    const zone = nearestZone(event);
    clearMarks();
    if (!zone) {
        return;
    }

    // the drop target's server listener reads the dragged component from the server, not from the event
    event.preventDefault();
    event.stopPropagation();
    clearDragOver();
    zone.dispatchEvent(new DragEvent("drop", {
        bubbles: true,
        cancelable: true,
        clientX: event.clientX,
        clientY: event.clientY,
        dataTransfer: event.dataTransfer
    }));
}

let w: any = window;
if (!w._dndbuilderDropZones) { // a re-evaluated module must not redirect every drop twice
    w._dndbuilderDropZones = true;

    // capturing on the document runs before Vaadin's drop target handlers, which stop the event at the container
    document.addEventListener("dragover", onDragOver, true);
    document.addEventListener("drop", onDrop, true);
    document.addEventListener("dragend", clearMarks, true);
    document.addEventListener("dragleave", event => {
        if (!event.relatedTarget) {
            clearMarks(); // left the window
        }
    }, true);
}
