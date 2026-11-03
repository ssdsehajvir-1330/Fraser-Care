const form = document.getElementById("waitlistForm");
const joinButton = document.getElementById("joinButton");
const loadingMessage = document.getElementById("loadingMessage");
const confirmation = document.getElementById("confirmation");
const errorMessage = document.getElementById("errorMessage");
const waitlistStatus = document.getElementById("waitlistStatus");
const serviceSelect = document.getElementById("serviceId");

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

    } catch (error) {
        errorMessage.textContent =
            error.message || "An unexpected error occurred.";

        errorMessage.hidden = false;

    } finally {
        loadingMessage.hidden = true;
        joinButton.disabled = false;
    }
});