// Sample appointments for testing
const today = new Date();

const todayISO = [
    today.getFullYear(),
    String(today.getMonth() + 1).padStart(2, "0"),
    String(today.getDate()).padStart(2, "0")
].join("-");

const appointments = [
    {
        appointmentId: 1,
        customerId: 101,
        customerName: "Sarah Lee",
        serviceId: 1,
        serviceName: "Flu Vaccination",
        appointmentDate: todayISO,
        startTime: "09:00:00",
        endTime: "09:30:00",
        status: "BOOKED"
    },
    {
        appointmentId: 2,
        customerId: 102,
        customerName: "James Wilson",
        serviceId: 2,
        serviceName: "Medication Review",
        appointmentDate: todayISO,
        startTime: "10:15:00",
        endTime: "10:45:00",
        status: "BOOKED"
    },
    {
        appointmentId: 3,
        customerId: 103,
        customerName: "Emily Brown",
        serviceId: 3,
        serviceName: "Blood Pressure Check",
        appointmentDate: todayISO,
        startTime: "11:30:00",
        endTime: "11:45:00",
        status: "BOOKED"
    }
];

const dateInput = document.getElementById("appointment-date");

// Set today's date as the default selected date
dateInput.value = todayISO;

// Convert time into a readable format
function formatTime(time) {
    const [hours, minutes] = time.split(":");
    const date = new Date();

    date.setHours(Number(hours), Number(minutes));

    return date.toLocaleTimeString([], {
        hour: "numeric",
        minute: "2-digit"
    });
}

// Display appointments for the selected date
function displayAppointments() {

    const selectedDate = dateInput.value;

    const table = document.getElementById("table-container");
    const emptyState = document.getElementById("empty-state");

    const appointmentRows =
        document.getElementById("appointment-rows");

    // Filter appointments by the selected date
    const selectedAppointments = appointments.filter(
        appointment =>
            appointment.appointmentDate === selectedDate
    );

    // Sort appointments by start time
    selectedAppointments.sort((a, b) =>
        a.startTime.localeCompare(b.startTime)
    );

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

    // Update the appointment count
    document.getElementById("appointment-count").textContent =
        selectedAppointments.length;

    appointmentRows.innerHTML = "";

    // Show a message if there are no appointments
    if (selectedAppointments.length === 0) {
        table.hidden = true;
        emptyState.hidden = false;
        return;
    }

    table.hidden = false;
    emptyState.hidden = true;

    // Display appointments in the table
    selectedAppointments.forEach(appointment => {

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
        serviceCell.textContent = appointment.serviceName;

        const statusCell = document.createElement("td");
        const statusBadge = document.createElement("span");

        statusBadge.className = "status";
        statusBadge.textContent = appointment.status;

        statusCell.appendChild(statusBadge);

        row.appendChild(timeCell);
        row.appendChild(customerCell);
        row.appendChild(serviceCell);
        row.appendChild(statusCell);

        appointmentRows.appendChild(row);
    });
}

// Update appointments whenever the selected date changes
dateInput.addEventListener("change", displayAppointments);

// Load the appointments
displayAppointments();
