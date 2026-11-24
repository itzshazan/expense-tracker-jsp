"use strict";

// Tell the server the browser's time zone so "today" and "this month" match the user's clock.
try {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if (zone) {
        document.cookie = "tz=" + encodeURIComponent(zone) + "; path=/; max-age=31536000; SameSite=Lax";
    }
} catch {
    // Older browsers: the server falls back to its own date.
}

for (const form of document.querySelectorAll("form[data-confirm]")) {
    form.addEventListener("submit", event => {
        if (!window.confirm(form.dataset.confirm)) {
            event.preventDefault();
        }
    });
}

for (const button of document.querySelectorAll("[data-print]")) {
    button.addEventListener("click", () => window.print());
}

for (const button of document.querySelectorAll("[data-password-toggle]")) {
    const field = document.getElementById(button.dataset.passwordToggle);
    button.addEventListener("click", () => {
        const visible = field.type === "password";
        field.type = visible ? "text" : "password";
        button.setAttribute("aria-label", visible ? "Hide password" : "Show password");
        button.setAttribute("aria-pressed", String(visible));
    });
}

const receiptForm = document.querySelector("[data-receipt-form]");
if (receiptForm) {
    const file = receiptForm.elements.namedItem("receipt");
    const scan = receiptForm.querySelector("[data-scan]");
    const apply = receiptForm.querySelector("[data-apply]");
    const note = receiptForm.querySelector("[data-scan-note]");
    let controller;
    let suggestions;

    const reset = () => {
        controller?.abort();
        controller = undefined;
        suggestions = undefined;
        apply.hidden = true;
        scan.disabled = false;
        note.textContent = "";
    };

    const scanReceipt = async () => {
        reset();
        if (!file.files.length) {
            note.textContent = "Choose a receipt image first.";
            return;
        }
        const request = new AbortController();
        controller = request;
        const payload = new FormData();
        payload.append("csrf", receiptForm.elements.namedItem("csrf").value);
        payload.append("receipt", file.files[0]);
        scan.disabled = true;
        note.textContent = "Scanning receipt…";
        try {
            const response = await fetch(receiptForm.dataset.scanUrl, {
                method: "POST",
                body: payload,
                signal: request.signal,
                headers: { "Accept": "application/json" }
            });
            if (!response.headers.get("content-type")?.includes("application/json")) {
                throw new Error("Could not scan the receipt. Check the file size and reload if your session expired.");
            }
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || "Could not scan the receipt.");
            }
            const values = [data.merchant, data.amount, data.date].filter(Boolean);
            if (values.length) {
                suggestions = data;
                note.textContent = "Suggested details: " + values.join(" · ");
                apply.hidden = false;
            } else {
                note.textContent = "No details found. Enter them manually and save the receipt with the expense.";
            }
        } catch (error) {
            if (!request.signal.aborted) {
                note.textContent = error.message || "Could not scan the receipt.";
            }
        } finally {
            if (controller === request) {
                scan.disabled = false;
            }
        }
    };

    file.addEventListener("change", () => {
        if (file.files.length && receiptForm.dataset.autoscan === "true") {
            void scanReceipt();
        } else {
            reset();
        }
    });
    scan.addEventListener("click", () => void scanReceipt());
    apply.addEventListener("click", () => {
        for (const field of ["merchant", "amount", "date"]) {
            if (suggestions?.[field]) {
                receiptForm.elements.namedItem(field).value = suggestions[field];
            }
        }
        apply.hidden = true;
        note.textContent = "Suggestions applied. Review the amount and date before saving.";
    });
}
