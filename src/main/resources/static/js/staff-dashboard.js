// Get the date input
const dateInput = document.getElementById("appointment-date");

// Get today's date
const today = new Date();

const todayISO = [
    today.getFullYear(),
    String(today.getMonth() + 1).padStart(2, "0"),
    String(today.getDate()).padStart(2, "0")
].join("-");

// Set today's date as the default selected date
dateInput.value = todayISO;

// Convert time into a readable format
function formatTime(time) {
    if (!time) {
        return "N/A";
    }

    const [hours, minutes] = time.split(":");
    const date = new Date();

    date.setHours(Number(hours), Number(minutes));

    return date.toLocaleTimeString([], {
        hour: "numeric",
        minute: "2-digit"
    });
}

// Display a message when loading or when there are no appointments
function showMessage(title, message) {
    document.getElementById("table-container").hidden = true;

    const emptyState = document.getElementById("empty-state");

    emptyState.hidden = false;
    emptyState.querySelector("h3").textContent = title;
    emptyState.querySelector("p").textContent = message;
}

// Display appointments for the selected date
async function displayAppointments() {

    const selectedDate = dateInput.value;

    const table = document.getElementById("table-container");
    const emptyState = document.getElementById("empty-state");

    const appointmentRows =
        document.getElementById("appointment-rows");

    // Display the selected date
    const date = selectedDate
        ? new Date(selectedDate + "T00:00:00")
        : null;

    document.getElementById("today-date").textContent =
        date ? date.toLocaleDateString("en-CA", {
            weekday: "long",
            year: "numeric",
            month: "long",
            day: "numeric"
        }) : "Select an appointment date";

    // Reset the appointment count
    document.getElementById("appointment-count").textContent = "0";

    appointmentRows.innerHTML = "";

    if (!selectedDate) {
        showMessage("Select a date", "Choose a date to view appointments.");
        return;
    }

    // Show loading message
    showMessage("Loading appointments...", "Please wait while appointments are retrieved.");

    try {

        // Retrieve appointments from the backend
        const response = await fetch(
            `/api/staff/appointments?date=${encodeURIComponent(selectedDate)}`,
            {
                method: "GET",
                headers: {
                    "Accept": "application/json"
                },
                credentials: "same-origin"
            }
        );

        // Check if the request was successful
        if (!response.ok) {
            throw new Error("Unable to retrieve appointments. Status: " + response.status);
        }

        // Check if the backend returned JSON
        const contentType = response.headers.get("content-type") || "";

        if (!contentType.includes("application/json")) {
            throw new Error("Please sign in again to access appointments.");
        }

        // Convert the response into JavaScript data
        const appointments = await response.json();

        if (!Array.isArray(appointments)) {
            throw new Error("Invalid appointment data received.");
        }

        // Sort appointments by start time
        const sortedAppointments = [...appointments].sort(
            (a, b) => a.startTime.localeCompare(b.startTime)
        );

        // Update the appointment count
        document.getElementById("appointment-count").textContent =
            sortedAppointments.length;

        // Show a message if there are no appointments
        if (sortedAppointments.length === 0) {
            showMessage(
                "No appointments today",
                "There are currently no appointments scheduled for this date."
            );
            return;
        }

        // Display the appointment table
        table.hidden = false;
        emptyState.hidden = true;

        // Display appointments in the table
        sortedAppointments.forEach(appointment => {

            const row = document.createElement("tr");

            const timeCell = document.createElement("td");
            timeCell.textContent =
                formatTime(appointment.startTime) + " - " +
                formatTime(appointment.endTime);

            // Display the customer name or customer ID if the name is unavailable
            const customerCell = document.createElement("td");
            customerCell.textContent = appointment.customerName ||
                `Customer #${appointment.customerId}`;

            const serviceCell = document.createElement("td");
            serviceCell.textContent = appointment.serviceName ||
                `Service #${appointment.serviceId}`;

            const statusCell = document.createElement("td");
            const statusBadge = document.createElement("span");

            statusBadge.className = "status";
            statusBadge.textContent = appointment.status || "UNKNOWN";

            statusCell.appendChild(statusBadge);

            row.appendChild(timeCell);
            row.appendChild(customerCell);
            row.appendChild(serviceCell);
            row.appendChild(statusCell);

            appointmentRows.appendChild(row);
        });

    } catch (error) {

        // Display an error if appointments cannot be retrieved
        console.error("Error loading appointments:", error);

        document.getElementById("appointment-count").textContent = "0";

        showMessage(
            "Unable to load appointments",
            error.message
        );
    }
}

// Update appointments whenever the selected date changes
dateInput.addEventListener("change", displayAppointments);

// Load appointments when the dashboard opens
displayAppointments();
