"use strict";

// The landing-page product demo: a cursor adds an expense from a scanned receipt and the
// dashboard updates. It plays an intro once, then loops. motion.js calls start() once the
// hero has animated in.
window.ExpenseHeroPreview = (() => {
    const screen = document.querySelector("[data-preview]");
    if (!screen || !window.gsap) {
        return { start() {} };
    }
    const q = selector => screen.querySelector(selector);
    const qa = selector => Array.from(screen.querySelectorAll(selector));
    const figure = screen.closest(".hero-product");

    const workspace = q("[data-preview-workspace]");
    const pointer = q("[data-preview-pointer]");
    const ring = pointer.querySelector("i");
    const addButton = q("[data-preview-add]");
    const saveButton = q("[data-preview-save]");
    const editor = q("[data-preview-editor]");
    const backdrop = q("[data-preview-backdrop]");
    const receipt = q("[data-preview-receipt]");
    const scanline = q("[data-preview-scanline]");
    const scanBadge = q("[data-preview-scan]");
    const fields = qa("[data-type]");
    const toast = q("[data-preview-toast]");
    const rows = qa(".preview-row");
    const newRow = q("[data-preview-new]");
    const counters = qa("[data-count]");
    const week = q("[data-preview-week]");
    const shopping = q("[data-preview-shopping]");
    const budget = q("[data-preview-budget]");
    const used = q("[data-preview-used]");
    const chipScan = figure.querySelector("[data-chip-scan]");
    const chipBudget = figure.querySelector("[data-chip-budget]");
    const chipBar = figure.querySelector("[data-chip-bar]");
    const chipUsed = figure.querySelector("[data-chip-used]");
    const chipLeft = figure.querySelector("[data-chip-left]");

    const rupees = new Intl.NumberFormat("en-IN");
    const write = (element, value) => {
        element.textContent = (element.hasAttribute("data-money") ? "₹" : "") + rupees.format(Math.round(value));
    };

    // immediateRender is off so that building the loop does not write its start values
    // into the counters before the loop reaches them.
    function count(element, from, to, vars = {}) {
        const state = { value: from };
        return gsap.fromTo(state, { value: from }, {
            value: to, duration: 1, ease: "power2.out", immediateRender: false, ...vars,
            onUpdate: () => write(element, state.value)
        });
    }

    // Where the cursor should point, as a percentage of the workspace. Percentages keep the
    // path right when the preview is resized, because everything inside scales with it.
    function spot(element, fx, fy) {
        const box = workspace.getBoundingClientRect();
        const rect = element.getBoundingClientRect();
        return {
            left: ((rect.left - box.left + rect.width * fx) / box.width * 100) + "%",
            top: ((rect.top - box.top + rect.height * fy) / box.height * 100) + "%"
        };
    }

    const moveTo = (element, fx, fy, vars = {}) => gsap.to(pointer, {
        left: () => spot(element, fx, fy).left,
        top: () => spot(element, fx, fy).top,
        duration: .9, ease: "power2.inOut", ...vars
    });

    const click = element => gsap.timeline()
        .to(pointer, { scale: .8, duration: .12, ease: "power1.in" })
        .to(element, { scale: .94, duration: .12, ease: "power1.in" }, "<")
        .fromTo(ring, { scale: .2, autoAlpha: .9 }, { scale: 1.5, autoAlpha: 0, duration: .55, ease: "power2.out" }, "<.06")
        .to([pointer, element], { scale: 1, duration: .3, ease: "back.out(3)" }, "<");

    function type(field) {
        const text = field.dataset.type;
        const state = { length: 0 };
        return gsap.fromTo(state, { length: 0 }, {
            length: text.length, duration: Math.max(.25, text.length * .045), ease: "none",
            onUpdate: () => { field.textContent = text.slice(0, Math.round(state.length)); }
        });
    }

    function setBudgetLabels(after) {
        used.textContent = after ? "73% of ₹12,000" : "70% of ₹12,000";
        chipUsed.textContent = after ? "73% used" : "70% used";
        chipLeft.textContent = after ? "₹3,280 left this month" : "₹3,580 left this month";
    }

    // Starting state. Rows sit one row up so "Market Studio" waits, hidden, above the list.
    gsap.set(rows, { yPercent: -100 });
    gsap.set(newRow, { autoAlpha: 0 });
    gsap.set(pointer, { left: "72%", top: "72%" });
    counters.forEach(counter => write(counter, 0));

    const intro = gsap.timeline({ paused: true, defaults: { ease: "power3.out" } })
        .from(qa(".preview-sidebar li"), { x: -10, autoAlpha: 0, stagger: .07, duration: .45 })
        .from(qa(".preview-stats > div"), { y: 14, autoAlpha: 0, stagger: .08, duration: .55 }, "<.1")
        .from(qa(".preview-bars i"), { scaleY: 0, stagger: .09, duration: .8, ease: "back.out(1.5)" }, "<.25")
        .from(qa(".preview-categories progress"), { attr: { value: 0 }, stagger: .1, duration: 1, ease: "power2.out" }, "<")
        .from(rows.slice(1), { x: -14, autoAlpha: 0, stagger: .08, duration: .5 }, "<.15")
        .from(budget, { attr: { value: 0 }, duration: 1.2, ease: "power2.out" }, "<")
        .fromTo(chipBudget, { autoAlpha: 0, y: 18, scale: .9 }, { autoAlpha: 1, y: 0, scale: 1, duration: .7, ease: "back.out(1.7)" }, "<.2")
        .from(chipBar, { scaleX: 0, duration: 1, ease: "power2.out" }, "<.2");
    counters.forEach(counter => intro.add(count(counter, 0, Number(counter.dataset.count), { duration: 1.3 }), .2));

    function buildLoop() {
        const loop = gsap.timeline({ repeat: -1, repeatDelay: .4, defaults: { ease: "power2.inOut" } });

        // 1. The cursor glides in and clicks "Add expense".
        loop.to(pointer, { autoAlpha: 1, duration: .3 })
            .add(moveTo(addButton, .5, .6, { duration: 1.1 }))
            .add(click(addButton));

        // 2. The dialog opens and a receipt drops in.
        loop.to(backdrop, { autoAlpha: .3, duration: .35 }, "+=.05")
            .fromTo(editor, { autoAlpha: 0, y: 14, scale: .97 }, { autoAlpha: 1, y: 0, scale: 1, duration: .5, ease: "power3.out" }, "<")
            .fromTo(receipt, { autoAlpha: 0, y: -20, rotation: -5 }, { autoAlpha: 1, y: 0, rotation: 0, duration: .65, ease: "back.out(1.6)" }, "-=.15")
            .add(moveTo(receipt, .55, .5, { duration: .6 }), "-=.15");

        // 3. A scan sweeps the receipt, then the details fill themselves in.
        loop.fromTo(scanline, { autoAlpha: 0, yPercent: -100 }, { autoAlpha: 1, yPercent: 460, duration: 1.1, ease: "sine.inOut" })
            .to(scanline, { autoAlpha: 0, duration: .2 })
            .fromTo(scanBadge, { autoAlpha: 0, scale: .6 }, { autoAlpha: 1, scale: 1, duration: .45, ease: "back.out(2.5)" }, "<")
            .fromTo(chipScan, { autoAlpha: 0, y: 16, scale: .9 }, { autoAlpha: 1, y: 0, scale: 1, duration: .55, ease: "back.out(1.8)" }, "<");
        fields.forEach((field, index) => loop.add(type(field), index ? ">-.02" : ">"));

        // 4. Save.
        loop.add(moveTo(saveButton, .5, .6, { duration: .8 }), "-=.35")
            .add(click(saveButton))
            .to(editor, { autoAlpha: 0, y: 10, scale: .98, duration: .35, ease: "power2.in" }, "+=.1")
            .to(backdrop, { autoAlpha: 0, duration: .35 }, "<")
            .to(chipScan, { autoAlpha: 0, y: 10, duration: .4 }, "<")
            .to(pointer, { left: "84%", top: "88%", duration: .9 }, "<");

        // 5. The dashboard updates: new row, totals, chart, and budget.
        loop.addLabel("saved", "-=.15")
            .fromTo(toast, { autoAlpha: 0, y: -12 }, { autoAlpha: 1, y: 0, duration: .45, ease: "back.out(2)" }, "saved")
            .to(rows, { yPercent: 0, duration: .65, ease: "power3.out", stagger: .03 }, "saved")
            .fromTo(newRow, { autoAlpha: 0, backgroundColor: "#c9e8bb" }, { autoAlpha: 1, backgroundColor: "#e7f4e2", duration: .9 }, "saved")
            .to(week, { scaleY: 1.14, duration: .7, ease: "back.out(2)" }, "saved+=.1")
            .to(shopping, { attr: { value: 2480 }, duration: .8 }, "saved+=.1")
            .to(budget, { attr: { value: 8720 }, duration: .8 }, "saved+=.1")
            .to(chipBar, { scaleX: .73, duration: .8 }, "saved+=.1")
            .call(setBudgetLabels, [true], "saved+=.3")
            .fromTo(chipBudget, { scale: 1 }, { scale: 1.06, duration: .2, yoyo: true, repeat: 1, ease: "power1.inOut" }, "saved+=.3")
            .to(pointer, { autoAlpha: 0, duration: .4 }, "saved+=.4")
            .to(toast, { autoAlpha: 0, y: -8, duration: .35 }, "saved+=2.2");
        counters.forEach(counter => loop.add(
            count(counter, Number(counter.dataset.count), Number(counter.dataset.next), { duration: .9 }), "saved+=.1"));

        // 6. Hold, then ease back to the starting state so the next loop is seamless.
        loop.addLabel("reset", "saved+=3.6")
            .to(rows, { yPercent: -100, duration: .55 }, "reset")
            .to(newRow, { autoAlpha: 0, duration: .4 }, "reset")
            .to(week, { scaleY: 1, duration: .5 }, "reset")
            .to(shopping, { attr: { value: 2180 }, duration: .5 }, "reset")
            .to(budget, { attr: { value: 8420 }, duration: .5 }, "reset")
            .to(chipBar, { scaleX: .7, duration: .5 }, "reset")
            .set(scanBadge, { autoAlpha: 0 }, "reset")
            .set(pointer, { left: "72%", top: "72%" }, "reset")
            .call(setBudgetLabels, [false], "reset")
            .call(() => fields.forEach(field => { field.textContent = ""; }), null, "reset");
        counters.forEach(counter => loop.add(
            count(counter, Number(counter.dataset.next), Number(counter.dataset.count), { duration: .5 }), "reset"));
        return loop;
    }

    let loop;
    let started = false;

    function start() {
        if (started) {
            return;
        }
        started = true;
        intro.eventCallback("onComplete", () => {
            loop = buildLoop();
            gsap.to(chipBudget, { y: -7, duration: 2.4, yoyo: true, repeat: -1, ease: "sine.inOut" });
        });
        intro.play();

        // Pause the loop while the hero is off screen.
        if ("IntersectionObserver" in window) {
            new IntersectionObserver(entries => {
                const visible = entries[0].isIntersecting;
                if (loop) {
                    visible ? loop.resume() : loop.pause();
                }
            }).observe(figure);
        }

        // Cards over the frame drift a little with the mouse.
        const drift = [chipBudget, chipScan].map((chip, index) => ({
            x: gsap.quickTo(chip, "x", { duration: .8, ease: "power3.out" }),
            depth: index ? -14 : 10
        }));
        figure.addEventListener("pointermove", event => {
            const box = figure.getBoundingClientRect();
            const offset = (event.clientX - box.left) / box.width - .5;
            drift.forEach(item => item.x(offset * item.depth));
        });
        figure.addEventListener("pointerleave", () => drift.forEach(item => item.x(0)));
    }

    return { start };
})();
