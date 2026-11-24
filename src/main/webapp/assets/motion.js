"use strict";

// Page animations with GSAP. As requested, they play even when the operating system asks
// for reduced motion, so nothing here checks prefers-reduced-motion.
(() => {
    const root = document.documentElement;
    if (!window.gsap) {
        root.classList.remove("motion");
        return;
    }
    root.classList.add("motion-ready");
    gsap.defaults({ ease: "power3.out", duration: .7 });

    const $ = (selector, scope = document) => scope.querySelector(selector);
    const $$ = (selector, scope = document) => Array.from(scope.querySelectorAll(selector));
    const reveal = { autoAlpha: 1, x: 0, y: 0, scale: 1, clearProps: "transform" };

    // Animate a <progress> bar from empty to its value.
    const fill = (bars, vars = {}) => bars
        .filter(bar => bar.hasAttribute("value"))
        .map(bar => gsap.fromTo(bar, { attr: { value: 0 } },
            { attr: { value: bar.value }, duration: 1.1, ease: "power2.out", ...vars }));

    function landing() {
        const intro = gsap.timeline();
        intro.fromTo("[data-motion='header']", { autoAlpha: 0, y: -24 }, { ...reveal, duration: .7 })
            .fromTo(".home-hero-copy > *", { autoAlpha: 0, y: 30 }, { ...reveal, stagger: .09, duration: .85 }, .1)
            .fromTo(".hero-product", { autoAlpha: 0, y: 56, scale: .95 }, { ...reveal, duration: 1.2, ease: "power4.out" }, .25)
            .fromTo(".home-highlights li", { autoAlpha: 0, y: 18 }, { ...reveal, stagger: .08, duration: .6 }, .7)
            .call(() => window.ExpenseHeroPreview?.start(), null, .85);

        if (!window.ScrollTrigger) {
            gsap.set("[data-reveal]", { autoAlpha: 1 });
            return;
        }
        gsap.registerPlugin(ScrollTrigger);
        const onView = (trigger, start = "top 86%") => ({ trigger, start, once: true });

        for (const section of $$("[data-reveal]")) {
            const timeline = gsap.timeline({ scrollTrigger: onView(section) });
            timeline.fromTo(section, { autoAlpha: 0, y: 46 }, { ...reveal, duration: .9 });
            const children = $$(":scope > .home-feature, :scope > details, :scope > .privacy-details > div, :scope > .workflow-panel:not([hidden])", section);
            if (children.length) {
                timeline.from(children, { autoAlpha: 0, y: 26, stagger: .1, duration: .7 }, "<.15");
            }
        }

        // Budget illustration fills when it comes into view.
        for (const bar of $$(".budget-illustration progress")) {
            fill([bar], { scrollTrigger: onView(bar, "top 92%"), duration: 1.4 });
        }

        // Closing section: cards fly in, then keep floating and follow the mouse a little.
        const finalSection = $(".home-final");
        if (finalSection) {
            const cards = $$("[data-float]", finalSection);
            gsap.timeline({ scrollTrigger: onView(finalSection, "top 78%") })
                .from($$(".home-final-copy > *", finalSection), { autoAlpha: 0, y: 26, stagger: .1, duration: .8 }, .2)
                .from(cards, { autoAlpha: 0, y: 70, rotation: (index) => [8, -10, 6][index] || 0, stagger: .14, duration: 1, ease: "back.out(1.4)" }, .3)
                .from($(".final-card-bar i", finalSection), { scaleX: 0, duration: 1.2, ease: "power2.out" }, "-=.4")
                .add(() => cards.forEach((card, index) => gsap.to(card, {
                    y: index % 2 ? 9 : -9, duration: 2.6 + index * .5, yoyo: true, repeat: -1, ease: "sine.inOut"
                })));

            const drift = cards.map((card, index) => ({
                x: gsap.quickTo(card, "x", { duration: .9, ease: "power3.out" }),
                depth: [14, -22, 18][index] || 10
            }));
            finalSection.addEventListener("pointermove", event => {
                const box = finalSection.getBoundingClientRect();
                const offset = (event.clientX - box.left) / box.width - .5;
                drift.forEach(item => item.x(offset * item.depth));
            });
            finalSection.addEventListener("pointerleave", () => drift.forEach(item => item.x(0)));
        }

        // The footer's big wordmark rises into place as the page bottoms out.
        const wordmark = $(".home-footer-wordmark");
        if (wordmark) {
            gsap.fromTo(wordmark, { yPercent: 45, autoAlpha: 0 }, {
                yPercent: 0, autoAlpha: 1, ease: "none",
                scrollTrigger: { trigger: ".home-footer", start: "top bottom", end: "bottom bottom", scrub: .6 }
            });
        }

        // FAQ answers slide open.
        for (const details of $$(".home-questions details")) {
            details.addEventListener("toggle", () => {
                if (details.open) {
                    gsap.fromTo($("p", details), { autoAlpha: 0, y: -8 }, { autoAlpha: 1, y: 0, duration: .4 });
                }
            });
        }
    }

    function auth() {
        const story = $("[data-motion='auth-story']");
        const card = $("[data-motion='auth-card']");
        const timeline = gsap.timeline();

        timeline.fromTo(story, { autoAlpha: 0, x: -36, scale: .98 }, { ...reveal, duration: 1 })
            .from($$(".auth-story-intro > *", story), { autoAlpha: 0, y: 26, stagger: .12, duration: .8 }, "-=.6")
            .from($$(".auth-perks li", story), { autoAlpha: 0, x: -22, stagger: .1, duration: .6 }, "-=.45")
            .from($(".auth-budget", story), { autoAlpha: 0, y: 60, rotation: 9, duration: 1, ease: "back.out(1.4)" }, "-=.45")
            .from($(".auth-budget-bar span", story), { scaleX: 0, duration: 1.1, ease: "power2.out" }, "-=.45")
            .add(() => gsap.to($(".auth-budget", story), { y: -8, duration: 2.6, yoyo: true, repeat: -1, ease: "sine.inOut" }));

        timeline.fromTo([".auth-brand", card], { autoAlpha: 0, y: 30 }, { ...reveal, stagger: .1, duration: .85 }, .15)
            .from($$(".credential-heading > *, .credential-form > *, .credential-divider, .credential-google, .credential-links", card),
                { autoAlpha: 0, y: 14, stagger: .05, duration: .5 }, .45);

        // Fields lift slightly when focused.
        for (const field of $$(".credential-field", card)) {
            field.addEventListener("focusin", () => gsap.to(field, { y: -2, duration: .25 }));
            field.addEventListener("focusout", () => gsap.to(field, { y: 0, duration: .25 }));
        }
    }

    function workspace() {
        const main = $(".workspace-main");
        const timeline = gsap.timeline();
        timeline.fromTo(".sidebar nav a, .sidebar-cta", { autoAlpha: 0, x: -12 }, { ...reveal, stagger: .05, duration: .5 }, 0)
            .fromTo($$(":scope > *", main), { autoAlpha: 0, y: 22 }, { ...reveal, stagger: .08, duration: .7 }, .05);

        for (const group of $$("[data-motion='group']", main)) {
            timeline.from($$(":scope > *", group), { autoAlpha: 0, y: 18, scale: .98, stagger: .07, duration: .6, clearProps: "transform" }, .15);
        }
        timeline.from($$("tbody tr", main), { autoAlpha: 0, x: -10, stagger: .035, duration: .45 }, .4);
        fill($$("progress", main), { delay: .35 });
    }

    const screen = document.body.dataset.screen;
    if (screen === "landing") {
        landing();
    } else if (screen === "auth") {
        auth();
    } else if (document.body.classList.contains("workspace")) {
        workspace();
    }

    // Every animated element now has an inline starting state, so the CSS hiding can go.
    root.classList.remove("motion");
    // Anything left hidden by .motion on pages without animation (e.g. a public error page)
    // is shown by removing the class above.
})();
