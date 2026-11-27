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

function isPastStaffAppointment(appointment) {
    const parts = new Intl.DateTimeFormat("en-CA", {
        timeZone: "America/Vancouver",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hourCycle: "h23"
    }).formatToParts(new Date());

    const values = {};

    parts.forEach(part => {
        values[part.type] = part.value;
    });

    const currentDate = `${values.year}-${values.month}-${values.day}`;
    const currentTime = `${values.hour}:${values.minute}:${values.second}`;
    const endTime = (appointment.endTime + ":00").substring(0, 8);

    return appointment.appointmentDate < currentDate ||
        (appointment.appointmentDate === currentDate &&
         endTime <= currentTime);
}

function showStaffAppointmentFeedback(message, isError = false) {
    const feedback = document.getElementById("feedback-message");

    feedback.textContent = message;
    feedback.className = isError
        ? "feedback-message error"
        : "feedback-message";

    feedback.hidden = false;
}

async function updateStaffAppointmentStatus(appointmentId, status, actionCell) {
    const label = status === "COMPLETED" ? "completed" : "no-show";

    if (!window.confirm(`Mark this appointment as ${label}?`)) {
        return;
    }

    const buttons = actionCell.querySelectorAll("button");

    buttons.forEach(button => {
        button.disabled = true;
    });

    try {
        const csrfResponse = await fetch("/csrf", {
            credentials: "same-origin",
            headers: {
                Accept: "application/json"
            }
        });

        if (!csrfResponse.ok || csrfResponse.redirected ||
            !csrfResponse.headers.get("content-type")?.includes("application/json")) {
            throw new Error("Unable to verify your session. Please log in again.");
        }

        const csrf = await csrfResponse.json();

        const response = await fetch(
            `/api/staff/appointments/${appointmentId}/status`,
            {
                method: "PATCH",
                credentials: "same-origin",
                headers: {
                    Accept: "application/json",
                    "Content-Type": "application/json",
                    [csrf.headerName]: csrf.token
                },
                body: JSON.stringify({ status })
            }
        );

        if (response.status === 401 || response.redirected) {
            throw new Error("Please log in again.");
        }

        if (!response.ok) {
            const message = await response.text();

            throw new Error(message || "Unable to update appointment status.");
        }

        const refreshed = await displayAppointments();

        if (refreshed) {
            showStaffAppointmentFeedback(
                `Appointment marked as ${label} successfully.`
            );
        } else {
            showStaffAppointmentFeedback(
                "Appointment updated, but the table could not refresh. Please reload the page.",
                true
            );
        }

    } catch (error) {
        showStaffAppointmentFeedback(error.message, true);

    } finally {
        buttons.forEach(button => {
            button.disabled = false;
        });
    }
}

// Display appointments for the selected date
async function displayAppointments() {

    const selectedDate = dateInput.value;

    const table = document.getElementById("table-container");
    const emptyState = document.getElementById("empty-state");

    const appointmentRows =
        document.getElementById("appointment-rows");

    document.getElementById("feedback-message").hidden = true;

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
        return true;
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
            return true;
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

            statusBadge.textContent =
                appointment.status === "COMPLETED" ? "Completed" :
                appointment.status === "NO_SHOW" ? "No-show" :
                appointment.status || "UNKNOWN";

            statusCell.appendChild(statusBadge);

            row.appendChild(timeCell);
            row.appendChild(customerCell);
            row.appendChild(serviceCell);
            row.appendChild(statusCell);

            const actionCell = document.createElement("td");

            if (appointment.status === "BOOKED" &&
                isPastStaffAppointment(appointment)) {

                const completedButton = document.createElement("button");
                completedButton.type = "button";
                completedButton.className = "pickup-action";
                completedButton.textContent = "Mark Completed";

                completedButton.addEventListener("click", () => {
                    updateStaffAppointmentStatus(
                        appointment.appointmentId,
                        "COMPLETED",
                        actionCell
                    );
                });

                const noShowButton = document.createElement("button");
                noShowButton.type = "button";
                noShowButton.className = "pickup-action secondary";
                noShowButton.textContent = "Mark No-show";

                noShowButton.addEventListener("click", () => {
                    updateStaffAppointmentStatus(
                        appointment.appointmentId,
                        "NO_SHOW",
                        actionCell
                    );
                });

                actionCell.append(
                    completedButton,
                    document.createTextNode(" "),
                    noShowButton
                );

            } else {
                actionCell.textContent = "-";
            }

            row.appendChild(actionCell);

            appointmentRows.appendChild(row);
        });

        return true;

    } catch (error) {

        // Display an error if appointments cannot be retrieved
        console.error("Error loading appointments:", error);

        document.getElementById("appointment-count").textContent = "0";

        showMessage(
            "Unable to load appointments",
            error.message
        );

        return false;
    }
}

// Update appointments whenever the selected date changes
dateInput.addEventListener("change", displayAppointments);

// Load appointments when the dashboard opens
displayAppointments();
