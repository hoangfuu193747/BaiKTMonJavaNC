"use strict";
document.querySelectorAll("[data-password]").forEach((button) => {
    button.addEventListener("click", () => {
        const input = document.getElementById(button.dataset.password);
        const show = input.type === "password";
        input.type = show ? "text" : "password";
        button.textContent = show ? "Ẩn" : "Hiện";
        button.setAttribute("aria-label", show ? "Ẩn mật khẩu" : "Hiện mật khẩu");
        button.setAttribute("aria-pressed", String(show));
    });
});

