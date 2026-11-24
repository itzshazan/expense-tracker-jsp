"use strict";

const workflow = document.querySelector("[data-workflow]");
const tabList = workflow.querySelector('[role="tablist"]');
const tabs = Array.from(tabList.querySelectorAll('[role="tab"]'));
const panels = tabs.map(tab => document.getElementById(tab.getAttribute("aria-controls")));

function selectTab(activeTab, animate = false) {
    tabs.forEach((tab, index) => {
        const selected = tab === activeTab;
        tab.setAttribute("aria-selected", String(selected));
        tab.tabIndex = selected ? 0 : -1;
        panels[index].hidden = !selected;
        if (selected && animate && window.gsap) {
            const panel = panels[index];
            gsap.timeline()
                .fromTo(panel, { autoAlpha: 0, y: 14 }, { autoAlpha: 1, y: 0, duration: .45, ease: "power3.out", clearProps: "transform" })
                .from(panel.querySelectorAll(".workflow-copy > *"), { y: 12, autoAlpha: 0, duration: .4, stagger: .06, ease: "power2.out" }, "<.05")
                .from(panel.querySelectorAll(".workflow-visual > *"), { y: 22, scale: .96, autoAlpha: 0, duration: .55, stagger: .1, ease: "back.out(1.6)" }, "<.1");
        }
    });
}

panels.forEach((panel, index) => {
    panel.setAttribute("role", "tabpanel");
    panel.setAttribute("aria-labelledby", tabs[index].id);
    panel.tabIndex = 0;
});
selectTab(tabs[0]);
tabList.hidden = false;

tabs.forEach((tab, index) => {
    tab.addEventListener("click", () => selectTab(tab, true));
    tab.addEventListener("keydown", event => {
        const nextIndex = {
            ArrowLeft: (index - 1 + tabs.length) % tabs.length,
            ArrowRight: (index + 1) % tabs.length,
            Home: 0,
            End: tabs.length - 1
        }[event.key];
        if (nextIndex === undefined) {
            return;
        }
        event.preventDefault();
        selectTab(tabs[nextIndex], true);
        tabs[nextIndex].focus();
    });
});

// Give the pinned navbar a stronger look once the page has scrolled.
const header = document.querySelector(".home-header");
if (header) {
    let queued = false;
    const update = () => {
        header.classList.toggle("is-scrolled", window.scrollY > 8);
        queued = false;
    };
    window.addEventListener("scroll", () => {
        if (!queued) {
            queued = true;
            requestAnimationFrame(update);
        }
    }, { passive: true });
    update();

    // Phones: fold the section links into a menu button.
    const toggle = header.querySelector(".home-menu-toggle");
    const menu = document.getElementById(toggle?.getAttribute("aria-controls"));
    if (toggle && menu) {
        const label = toggle.querySelector(".sr-only");
        const setOpen = open => {
            header.classList.toggle("is-open", open);
            toggle.setAttribute("aria-expanded", String(open));
            label.textContent = open ? "Close menu" : "Open menu";
            if (open && window.gsap) {
                gsap.fromTo(menu.children, { autoAlpha: 0, y: -6 }, { autoAlpha: 1, y: 0, stagger: .035, duration: .3, ease: "power2.out", clearProps: "transform" });
            }
        };
        header.classList.add("has-menu");
        toggle.addEventListener("click", () => setOpen(!header.classList.contains("is-open")));
        menu.addEventListener("click", event => {
            if (event.target.closest("a")) {
                setOpen(false);
            }
        });
        document.addEventListener("click", event => {
            if (header.classList.contains("is-open") && !header.contains(event.target)) {
                setOpen(false);
            }
        });
        document.addEventListener("keydown", event => {
            if (event.key === "Escape" && header.classList.contains("is-open")) {
                setOpen(false);
                toggle.focus();
            }
        });
        window.matchMedia("(min-width: 691px)").addEventListener("change", event => {
            if (event.matches) {
                setOpen(false);
            }
        });
    }
}
