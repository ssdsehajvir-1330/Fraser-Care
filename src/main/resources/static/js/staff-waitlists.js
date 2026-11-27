(() => {
    "use strict";

    // Staff waitlist section elements
    const waitlistDate = document.getElementById("staff-waitlist-date");
    const refreshButton = document.getElementById("staff-waitlist-refresh");
    const waitlistTable = document.getElementById("staff-waitlist-table");
    const waitlistRows = document.getElementById("staff-waitlist-rows");
    const waitlistEmpty = document.getElementById("staff-waitlist-empty");
    const waitlistMessage = document.getElementById("staff-waitlist-message");

    // Avoid affecting other dashboard features if this section is missing
    if (!waitlistDate || !refreshButton || !waitlistTable ||
        !waitlistRows || !waitlistEmpty || !waitlistMessage) {
        console.warn("Staff waitlist elements were not found.");
        return;
    }

    let requestVersion = 0;
    let actionInProgress = false;

    // Display upcoming dates with active waitlists above the date picker
    const activeDatesInfo = document.createElement("div");
    activeDatesInfo.id = "staff-waitlist-active-dates";
    activeDatesInfo.style.marginBottom = "8px";
    activeDatesInfo.textContent = "Checking upcoming waitlist dates...";
    waitlistDate.before(activeDatesInfo);

    async function loadActiveWaitlistDates() {
        try {
            const response = await fetch("/api/staff/waitlists/active-dates", {
                method: "GET",
                credentials: "same-origin",
                headers: { Accept: "application/json" }
            });

            if (!response.ok || response.redirected) {
                throw new Error("Unable to retrieve active dates.");
            }

            const dates = await response.json();
            if (!Array.isArray(dates)) {
                throw new Error("Invalid active dates response.");
            }

            activeDatesInfo.textContent = dates.length
                ? "Dates with active waitlists: " + dates.join(", ")
                : "No upcoming active waitlists.";

        } catch (error) {
            console.error("Error loading active waitlist dates:", error);
            activeDatesInfo.textContent = "Unable to load active waitlist dates.";
        }
    }


    // Display a feedback message
    function showWaitlistMessage(message, isError = false) {
        waitlistMessage.textContent = message;
        waitlistMessage.className =
            "feedback-message" + (isError ? " error" : "");
        waitlistMessage.hidden = false;
    }

    function hideWaitlistMessage() {
        waitlistMessage.hidden = true;
        waitlistMessage.textContent = "";
    }

    // Convert 24-hour time to a readable format
    function formatWaitlistTime(timeValue) {
        if (!timeValue) {
            return "N/A";
        }

        const [hours, minutes] = timeValue.split(":").map(Number);

        if (!Number.isFinite(hours) || !Number.isFinite(minutes)) {
            return "N/A";
        }

        const period = hours >= 12 ? "PM" : "AM";
        const displayHours = hours % 12 || 12;

        return `${displayHours}:${String(minutes).padStart(2, "0")} ${period}`;
    }

    function formatQueuePosition(entry) {
        if (entry.status !== "WAITING") {
            return "—";
        }

        if (Number.isInteger(entry.queuePosition) &&
            entry.queuePosition > 0) {
            return `#${entry.queuePosition}`;
        }

        return "Unavailable";
    }

    // Read backend errors whether they are JSON or plain text
    async function getErrorMessage(response, fallback) {
        if (response.redirected || response.status === 401) {
            return "Your session has expired. Please log in again.";
        }

        if (response.status === 403) {
            return "Staff or administrator access is required.";
        }

        try {
            const responseText = (await response.text()).trim();

            if (!responseText) {
                return fallback;
            }

            const contentType =
                response.headers.get("content-type") || "";

            if (contentType.includes("text/html")) {
                return fallback;
            }

            try {
                const data = JSON.parse(responseText);

                if (typeof data === "string") {
                    return data.trim() || fallback;
                }

                if (data && typeof data === "object") {
                    const message =
                        data.message || data.error || data.detail;

                    if (typeof message === "string" && message.trim()) {
                        return message.trim();
                    }
                }

                return fallback;

            } catch (error) {
                return responseText;
            }

        } catch (error) {
            return fallback;
        }
    }

    // Create a table cell containing text
    function makeCell(value) {
        const cell = document.createElement("td");
        cell.textContent = value;
        return cell;
    }

    // Create staff waitlist row
    function makeWaitlistRow(entry) {
        const row = document.createElement("tr");

        const timeCell = makeCell(
            `${formatWaitlistTime(entry.startTime)} - ` +
            `${formatWaitlistTime(entry.endTime)}`
        );

        const customerCell = makeCell(
            entry.customerName ||
            `Customer #${entry.customerId}`
        );

        const serviceCell = makeCell(
            entry.serviceName ||
            `Service #${entry.serviceId}`
        );

        const statusCell = document.createElement("td");
        const statusBadge = document.createElement("span");

        const status =
            String(entry.status || "UNKNOWN").toUpperCase();

        statusBadge.className =
            "status staff-waitlist-status " +
            `staff-waitlist-status-${status.toLowerCase()}`;

        statusBadge.textContent = status;
        statusCell.appendChild(statusBadge);

        const positionCell = makeCell(
            formatQueuePosition(entry)
        );

        const isFirstWaiting =
            status === "WAITING" &&
            entry.queuePosition === 1;

        const readyToNotify =
            isFirstWaiting &&
            entry.readyToNotify === true;

        // Display hybrid notification eligibility
        if (isFirstWaiting) {
            const eligibilityBadge =
                document.createElement("span");

            eligibilityBadge.className =
                "staff-waitlist-status " +
                (readyToNotify
                    ? "staff-waitlist-status-notified"
                    : "staff-waitlist-status-cancelled");

            eligibilityBadge.textContent =
                readyToNotify ? "Ready to Notify" : "Not Ready";

            statusCell.append(
                document.createElement("br"),
                eligibilityBadge
            );

            if (!readyToNotify) {
                const reason = document.createElement("small");

                reason.textContent =
                    entry.notificationReason ||
                    "Eligibility is unavailable.";

                statusCell.append(
                    document.createElement("br"),
                    reason
                );
            }
        }

        // Show the recorded reason for previously skipped customers
        if (status === "SKIPPED" && entry.skipReason) {
            const reason = document.createElement("small");

            reason.textContent =
                `Reason: ${entry.skipReason}`;

            statusCell.append(
                document.createElement("br"),
                reason
            );
        }

        const actionCell = document.createElement("td");

        // Existing staff notification action
        if (readyToNotify) {
            const notifyButton =
                document.createElement("button");

            notifyButton.type = "button";
            notifyButton.className =
                "pickup-action staff-waitlist-notify";

            notifyButton.textContent = "Notify Next";

            notifyButton.addEventListener("click", () => {
                notifyNextCustomer(entry, notifyButton);
            });

            actionCell.appendChild(notifyButton);

        } else if (status === "NOTIFIED") {
            actionCell.appendChild(
                document.createTextNode("Already notified ")
            );

        } else if (status === "WAITING") {
            actionCell.appendChild(
                document.createTextNode(
                    isFirstWaiting ? "Not ready " : "Waiting turn"
                )
            );

        } else {
            actionCell.textContent = "—";
        }

        // Allow skipping only 1st waiting customer or already notified customer
        if (isFirstWaiting || status === "NOTIFIED") {
            const reasonSelect =
                document.createElement("select");

            reasonSelect.setAttribute(
                "aria-label",
                `Reason for skipping ${entry.customerName || "customer"}`
            );

            reasonSelect.append(
                new Option("Skip reason", ""),
                new Option(
                    "Customer declined",
                    "Customer declined"
                ),
                new Option(
                    "Unable to contact",
                    "Unable to contact"
                )
            );

            const skipButton =
                document.createElement("button");

            skipButton.type = "button";
            skipButton.className =
                "pickup-action staff-waitlist-skip";

            skipButton.textContent = "Skip";

            skipButton.addEventListener("click", () => {
                skipWaitlistEntry(
                    entry,
                    reasonSelect,
                    skipButton
                );
            });

            actionCell.append(
                document.createTextNode(" "),
                reasonSelect,
                document.createTextNode(" "),
                skipButton
            );
        }

        row.append(
            timeCell,
            customerCell,
            serviceCell,
            statusCell,
            positionCell,
            actionCell
        );

        return row;
    }

    // Retrieve waitlist entries for the selected date
    async function loadStaffWaitlists(successMessage = "") {
        const selectedDate = waitlistDate.value;
        loadActiveWaitlistDates();
        const currentRequest = ++requestVersion;

        refreshButton.disabled = true;
        waitlistTable.hidden = true;
        waitlistEmpty.hidden = true;
        waitlistRows.replaceChildren();

        if (!selectedDate) {
            showWaitlistMessage(
                "Please select a date to view waitlists.",
                true
            );

            refreshButton.disabled = false;
            return false;
        }

        showWaitlistMessage("Loading waitlists...");

        try {
            const response = await fetch(
                `/api/staff/waitlists?date=${encodeURIComponent(selectedDate)}`,
                {
                    method: "GET",
                    credentials: "same-origin",
                    headers: {
                        Accept: "application/json"
                    }
                }
            );

            if (!response.ok || response.redirected) {
                throw new Error(
                    await getErrorMessage(
                        response,
                        "Unable to retrieve staff waitlists."
                    )
                );
            }

            const contentType =
                response.headers.get("content-type") || "";

            if (!contentType.includes("application/json")) {
                throw new Error(
                    "Unexpected response. Please log in again."
                );
            }

            const entries = await response.json();

            if (!Array.isArray(entries)) {
                throw new Error(
                    "Invalid waitlist information received."
                );
            }

            // Ignore outdated requests
            if (currentRequest !== requestVersion) {
                return false;
            }

            waitlistRows.replaceChildren();

            if (entries.length === 0) {
                waitlistEmpty.hidden = false;
                waitlistTable.hidden = true;

            } else {
                entries.forEach(entry => {
                    waitlistRows.appendChild(
                        makeWaitlistRow(entry)
                    );
                });

                waitlistTable.hidden = false;
                waitlistEmpty.hidden = true;
            }

            if (successMessage) {
                showWaitlistMessage(successMessage);
            } else {
                hideWaitlistMessage();
            }

            return true;

        } catch (error) {
            if (currentRequest !== requestVersion) {
                return false;
            }

            console.error(
                "Error loading staff waitlists:",
                error
            );

            waitlistTable.hidden = true;
            waitlistEmpty.hidden = true;

            showWaitlistMessage(error.message, true);
            return false;

        } finally {
            if (currentRequest === requestVersion) {
                refreshButton.disabled = false;
            }
        }
    }

    async function submitWaitlistAction(
            entry,
            button,
            action,
            reasonSelect = null) {

        if (actionInProgress) {
            return;
        }

        actionInProgress = true;
        const originalText = button.textContent;

        button.disabled = true;
        button.textContent =
            action === "skip" ? "Skipping..." : "Notifying...";

        if (reasonSelect) {
            reasonSelect.disabled = true;
        }

        refreshButton.disabled = true;
        waitlistDate.disabled = true;

        try {
            // Obtain CSRF token
            const csrfResponse = await fetch("/csrf", {
                method: "GET",
                credentials: "same-origin",
                headers: {
                    Accept: "application/json"
                }
            });

            if (!csrfResponse.ok || csrfResponse.redirected) {
                throw new Error(
                    "Unable to verify your session. Please log in again."
                );
            }

            const csrf = await csrfResponse.json();

            if (!csrf.headerName || !csrf.token) {
                throw new Error("CSRF token is unavailable.");
            }

            const headers = {
                Accept: "application/json",
                [csrf.headerName]: csrf.token
            };

            const options = {
                method: "POST",
                credentials: "same-origin",
                headers
            };

            // Skip requests include reason in JSON body
            if (action === "skip") {
                headers["Content-Type"] = "application/json";

                options.body = JSON.stringify({
                    reason: reasonSelect.value
                });
            }

            const response = await fetch(
                `/api/staff/waitlists/${encodeURIComponent(entry.waitlistId)}/${action}`,
                options
            );

            if (!response.ok || response.redirected) {
                throw new Error(
                    await getErrorMessage(
                        response,
                        action === "skip"
                            ? "Unable to skip this customer."
                            : "Unable to notify this customer."
                    )
                );
            }

            // Refresh dashboard after action
            const status =
                action === "skip" ? "SKIPPED" : "NOTIFIED";

            const refreshed = await loadStaffWaitlists(
                `Customer marked as ${status} successfully.`
            );

            if (!refreshed) {
                showWaitlistMessage(
                    `Customer was marked as ${status}, but the waitlist ` +
                    "could not refresh. Please reload the page.",
                    true
                );
            }

        } catch (error) {
            console.error(
                `Error performing waitlist ${action}:`,
                error
            );

            showWaitlistMessage(error.message, true);

        } finally {
            actionInProgress = false;

            button.disabled = false;
            button.textContent = originalText;

            if (reasonSelect) {
                reasonSelect.disabled = false;
            }

            refreshButton.disabled = false;
            waitlistDate.disabled = false;
        }
    }

    // Staff confirm recommended notification
    function notifyNextCustomer(entry, button) {
        if (actionInProgress) {
            return;
        }

        const confirmed = window.confirm(
            `Notify ${entry.customerName || "this customer"} ` +
            "that their appointment slot is available?\n\n" +
            "This changes their waitlist status to NOTIFIED. " +
            "It does not reserve the appointment."
        );

        if (confirmed) {
            submitWaitlistAction(
                entry,
                button,
                "notify"
            );
        }
    }

    // Staff skip customer with selected reason
    function skipWaitlistEntry(entry, reasonSelect, button) {
        if (actionInProgress) {
            return;
        }

        const reason = reasonSelect.value;

        if (!reason) {
            showWaitlistMessage(
                "Please select a skip reason.",
                true
            );

            reasonSelect.focus();
            return;
        }

        const confirmed = window.confirm(
            `Skip ${entry.customerName || "this customer"}?\n\n` +
            `Reason: ${reason}\n\n` +
            "They will be removed from the active waitlist queue."
        );

        if (confirmed) {
            submitWaitlistAction(
                entry,
                button,
                "skip",
                reasonSelect
            );
        }
    }

    // Initialize the date using the appointment dashboard's date
    const appointmentDate =
        document.getElementById("appointment-date");

    if (appointmentDate && appointmentDate.value) {
        waitlistDate.value = appointmentDate.value;

    } else {
        const today = new Date();

        waitlistDate.value = [
            today.getFullYear(),
            String(today.getMonth() + 1).padStart(2, "0"),
            String(today.getDate()).padStart(2, "0")
        ].join("-");
    }

    // Refresh whenever staff select another date
    waitlistDate.addEventListener("change", () => {
        loadStaffWaitlists();
    });

    refreshButton.addEventListener("click", () => {
        loadStaffWaitlists();
    });

    // Load waitlists when the dashboard opens
    loadStaffWaitlists();

})();