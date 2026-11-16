const waitlistsList = document.getElementById("waitlistsList");
const waitlistsMessage = document.getElementById("waitlistsMessage");
const refreshWaitlistsButton = document.getElementById("refreshWaitlistsButton");

function formatWaitlistDate(dateValue) {
    const [year, month, day] = dateValue.split("-").map(Number);
    const date = new Date(Date.UTC(year, month - 1, day, 12));

    return new Intl.DateTimeFormat("en-CA", {
        year: "numeric",
        month: "long",
        day: "numeric",
        timeZone: "UTC"
    }).format(date);
}

function formatWaitlistTime(timeValue) {
    const [hours, minutes] = timeValue.split(":").map(Number);
    const period = hours >= 12 ? "PM" : "AM";
    const displayHours = hours % 12 || 12;

    return `${displayHours}:${String(minutes).padStart(2, "0")} ${period}`;
}

async function loadWaitlistServiceNames() {
    try {
        const response = await fetch("/api/pharmacy-services", {
            credentials: "same-origin",
            headers: { Accept: "application/json" }
        });

        if (!response.ok || response.redirected ||
            !response.headers.get("content-type")?.includes("application/json")) {
            throw new Error("Service names could not be loaded.");
        }

        const services = await response.json();

        return new Map(services.map(service => [
            String(service.serviceId),
            service.name
        ]));
    } catch (error) {
        console.error("Could not load pharmacy service names:", error);
        return new Map();
    }
}

function makeWaitlistCard(entry, serviceNames) {
    const card = document.createElement("article");
    card.className = "waitlist-card";

    const header = document.createElement("div");
    header.className = "waitlist-card-header";

    const serviceName = document.createElement("h3");
    serviceName.textContent =
        serviceNames.get(String(entry.serviceId)) ||
        `Pharmacy Service #${entry.serviceId}`;

    const status = String(entry.status || "UNKNOWN").toUpperCase();
    const statusBadge = document.createElement("span");
    statusBadge.className = "waitlist-status";
    statusBadge.textContent = status;

    if (status === "WAITING") {
        statusBadge.classList.add("waitlist-status-waiting");
    } else if (status === "NOTIFIED") {
        statusBadge.classList.add("waitlist-status-notified");
    } else {
        statusBadge.classList.add("waitlist-status-other");
    }

    header.append(serviceName, statusBadge);

    const details = document.createElement("div");
    details.className = "waitlist-details";

    const date = document.createElement("p");
    date.textContent = `Date: ${formatWaitlistDate(entry.appointmentDate)}`;

    const time = document.createElement("p");
    time.textContent =
        `Time: ${formatWaitlistTime(entry.startTime)} - ${formatWaitlistTime(entry.endTime)}`;

    details.append(date, time);

    const position = document.createElement("p");
    position.className = "waitlist-position";

    if (status === "WAITING" &&
        Number.isInteger(entry.queuePosition) && entry.queuePosition > 0) {
        position.textContent = `Position in line: #${entry.queuePosition}`;
    } else if (status === "WAITING") {
        position.textContent = "Queue position currently unavailable.";
    } else if (status === "NOTIFIED") {
        position.textContent = "Your waitlist status has been marked as notified.";
    } else {
        position.textContent = "This entry is no longer in the waiting queue.";
    }

    card.append(header, details, position);

    if (status === "WAITING" || status === "NOTIFIED") {
        const actions = document.createElement("div");
        actions.className = "waitlist-actions";

        const leaveButton = document.createElement("button");
        leaveButton.type = "button";
        leaveButton.className = "leave-waitlist-button";
        leaveButton.textContent = "Leave Waitlist";
        leaveButton.addEventListener("click", () => {
            leaveCustomerWaitlist(entry.waitlistId, leaveButton);
        });

        actions.appendChild(leaveButton);
        card.appendChild(actions);
    }

    return card;
}

async function loadCustomerWaitlists() {
    refreshWaitlistsButton.disabled = true;
    waitlistsMessage.textContent = "";
    waitlistsMessage.className = "waitlists-message";
    waitlistsList.textContent = "Loading your waitlists...";

    try {
        const response = await fetch("/api/waitlist/my", {
            credentials: "same-origin",
            headers: { Accept: "application/json" }
        });

        if (response.status === 401 || response.redirected) {
            throw new Error("Please log in again to view your waitlists.");
        }

        if (response.status === 403) {
            throw new Error("Only customer accounts can view waitlists.");
        }

        if (!response.ok ||
            !response.headers.get("content-type")?.includes("application/json")) {
            throw new Error("Unable to retrieve your waitlists.");
        }

        const entries = await response.json();

        if (!Array.isArray(entries)) {
            throw new Error("Unexpected waitlist information received.");
        }

        waitlistsList.replaceChildren();

        if (entries.length === 0) {
            const emptyMessage = document.createElement("p");
            emptyMessage.className = "empty-waitlists";
            emptyMessage.textContent = "You haven't joined any waitlists yet.";
            waitlistsList.appendChild(emptyMessage);
            return true;
        }

        const serviceNames = await loadWaitlistServiceNames();

        entries.forEach(entry => {
            waitlistsList.appendChild(makeWaitlistCard(entry, serviceNames));
        });
        return true;
    } catch (error) {
        waitlistsList.replaceChildren();
        waitlistsMessage.textContent = error.message;
        waitlistsMessage.className = "waitlists-message error";
        return false;
    } finally {
        refreshWaitlistsButton.disabled = false;
    }
}

async function leaveCustomerWaitlist(waitlistId, button) {
    const confirmed = window.confirm(
        "Are you sure you want to leave this waitlist? Your position will be lost."
    );
    if (!confirmed) {
        return;
    }

    button.disabled = true;
    button.textContent = "Leaving...";

    try {
        // The project uses CSRF protection for requests that change data
        const csrfResponse = await fetch("/csrf", {
            credentials: "same-origin",
            headers: { Accept: "application/json" }
        });

        if (!csrfResponse.ok || csrfResponse.redirected) {
            throw new Error("Unable to verify your session. Please log in again.");
        }
        const csrf = await csrfResponse.json();

        const response = await fetch(`/api/waitlist/${waitlistId}`, {
            method: "DELETE",
            credentials: "same-origin",
            headers: {
                Accept: "application/json",
                [csrf.headerName]: csrf.token
            }
        });

        if (response.status === 401 || response.redirected) {
            throw new Error("Please log in again to leave this waitlist.");
        }
        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || "Unable to leave this waitlist.");
        }

        const refreshed = await loadCustomerWaitlists();
        waitlistsMessage.className = refreshed
            ? "waitlists-message success"
            : "waitlists-message error";
        waitlistsMessage.textContent = refreshed
            ? "You left the waitlist successfully."
            : "You left the waitlist, but the list could not refresh. Please reload the page.";

    } catch (error) {
        waitlistsMessage.className = "waitlists-message error";
        waitlistsMessage.textContent = error.message;
        button.disabled = false;
        button.textContent = "Leave Waitlist";
    }
}

refreshWaitlistsButton.addEventListener("click", loadCustomerWaitlists);

document.addEventListener("DOMContentLoaded", loadCustomerWaitlists);
