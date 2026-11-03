const serviceSelect = document.getElementById("serviceSelect");
const appointmentDate = document.getElementById("appointmentDate");
const serviceDescription = document.getElementById("serviceDescription");
const timeSlots = document.getElementById("timeSlots");
const bookingForm = document.getElementById("bookingForm");
const bookingMessage = document.getElementById("bookingMessage");
const bookButton = document.getElementById("bookButton");

const summaryService = document.getElementById("summaryService");
const summaryDate = document.getElementById("summaryDate");
const summaryTime = document.getElementById("summaryTime");

let services = [];
let selectedService = null;
let selectedTime = null;

function getTodayDate() {
    const parts = new Intl.DateTimeFormat("en-CA", {
        timeZone: "America/Vancouver",
        year: "numeric",
        month: "2-digit",
        day: "2-digit"
    }).formatToParts(new Date());

    const year = parts.find(part => part.type === "year").value;
    const month = parts.find(part => part.type === "month").value;
    const day = parts.find(part => part.type === "day").value;

    return `${year}-${month}-${day}`;
}

function showMessage(message, isError = false) {
    bookingMessage.textContent = message;
    bookingMessage.style.color = isError ? "#b42318" : "#176b52";
}

function updateSummary() {
    summaryService.textContent = selectedService
        ? selectedService.name
        : "Not selected";

    summaryDate.textContent = appointmentDate.value
        ? new Date(appointmentDate.value + "T12:00:00")
            .toLocaleDateString("en-CA")
        : "Not selected";

    summaryTime.textContent = selectedTime && selectedService
        ? `${formatTime(selectedTime)} - ${formatTime(
            calculateEndTime(selectedTime, selectedService.durationMinutes)
        )}`
        : "Not selected";

    bookButton.disabled = !selectedService ||
        !appointmentDate.value ||
        !selectedTime;
}

function formatTime(time) {
    const parts = time.split(":");
    let hours = Number(parts[0]);
    const minutes = parts[1];

    const period = hours >= 12 ? "PM" : "AM";
    hours = hours % 12 || 12;

    return `${hours}:${minutes} ${period}`;
}

function calculateEndTime(startTime, durationMinutes) {
    const [hours, minutes] = startTime.split(":").map(Number);

    const totalMinutes = hours * 60 + minutes + durationMinutes;

    const endHours = Math.floor(totalMinutes / 60);
    const endMinutes = totalMinutes % 60;

    return `${String(endHours).padStart(2, "0")}:${String(endMinutes).padStart(2, "0")}:00`;
}

function showPlaceholder(message) {
    timeSlots.replaceChildren();

    const paragraph = document.createElement("p");
    paragraph.textContent = message;

    timeSlots.appendChild(paragraph);
}

async function loadServices() {
    try {
        const response = await fetch("/api/pharmacy-services", {
            headers: {
                Accept: "application/json"
            }
        });

        if (!response.ok) {
            throw new Error("Unable to retrieve pharmacy services.");
        }

        if (!response.headers.get("content-type")?.includes("application/json")) {
            throw new Error("API authentication needs to be configured before services can load.");
        }

        services = await response.json();

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

        if (services.length === 0) {
            showMessage("No pharmacy services are currently available.", true);
        }

    } catch (error) {
        showMessage(error.message, true);
    }
}

async function loadAvailableTimes() {
    selectedTime = null;
    updateSummary();

    if (!selectedService || !appointmentDate.value) {
        showPlaceholder("Select a service and date first.");
        return;
    }

    showPlaceholder("Loading available times...");
    showMessage("");

    try {
        const params = new URLSearchParams({
            serviceId: selectedService.serviceId,
            date: appointmentDate.value
        });

        const response = await fetch(
            `/api/appointments/availability?${params.toString()}`,
            {
                headers: {
                    Accept: "application/json"
                }
            }
        );

        if (!response.ok) {
            throw new Error("Unable to retrieve available times.");
        }

        if (!response.headers.get("content-type")?.includes("application/json")) {
            throw new Error("API authentication needs to be configured before availability can load.");
        }

        const availableTimes = await response.json();

        timeSlots.replaceChildren();

        if (availableTimes.length === 0) {
            showPlaceholder("No available appointments for this date.");
            return;
        }

        availableTimes.forEach(time => {
            const button = document.createElement("button");

            button.type = "button";
            button.textContent = formatTime(time);

            button.addEventListener("click", () => {
                selectedTime = time;

                document.querySelectorAll(".time-slots button")
                    .forEach(slot => slot.classList.remove("selected"));

                button.classList.add("selected");

                updateSummary();
            });

            timeSlots.appendChild(button);
        });

    } catch (error) {
        showPlaceholder(error.message);
    }
}

serviceSelect.addEventListener("change", () => {
    selectedService = services.find(
        service => String(service.serviceId) === serviceSelect.value
    );

    selectedTime = null;

    serviceDescription.textContent = selectedService
        ? `${selectedService.description || "No description available."} Duration: ${selectedService.durationMinutes} minutes.`
        : "Choose a service to see its details.";

    updateSummary();
    loadAvailableTimes();
});

appointmentDate.addEventListener("change", () => {
    updateSummary();
    loadAvailableTimes();
});


bookingForm.addEventListener("submit", async event => {
    event.preventDefault();

    if (!selectedService || !appointmentDate.value || !selectedTime) {
        showMessage("Please complete all appointment selections.", true);
        return;
    }

    const appointmentDetails = {
        serviceId: selectedService.serviceId,
        appointmentDate: appointmentDate.value,
        startTime: selectedTime,
        endTime: calculateEndTime(
            selectedTime,
            selectedService.durationMinutes
        )
    };

    bookButton.disabled = true;
    showMessage("Booking your appointment...");

    try {
        const csrfResponse = await fetch("/csrf");

        if (!csrfResponse.ok) {
            throw new Error("Unable to verify your session. Please log in again.");
        }

        const csrf = await csrfResponse.json();

        const response = await fetch("/api/appointments", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json",
                [csrf.headerName]: csrf.token
            },
            body: JSON.stringify(appointmentDetails)
        });

        if (response.status === 401 || response.redirected) {
            throw new Error("Please log in as a customer before booking.");
        }

        if (!response.ok) {
            const errorMessage = await response.text();
            throw new Error(errorMessage || "Unable to book appointment.");
        }

        const bookedAppointment = await response.json();

        await loadAvailableTimes();

        showMessage(
        `Appointment booked successfully! Confirmation #${bookedAppointment.appointmentId}`
    );

    } catch (error) {
        showMessage(error.message, true);
    } finally {
        updateSummary();
    }
});


appointmentDate.min = getTodayDate();

updateSummary();
loadServices();