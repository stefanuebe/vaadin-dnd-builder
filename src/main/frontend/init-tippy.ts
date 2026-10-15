import tippy, {Instance} from "tippy.js";

// time to move the pointer from a component onto its tooltip, across whatever lies between them
const PENDING_DELAY_MS = 300;

let w: any = window;
if (!w._dndbuilder) {
    w._dndbuilder = {
        tippyInstance: undefined as Instance | undefined,
        pendingTimeout: undefined as number | undefined,

        /**
         * Shows the component name on hover. An editable component's tooltip gets an Edit button in design mode,
         * which sends a "dnd-edit" event to the component; the tooltip stays open while the pointer is on it.
         */
        applyTippy(el: Element, name: string, editable: boolean) {
            el.addEventListener("mouseover", e => {
                e.stopPropagation(); // only the innermost component shows its tooltip
                if (this.tippyInstance?.reference === el) {
                    this.cancelPending();
                } else if (this.tippyInstance?.reference.isConnected) {
                    // the pointer may only be crossing this component on its way to the open tooltip; a tooltip whose
                    // component was replaced (the canvas is rebuilt after every change) gives way at once
                    this.schedule(() => this.showTippy(el, name, editable));
                } else {
                    this.showTippy(el, name, editable);
                }
            });

            el.addEventListener("mouseout", e => {
                e.stopPropagation();
                this.schedule(() => this.destroyTippy());
            });
        },

        showTippy(el: Element, name: string, editable: boolean) {
            this.destroyTippy();
            if (!el.isConnected) {
                return; // moved or removed while the switch was pending
            }

            this.tippyInstance = tippy(el, {
                content: this.createContent(el, name, editable),
                placement: 'top-start',
                offset: [0, 2],
                theme: "translucent",
                trigger: "manual",
                interactive: true,
                appendTo: document.body,
                onCreate: instance => {
                    instance.popper.addEventListener("mouseenter", () => this.cancelPending());
                    instance.popper.addEventListener("mouseleave", () => this.schedule(() => this.destroyTippy()));
                },
                // tippy hides on its own on a click outside the tooltip; forget it then, so hovering shows it again
                onHidden: instance => {
                    if (this.tippyInstance === instance) {
                        this.destroyTippy();
                    }
                }
            });
            this.tippyInstance.show();
        },

        createContent(el: Element, name: string, editable: boolean): HTMLElement {
            const content = document.createElement("span");
            content.textContent = name;

            // the UI element is the body; it carries preview-mode outside the design mode
            if (editable && !document.body.hasAttribute("preview-mode")) {
                const edit = document.createElement("button");
                edit.textContent = "Edit";
                edit.style.marginLeft = "0.5em";
                edit.addEventListener("click", () => {
                    this.destroyTippy();
                    el.dispatchEvent(new CustomEvent("dnd-edit"));
                });
                content.append(edit);
            }

            return content;
        },

        /** Runs the action after the delay, replacing any pending one (hiding, or switching to another component). */
        schedule(action: () => void) {
            this.cancelPending();
            this.pendingTimeout = window.setTimeout(action, PENDING_DELAY_MS);
        },

        cancelPending() {
            window.clearTimeout(this.pendingTimeout);
        },

        destroyTippy() {
            this.cancelPending();
            this.tippyInstance?.destroy();
            this.tippyInstance = undefined;
        }
    };
}
