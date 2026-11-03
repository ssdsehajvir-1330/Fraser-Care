// Shared login helpers used by every page that shows a Logout button.

// Returns the logged-in user from /api/me, or null if nobody is logged in
async function getCurrentUser() {
    try {
        const response = await fetch("/api/me", { credentials: "same-origin" });
        return response.ok ? await response.json() : null;
    } catch (error) {
        console.error("Could not check login status:", error);
        return null;
    }
}

// Page each role lands on after logging in (matches SecurityConfig)
function landingPageFor(user) {
    if (user.roles.includes("ROLE_ADMIN")) {
        return "/admin-users.html";
    }

    if (user.roles.includes("ROLE_STAFF")) {
        return "/staff-dashboard.html";
    }

    return "/customer-home.html";
}

// Logout is a POST, so each logout form needs a CSRF token
async function prepareLogoutForms() {
    try {
        const response = await fetch("/csrf", { credentials: "same-origin" });
        const csrf = await response.json();

        document.querySelectorAll("form.logout-form").forEach(form => {
            let input = form.querySelector('input[name="_csrf"]');

            if (!input) {
                input = document.createElement("input");
                input.type = "hidden";
                input.name = "_csrf";
                form.appendChild(input);
            }

            input.value = csrf.token;
        });
    } catch (error) {
        console.error("Could not prepare logout:", error);
    }
}

// Fills any element with class "user-name" and sets up logout forms
async function setUpLoggedInPage() {
    const user = await getCurrentUser();

    if (user) {
        document.querySelectorAll(".user-name").forEach(element => {
            element.textContent = user.fullName || user.email;
        });
    }

    await prepareLogoutForms();

    return user;
}
