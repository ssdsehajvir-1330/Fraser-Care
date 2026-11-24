// Staff pickup requests section
const pickupFilter = document.getElementById("pickup-filter");
const pickupTable = document.getElementById("pickup-table");
const pickupRows = document.getElementById("pickup-rows");
const pickupEmpty = document.getElementById("pickup-empty");
const pickupMessage = document.getElementById("pickup-message");

const PICKUP_LABELS = {
    RECEIVED: "Received",
    READY: "Ready for pickup",
    PICKED_UP: "Picked up"
};

// Next status and button text for each status
const NEXT_STEP = {
    RECEIVED: { status: "READY", label: "Mark Ready" },
    READY: { status: "PICKED_UP", label: "Mark Picked Up" }
};

function showPickupMessage(text, isError) {
    pickupMessage.textContent = text;
    pickupMessage.className = "feedback-message" + (isError ? " error" : "");
    pickupMessage.hidden = false;
}

function formatPickupDate(value) {
    if (!value) {
        return "-";
    }

    return new Date(value).toLocaleString("en-CA", {
        dateStyle: "medium",
        timeStyle: "short"
    });
}

// Build one table row for a pickup
function buildPickupRow(pickup) {
    const row = document.createElement("tr");

    const customerCell = document.createElement("td");
    customerCell.textContent = pickup.customerName ||
        `Customer #${pickup.customerId}`;

    const itemCell = document.createElement("td");
    itemCell.textContent = pickup.itemName;

    const notesCell = document.createElement("td");
    notesCell.textContent = pickup.notes || "-";

    const createdCell = document.createElement("td");
    createdCell.textContent = formatPickupDate(pickup.createdAt);

    const statusCell = document.createElement("td");
    const badge = document.createElement("span");
    badge.className = "pickup-status " + pickup.status.toLowerCase();
    badge.textContent = PICKUP_LABELS[pickup.status] || pickup.status;
    statusCell.appendChild(badge);

    const readyCell = document.createElement("td");
    readyCell.textContent = formatPickupDate(pickup.readyBy);

    const actionCell = document.createElement("td");
    const next = NEXT_STEP[pickup.status];

    if (next) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "pickup-action";
        button.textContent = next.label;

        button.addEventListener("click", () => {
            updatePickupStatus(pickup.pickupId, next.status, button);
        });

        actionCell.append(button, buildReadyTimeForm(pickup));
    } else {
        actionCell.textContent = "Done";
    }

    row.append(customerCell, itemCell, notesCell, createdCell,
        statusCell, readyCell, actionCell);
    return row;
}

// Load pickups for the selected filter
async function loadStaffPickups() {
    const status = pickupFilter.value;
    const url = status
        ? `/api/staff/pickups?status=${encodeURIComponent(status)}`
        : "/api/staff/pickups";

    pickupRows.replaceChildren();

    try {
        const response = await fetch(url, {
            headers: { Accept: "application/json" },
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error("Unable to retrieve pickups. Status: " + response.status);
        }

        const contentType = response.headers.get("content-type") || "";

        if (!contentType.includes("application/json")) {
            throw new Error("Please sign in again to see pickups.");
        }

        const pickups = await response.json();

        if (pickups.length === 0) {
            pickupTable.hidden = true;
            pickupEmpty.hidden = false;
            return;
        }

        pickups.forEach(pickup => pickupRows.appendChild(buildPickupRow(pickup)));

        pickupEmpty.hidden = true;
        pickupTable.hidden = false;

    } catch (error) {
        console.error("Error loading pickups:", error);
        pickupTable.hidden = true;
        pickupEmpty.hidden = true;
        showPickupMessage(error.message, true);
    }
}

// Move a pickup to its next status
async function updatePickupStatus(pickupId, newStatus, button) {
    button.disabled = true;

    try {
        const csrfResponse = await fetch("/csrf", { credentials: "same-origin" });

        if (!csrfResponse.ok) {
            throw new Error("Could not update right now. Please refresh the page.");
        }

        const csrf = await csrfResponse.json();

        const response = await fetch(`/api/staff/pickups/${pickupId}/status`, {
            method: "PATCH",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json",
                [csrf.headerName]: csrf.token
            },
            body: JSON.stringify({ status: newStatus })
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || "Could not update the pickup.");
        }

        showPickupMessage(`Pickup marked as ${PICKUP_LABELS[newStatus]}.`, false);
        await loadStaffPickups();

    } catch (error) {
        showPickupMessage(error.message, true);
        button.disabled = false;
    }
}

// Current local time in the format a datetime-local input uses
function currentLocalTime() {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
}

// Time picker and button for setting the ready time
function buildReadyTimeForm(pickup) {
    const wrapper = document.createElement("div");
    wrapper.className = "ready-time-form";

    const input = document.createElement("input");
    input.type = "datetime-local";
    input.className = "ready-time-input";
    input.min = currentLocalTime();
    input.setAttribute("aria-label", "Ready by time for " + pickup.itemName);

    if (pickup.readyBy) {
        input.value = pickup.readyBy.slice(0, 16);
    }

    const button = document.createElement("button");
    button.type = "button";
    button.className = "pickup-action secondary";
    button.textContent = pickup.readyBy ? "Change Time" : "Set Time";

    button.addEventListener("click", () => {
        setPickupReadyTime(pickup.pickupId, input, button);
    });

    wrapper.append(input, button);
    return wrapper;
}

// Save the ready time for a pickup
async function setPickupReadyTime(pickupId, input, button) {
    if (!input.value) {
        showPickupMessage("Please pick a ready time first.", true);
        return;
    }

    button.disabled = true;

    try {
        const csrfResponse = await fetch("/csrf", { credentials: "same-origin" });

        if (!csrfResponse.ok) {
            throw new Error("Could not update right now. Please refresh the page.");
        }

        const csrf = await csrfResponse.json();

        const response = await fetch(`/api/staff/pickups/${pickupId}/ready-time`, {
            method: "PATCH",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json",
                [csrf.headerName]: csrf.token
            },
            body: JSON.stringify({ readyBy: input.value })
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || "Could not save the ready time.");
        }

        showPickupMessage("Ready time saved.", false);
        await loadStaffPickups();

    } catch (error) {
        showPickupMessage(error.message, true);
        button.disabled = false;
    }
}

pickupFilter.addEventListener("change", () => {
    pickupMessage.hidden = true;
    loadStaffPickups();
});

loadStaffPickups();