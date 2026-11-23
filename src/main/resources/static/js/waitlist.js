const form = document.getElementById("waitlistForm");
const joinButton = document.getElementById("joinButton");
const loadingMessage = document.getElementById("loadingMessage");
const confirmation = document.getElementById("confirmation");
const errorMessage = document.getElementById("errorMessage");
const waitlistStatus = document.getElementById("waitlistStatus");
const serviceSelect = document.getElementById("serviceId");
const dateInput = document.getElementById("appointmentDate");
const startTimeInput = document.getElementById("startTime");
const waitingCountMessage = document.getElementById("waitingCountMessage");

let countRequestVersion = 0;
let countRefreshTimer;

// WAIT-7: Update the waiting count whenever the selected slot changes
function refreshWaitingCount() {
    countRequestVersion++;
    const requestVersion = countRequestVersion;
    clearTimeout(countRefreshTimer);
    waitingCountMessage.hidden = true;

    const serviceId = serviceSelect.value;
    const appointmentDate = dateInput.value;
    const startTime = startTimeInput.value;

    // Wait until the customer has selected a complete appointment slot
    if (!serviceId || !appointmentDate || !startTime) {
        return;
    }

    // Avoid requesting the count for every keystroke
    countRefreshTimer = setTimeout(async () => {
        waitingCountMessage.textContent = "Checking current waiting count...";
        waitingCountMessage.hidden = false;

        const params = new URLSearchParams({
            serviceId,
            appointmentDate,
            startTime
        });

        try {
            const response = await fetch(`/api/waitlist/count?${params}`);

            if (requestVersion !== countRequestVersion) {
                return;
            }

            if (!response.ok) {
                throw new Error("Unable to retrieve waiting count.");
            }

            const data = await response.json();

            if (requestVersion !== countRequestVersion) {
                return;
            }

            const count = Number(data.waitingCount);

            if (!Number.isSafeInteger(count) || count < 0) {
                throw new Error("Invalid waiting count.");
            }

            const people = count === 1 ? "person" : "people";

            waitingCountMessage.textContent =
                `${count} ${people} currently waiting. ` +
                `If eligible, your estimated queue position would be #${count + 1}.`;

        } catch (error) {
            if (requestVersion !== countRequestVersion) {
                return;
            }

            waitingCountMessage.textContent =
                "Unable to load the current waiting count.";
        }
    }, 250);
}

// Update on edits and completed selections
[serviceSelect, dateInput, startTimeInput].forEach(input => {
    input.addEventListener("input", refreshWaitingCount);
    input.addEventListener("change", refreshWaitingCount);
});

// Load pharmacy services into the dropdown
async function loadServices() {
    try {
        const response = await fetch("/api/pharmacy-services");

        if (!response.ok) {
            throw new Error("Unable to load pharmacy services.");
        }

        const services = await response.json();

        serviceSelect.replaceChildren(
            new Option("Select a service", "")
        );

        services.filter(service => service.active).forEach(service => {
            const option = new Option(
                `${service.name} (${service.durationMinutes} min)`,
                service.serviceId
            );

            serviceSelect.appendChild(option);
        });

        // Allow the booking page to preselect a service
        const params = new URLSearchParams(window.location.search);
        const serviceId = params.get("serviceId");

        if (serviceId) {
            serviceSelect.value = serviceId;
        }

        if (params.has("date")) {
            document.getElementById("appointmentDate").value =
                params.get("date");
        }

        if (params.has("startTime")) {
            document.getElementById("startTime").value =
                params.get("startTime");
        }

        if (params.has("endTime")) {
            document.getElementById("endTime").value =
                params.get("endTime");
        }

        // Also update the count for slots preselected on the booking page
        refreshWaitingCount();

    } catch (error) {
        serviceSelect.replaceChildren(
            new Option("Unable to load services", "")
        );

        errorMessage.textContent = error.message;
        errorMessage.hidden = false;
    }
}

// Set up shared navigation and load services
setUpLoggedInPage();
loadServices();

form.addEventListener("submit", async function(event) {
    event.preventDefault();

    confirmation.hidden = true;
    errorMessage.hidden = true;
    loadingMessage.hidden = false;
    joinButton.disabled = true;

    const entry = {
        serviceId: Number(serviceSelect.value),
        appointmentDate:
            document.getElementById("appointmentDate").value,
        startTime:
            document.getElementById("startTime").value,
        endTime:
            document.getElementById("endTime").value
    };

    try {
        // Get CSRF token from the shared authentication system
        const csrfResponse = await fetch("/csrf");

        if (!csrfResponse.ok) {
            throw new Error("Could not obtain CSRF token.");
        }

        const csrfData = await csrfResponse.json();

        // Submit request as the authenticated customer
        const response = await fetch("/api/waitlist", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                [csrfData.headerName]: csrfData.token
            },
            body: JSON.stringify(entry)
        });

        const responseText = await response.text();

        if (!response.ok) {
            let message = responseText;

            try {
                const parsed = JSON.parse(responseText);
                message =
                    parsed.message ||
                    parsed.error ||
                    responseText;
            } catch (e) {
                // Keep plain-text errors from the controller
            }

            throw new Error(
                message || "Unable to join the waitlist."
            );
        }

        const savedEntry = JSON.parse(responseText);

        waitlistStatus.textContent =
            savedEntry.status || "WAITING";

        confirmation.hidden = false;

        // After a successful registration hide the pre join estimate 
        countRequestVersion++;
        clearTimeout(countRefreshTimer);
        waitingCountMessage.hidden = true;

    } catch (error) {
        errorMessage.textContent =
            error.message || "An unexpected error occurred.";

        errorMessage.hidden = false;

    } finally {
        loadingMessage.hidden = true;
        joinButton.disabled = false;
    }
});