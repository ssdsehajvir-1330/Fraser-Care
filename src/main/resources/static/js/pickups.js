const pickupForm = document.getElementById("pickupForm");
const itemNameInput = document.getElementById("itemName");
const notesInput = document.getElementById("notes");
const submitButton = document.getElementById("submitButton");
const formMessage = document.getElementById("formMessage");
const listMessage = document.getElementById("listMessage");
const pickupTable = document.getElementById("pickupTable");
const pickupRows = document.getElementById("pickupRows");

// Readable labels for each status
const STATUS_LABELS = {
    RECEIVED: "Received",
    READY: "Ready for pickup",
    PICKED_UP: "Picked up"
};

function showFormMessage(text, isError) {
    formMessage.textContent = text;
    formMessage.className = "form-message " + (isError ? "error" : "success");
}

function formatDateTime(value) {
    if (!value) {
        return "-";
    }

    return new Date(value).toLocaleString("en-CA", {
        dateStyle: "medium",
        timeStyle: "short"
    });
}

// Load the logged-in customer's pickups
async function loadPickups() {
    try {
        const response = await fetch("/api/pickups/mine", {
            headers: { Accept: "application/json" },
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error("Could not load your pickups.");
        }

        if (!response.headers.get("content-type")?.includes("application/json")) {
            throw new Error("Please log in again to see your pickups.");
        }

        const pickups = await response.json();

        pickupRows.replaceChildren();

        if (pickups.length === 0) {
            pickupTable.hidden = true;
            listMessage.className = "muted";
            listMessage.textContent = "You have no pickup requests yet.";
            listMessage.hidden = false;
            return;
        }

        pickups.forEach(pickup => {
            const row = document.createElement("tr");

            const itemCell = document.createElement("td");
            itemCell.textContent = pickup.itemName;

            const notesCell = document.createElement("td");
            notesCell.textContent = pickup.notes || "-";

            const createdCell = document.createElement("td");
            createdCell.textContent = formatDateTime(pickup.createdAt);

            const updatedCell = document.createElement("td");
            updatedCell.textContent = formatDateTime(pickup.updatedAt);

            const statusCell = document.createElement("td");
            const badge = document.createElement("span");
            badge.className = "pickup-status " + pickup.status.toLowerCase();
            badge.textContent = STATUS_LABELS[pickup.status] || pickup.status;
            statusCell.appendChild(badge);

            row.append(itemCell, notesCell, createdCell, updatedCell, statusCell);
            pickupRows.appendChild(row);
        });

        listMessage.hidden = true;
        pickupTable.hidden = false;

    } catch (error) {
        pickupTable.hidden = true;
        listMessage.className = "error";
        listMessage.textContent = error.message;
        listMessage.hidden = false;
    }
}

// Submit a new pickup request
pickupForm.addEventListener("submit", async event => {
    event.preventDefault();

    const itemName = itemNameInput.value.trim();
    const notes = notesInput.value.trim();

    if (itemName.length < 2) {
        showFormMessage("Item name must be at least 2 characters.", true);
        return;
    }

    submitButton.disabled = true;
    formMessage.textContent = "";

    try {
        const csrfResponse = await fetch("/csrf", { credentials: "same-origin" });

        if (!csrfResponse.ok) {
            throw new Error("Could not submit right now. Please refresh the page.");
        }

        const csrf = await csrfResponse.json();

        const response = await fetch("/api/pickups", {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json",
                [csrf.headerName]: csrf.token
            },
            body: JSON.stringify({ itemName, notes })
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || "Could not submit your pickup request.");
        }

        pickupForm.reset();
        showFormMessage("Pickup request submitted. Status: Received.", false);
        await loadPickups();

    } catch (error) {
        showFormMessage(error.message, true);

    } finally {
        submitButton.disabled = false;
    }
});

loadPickups();