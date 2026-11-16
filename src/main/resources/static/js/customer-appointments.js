const appointmentsList = document.getElementById("appointmentsList");
const appointmentsMessage = document.getElementById("appointmentsMessage");

let currentBookingTab = "upcoming";

const upcomingTab = document.getElementById("upcomingTab");
const pastTab = document.getElementById("pastTab");

upcomingTab.addEventListener("click", () => {
    currentBookingTab = "upcoming";
    upcomingTab.classList.add("active");
    pastTab.classList.remove("active");
    loadCustomerAppointments();
});

pastTab.addEventListener("click", () => {
    currentBookingTab = "past";
    pastTab.classList.add("active");
    upcomingTab.classList.remove("active");
    loadCustomerAppointments();
});

function showAppointmentMessage(message, isError = false) {
    appointmentsMessage.textContent = message;
    appointmentsMessage.className = isError
        ? "appointment-message error"
        : "appointment-message success";
}

function formatAppointmentDate(dateValue) {
    const [year, month, day] = dateValue.split("-").map(Number);

    const date = new Date(Date.UTC(year, month - 1, day, 12));

    return new Intl.DateTimeFormat("en-CA", {
        year: "numeric",
        month: "long",
        day: "numeric",
        timeZone: "UTC"
    }).format(date);
}

function formatAppointmentTime(timeValue) {
    const [hours, minutes] = timeValue.split(":").map(Number);

    const period = hours >= 12 ? "PM" : "AM";
    const displayHours = hours % 12 || 12;

    return `${displayHours}:${String(minutes).padStart(2, "0")} ${period}`;
}

function isFutureAppointment(appointment) {
    const parts = new Intl.DateTimeFormat("en-CA", {
        timeZone: "America/Vancouver",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hourCycle: "h23"
    }).formatToParts(new Date());

    const values = {};

    parts.forEach(part => {
        values[part.type] = part.value;
    });

    const today = `${values.year}-${values.month}-${values.day}`;
    const currentTime = `${values.hour}:${values.minute}`;

    const appointmentTime = appointment.startTime.substring(0, 5);

    return appointment.appointmentDate > today ||
        (appointment.appointmentDate === today &&
         appointmentTime > currentTime);
}

async function loadCustomerAppointments() {
    appointmentsList.textContent = "Loading your appointments...";

    const response = await fetch("/api/appointments/mine", {
        credentials: "same-origin",
        headers: {
            Accept: "application/json"
        }
    });

    if (response.status === 401 || response.redirected) {
        throw new Error("Please log in again to view your appointments.");
    }

    if (!response.ok) {
        throw new Error("Unable to retrieve your appointments.");
    }

    if (!response.headers.get("content-type")?.includes("application/json")) {
        throw new Error("Unable to retrieve appointment information.");
    }

    const allAppointments = await response.json();

    const appointments = allAppointments.filter(appointment => {
        const status = String(appointment.status || "").toUpperCase();

        // Cancelled bookings should not appear for customers.
        if (status === "CANCELLED") {
            return false;
        }

        const isUpcoming =
            status === "BOOKED" && isFutureAppointment(appointment);

        return currentBookingTab === "upcoming"
            ? isUpcoming
            : !isUpcoming;
    });

    let services = [];

    try {
        const serviceResponse = await fetch("/api/pharmacy-services", {
            credentials: "same-origin",
            headers: {
                Accept: "application/json"
            }
        });

        if (serviceResponse.ok &&
            serviceResponse.headers.get("content-type")?.includes("application/json")) {
            services = await serviceResponse.json();
        }
    } catch (error) {
        console.error("Could not retrieve pharmacy services:", error);
    }

    const serviceNames = new Map(
        services.map(service => [
            String(service.serviceId),
            service.name
        ])
    );

    appointmentsList.replaceChildren();

    if (appointments.length === 0) {
        const emptyMessage = document.createElement("p");
        emptyMessage.className = "empty-appointments";
        emptyMessage.textContent = currentBookingTab === "upcoming"
            ? "You have no upcoming appointments."
            : "You have no past bookings.";

        appointmentsList.appendChild(emptyMessage);
        return;
    }

    
    const tableWrapper = document.createElement("div");
    tableWrapper.className = "appointment-table-wrapper";

    const table = document.createElement("table");
    table.className = "appointment-table";

    table.innerHTML = `
        <thead>
            <tr>
                <th>Service</th>
                <th>Date</th>
                <th>Time</th>
                <th>Status</th>
                <th>Action</th>
            </tr>
        </thead>
    `;

    const tbody = document.createElement("tbody");

    appointments.forEach(appointment => {
        const row = document.createElement("tr");

        const service = document.createElement("td");
        service.textContent =
            serviceNames.get(String(appointment.serviceId)) ||
            `Pharmacy Service #${appointment.serviceId}`;

        const date = document.createElement("td");
        date.textContent =
            formatAppointmentDate(appointment.appointmentDate);

        const time = document.createElement("td");
        time.textContent =
            `${formatAppointmentTime(appointment.startTime)} - ${formatAppointmentTime(appointment.endTime)}`;

        const statusCell = document.createElement("td");

        const status = String(appointment.status || "").toUpperCase();

        const badge = document.createElement("span");
        badge.className = "appointment-status";

        if (status === "BOOKED") {
            badge.classList.add("status-booked");
        } else if (status === "CANCELLED") {
            badge.classList.add("status-cancelled");
        } else {
            badge.classList.add("status-other");
        }

        badge.textContent = status;
        statusCell.appendChild(badge);

        const actions = document.createElement("td");

        if (status === "BOOKED" && isFutureAppointment(appointment)) {
            const cancelButton = document.createElement("button");
            cancelButton.type = "button";
            cancelButton.className = "cancel-appointment-button";
            cancelButton.textContent = "Cancel";

            cancelButton.addEventListener("click", () => {
            const confirmation = document.createElement("div");
            confirmation.className = "cancel-confirmation";

            const message = document.createElement("span");
            message.textContent = "Sure?";

            const yesButton = document.createElement("button");
            yesButton.type = "button";
            yesButton.className = "confirm-yes";
            yesButton.textContent = "Yes";

            const noButton = document.createElement("button");
            noButton.type = "button";
            noButton.className = "confirm-no";
            noButton.textContent = "No";

            confirmation.append(message, yesButton, noButton);
            actions.replaceChildren(confirmation);

            noButton.addEventListener("click", () => {
                actions.replaceChildren(cancelButton);
            });

    yesButton.addEventListener("click", () => {
        cancelAppointment(appointment.appointmentId, yesButton);
    });
});

            actions.appendChild(cancelButton);
        } else {
            actions.textContent = "—";
        }

        row.append(service, date, time, statusCell, actions);
        tbody.appendChild(row);
    });

    table.appendChild(tbody);
    tableWrapper.appendChild(table);
    appointmentsList.appendChild(tableWrapper);

}

async function cancelAppointment(appointmentId, button) {


    button.disabled = true;
    button.textContent = "Cancelling...";

    try {
        showAppointmentMessage("Cancelling your appointment...");

        const csrfResponse = await fetch("/csrf", {
            credentials: "same-origin",
            headers: {
                Accept: "application/json"
            }
        });

        if (!csrfResponse.ok) {
            throw new Error("Unable to verify your session. Please log in again.");
        }

        const csrf = await csrfResponse.json();

        const response = await fetch(
            `/api/appointments/${appointmentId}/cancel`,
            {
                method: "PATCH",
                credentials: "same-origin",
                headers: {
                    Accept: "application/json",
                    [csrf.headerName]: csrf.token
                }
            }
        );

        if (response.status === 401 || response.redirected) {
            throw new Error("Please log in again to cancel your appointment.");
        }

        if (!response.ok) {
            const errorMessage = await response.text();

            throw new Error(
                errorMessage || "Unable to cancel your appointment."
            );
        }

        try {
            await loadCustomerAppointments();

            showAppointmentMessage(`Appointment cancelled successfully!`);

            setTimeout(() => {
                showAppointmentMessage("");
            }, 2000);

        } catch (refreshError) {
            showAppointmentMessage(
                "Your appointment was cancelled, but the list could not refresh. Please reload the page.",
                true
            );
        }

    } catch (error) {
        showAppointmentMessage(error.message, true);

        button.disabled = false;
        button.textContent = "Cancel Appointment";
    }
}

document.addEventListener("DOMContentLoaded", () => {
    loadCustomerAppointments().catch(error => {
        appointmentsList.textContent = "";
        showAppointmentMessage(error.message, true);
    });
});
