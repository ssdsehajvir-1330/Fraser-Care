const waitlistsList = document.getElementById("waitlistsList");
const waitlistsMessage = document.getElementById("waitlistsMessage");
const refreshWaitlistsButton = document.getElementById("refreshWaitlistsButton");

// Format a date string in more readable format 
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

// Time string in readable format
function formatWaitlistTime(timeValue) {
    const [hours, minutes] = timeValue.split(":").map(Number);
    const period = hours >= 12 ? "PM" : "AM";
    const displayHours = hours % 12 || 12;

    return `${displayHours}:${String(minutes).padStart(2, "0")} ${period}`;
}

// Load pharmacy service names from the server and return a map of serviceId to name
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

// Create a waitlist card element for a given entry and service names
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
    return card;
}

// Load customer's waitlists from the server and show them
async function loadCustomerWaitlists() {
    refreshWaitlistsButton.disabled = true;
    waitlistsMessage.textContent = "";
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
            return;
        }

        const serviceNames = await loadWaitlistServiceNames();

        entries.forEach(entry => {
            waitlistsList.appendChild(makeWaitlistCard(entry, serviceNames));
        });
    } catch (error) {
        waitlistsList.replaceChildren();
        waitlistsMessage.textContent = error.message;
        waitlistsMessage.className = "waitlists-message error";
    } finally {
        refreshWaitlistsButton.disabled = false;
    }
}

refreshWaitlistsButton.addEventListener("click", loadCustomerWaitlists);

document.addEventListener("DOMContentLoaded", loadCustomerWaitlists);
