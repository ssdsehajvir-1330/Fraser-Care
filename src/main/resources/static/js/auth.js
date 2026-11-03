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

// Links shown in the top nav for each type of user
function navLinksFor(user) {
    if (!user) {
        return [
            ["Book Appointment", "/#booking"],
            ["Login", "/login.html"],
            ["Sign Up", "/signup.html"]
        ];
    }

    if (user.roles.includes("ROLE_ADMIN")) {
        return [
            ["Users", "/admin-users.html"],
            ["Staff Dashboard", "/staff-dashboard.html"],
            ["Drug Search", "/drug-search.html"]
        ];
    }

    if (user.roles.includes("ROLE_STAFF")) {
        return [
            ["Staff Dashboard", "/staff-dashboard.html"],
            ["Drug Search", "/drug-search.html"]
        ];
    }

    return [
        ["My Account", "/customer-home.html"],
        ["Book Appointment", "/#booking"],
        ["Self-Check", "/self-check.html"],
        ["Drug Search", "/drug-search.html"]
    ];
}

// Builds the top nav inside <div id="site-nav"></div>.
// Shows the right links for the user, their name and a Log out button.
async function renderSiteNav() {
    const container = document.getElementById("site-nav");
    const user = await getCurrentUser();

    if (container) {
        container.className = "site-nav";
        container.replaceChildren();

        const logo = document.createElement("a");
        logo.href = "/";
        logo.className = "nav-logo";
        logo.textContent = "FraserCare";

        const links = document.createElement("div");
        links.className = "nav-links";

        navLinksFor(user).forEach(([label, href]) => {
            const link = document.createElement("a");
            link.href = href;
            link.textContent = label;

            if (window.location.pathname === href) {
                link.classList.add("active");
            }

            links.appendChild(link);
        });

        if (user) {
            const name = document.createElement("span");
            name.className = "nav-user";
            name.textContent = user.fullName || user.email;

            const logout = document.createElement("form");
            logout.className = "logout-form";
            logout.action = "/logout";
            logout.method = "post";

            const button = document.createElement("button");
            button.type = "submit";
            button.textContent = "Log out";

            logout.appendChild(button);
            links.append(name, logout);
        }

        container.append(logo, links);
    }

    if (user) {
        document.querySelectorAll(".user-name").forEach(element => {
            element.textContent = user.fullName || user.email;
        });

        await prepareLogoutForms();
    }

    return user;
}
