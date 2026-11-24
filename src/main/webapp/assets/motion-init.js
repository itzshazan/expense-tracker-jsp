"use strict";

// Runs in <head> before the first paint. The .motion class hides elements that motion.js
// animates in, so they do not flash in their final position first.
document.documentElement.classList.add("motion");

// Safety net: if GSAP or motion.js fails to load, show the page anyway.
setTimeout(() => {
    if (!document.documentElement.classList.contains("motion-ready")) {
        document.documentElement.classList.remove("motion");
    }
}, 2500);
