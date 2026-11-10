const appointmentsList = document.getElementById("appointmentsList");
const appointmentsMessage = document.getElementById("appointmentsMessage");

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

    const appointments = await response.json();

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
        emptyMessage.textContent =
            "You don't have any appointments yet. Book an appointment to get started.";

        appointmentsList.appendChild(emptyMessage);
        return;
    }

    appointments.forEach(appointment => {
        const card = document.createElement("article");
        card.className = "appointment-card";

        const header = document.createElement("div");
        header.className = "appointment-card-header";

        const serviceName = document.createElement("h3");
        serviceName.textContent =
            serviceNames.get(String(appointment.serviceId)) ||
            `Pharmacy Service #${appointment.serviceId}`;

        const status = String(appointment.status || "").toUpperCase();

        const statusBadge = document.createElement("span");
        statusBadge.className = "appointment-status";

        if (status === "BOOKED") {
            statusBadge.classList.add("status-booked");
        } else if (status === "CANCELLED") {
            statusBadge.classList.add("status-cancelled");
        } else {
            statusBadge.classList.add("status-other");
        }

        statusBadge.textContent = status;

        header.append(serviceName, statusBadge);

        const details = document.createElement("div");
        details.className = "appointment-details";

        const date = document.createElement("p");
        date.textContent =
            `Date: ${formatAppointmentDate(appointment.appointmentDate)}`;

        const time = document.createElement("p");
        time.textContent =
            `Time: ${formatAppointmentTime(appointment.startTime)} - ${formatAppointmentTime(appointment.endTime)}`;

        const confirmation = document.createElement("p");
        confirmation.textContent =
            `Confirmation #${appointment.appointmentId}`;

        details.append(date, time, confirmation);

        card.append(header, details);

        if (status === "BOOKED" && isFutureAppointment(appointment)) {
            const actions = document.createElement("div");
            actions.className = "appointment-actions";

            const cancelButton = document.createElement("button");
            cancelButton.type = "button";
            cancelButton.className = "cancel-appointment-button";
            cancelButton.textContent = "Cancel Appointment";

            cancelButton.addEventListener("click", () => {
                cancelAppointment(appointment.appointmentId, cancelButton);
            });

            actions.appendChild(cancelButton);
            card.appendChild(actions);
        }

        appointmentsList.appendChild(card);
    });
}

async function cancelAppointment(appointmentId, button) {

    const confirmed = window.confirm(
        "Are you sure you want to cancel this appointment? This action cannot be undone."
    );

    if (!confirmed) {
        return;
    }

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

            showAppointmentMessage(
                `Appointment #${appointmentId} was cancelled successfully!`
            );
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
