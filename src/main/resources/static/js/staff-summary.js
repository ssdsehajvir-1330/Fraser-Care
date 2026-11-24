// Today's summary cards at the top of the staff dashboard
const summaryMessage = document.getElementById("summary-message");

// Which number goes in which card
const SUMMARY_FIELDS = {
    appointmentsToday: "summary-appointments",
    pickupsReceived: "summary-received",
    pickupsReady: "summary-ready",
    waitlistToday: "summary-waitlist"
};

async function loadStaffSummary() {
    try {
        const response = await fetch("/api/staff/summary", {
            headers: { Accept: "application/json" },
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error("Could not load today's summary.");
        }

        const contentType = response.headers.get("content-type") || "";

        if (!contentType.includes("application/json")) {
            throw new Error("Please sign in again to see today's summary.");
        }

        const summary = await response.json();

        Object.entries(SUMMARY_FIELDS).forEach(([field, id]) => {
            document.getElementById(id).textContent = summary[field];
        });

        summaryMessage.hidden = true;

    } catch (error) {
        summaryMessage.textContent = error.message;
        summaryMessage.hidden = false;
    }
}

loadStaffSummary();

// Keep the numbers up to date while the page is open
setInterval(loadStaffSummary, 60000);