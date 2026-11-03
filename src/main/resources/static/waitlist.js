const form = document.getElementById("waitlistForm");
const joinButton = document.getElementById("joinButton");
const loadingMessage = document.getElementById("loadingMessage");
const confirmation = document.getElementById("confirmation");
const errorMessage = document.getElementById("errorMessage");
const waitlistStatus = document.getElementById("waitlistStatus");

form.addEventListener("submit", async function(event) {
    event.preventDefault();

    confirmation.hidden = true;
    errorMessage.hidden = true;
    loadingMessage.hidden = false;
    joinButton.disabled = true;

    const entry = {
        serviceId: Number(
            document.getElementById("serviceId").value
        ),
        appointmentDate:
            document.getElementById("appointmentDate").value,
        startTime:
            document.getElementById("startTime").value,
        endTime:
            document.getElementById("endTime").value
    };

    try {
        // Get CSRF token from the existing authentication system
        const csrfResponse = await fetch("/csrf");

        if (!csrfResponse.ok) {
            throw new Error("Could not obtain CSRF token.");
        }

        const csrfData = await csrfResponse.json();

        // Submit the request as the logged-in customer
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
