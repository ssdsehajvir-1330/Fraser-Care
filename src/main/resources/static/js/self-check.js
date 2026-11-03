const form = document.getElementById("self-check-form");
const questionsEl = document.getElementById("questions");
const resultEl = document.getElementById("result");

async function loadQuestions() {
    const res = await fetch("/api/self-check/questions");
    if (!res.ok) {
        questionsEl.textContent = "Could not load the questionnaire. Please try again later.";
        return;
    }
    (await res.json()).forEach(renderQuestion);
}

function renderQuestion(question) {
    const fieldset = document.createElement("fieldset");
    fieldset.dataset.id = question.id;
    const legend = document.createElement("legend");
    legend.textContent = question.text;
    fieldset.appendChild(legend);

    question.options.forEach(option => {
        const label = document.createElement("label");
        const input = document.createElement("input");
        input.type = "radio";
        input.name = question.id;
        input.value = option.value;
        input.required = true;
        label.append(input, " " + option.label);
        fieldset.appendChild(label);
    });
    questionsEl.appendChild(fieldset);
}

questionsEl.addEventListener("change", e => {
    if (e.target.name !== "urgent") return;
    const skip = e.target.value === "yes";
    questionsEl.querySelectorAll("fieldset:not([data-id='urgent'])").forEach(fs => {
        fs.disabled = skip;
        fs.classList.toggle("skipped", skip);
    });
});

form.addEventListener("submit", async e => {
    e.preventDefault();
    const res = await fetch("/api/self-check", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ answers: Object.fromEntries(new FormData(form)) })
    });
    if (!res.ok) {
        showError(await res.text());
        return;
    }
    showResult(await res.json());
});

function showResult(result) {
    resultEl.replaceChildren();
    resultEl.className = result.urgent ? "urgent" : "";

    const message = document.createElement("p");
    message.className = "message";
    message.textContent = result.message;
    resultEl.appendChild(message);

    if (result.recommendedServices.length) {
        const list = document.createElement("ul");
        result.recommendedServices.forEach(service => {
            const item = document.createElement("li");
            const book = document.createElement("button");
            book.textContent = "Book";
            book.disabled = true;
            book.title = "Booking from the self-check is coming in Iteration 2";
            item.append(service, " ", book);
            list.appendChild(item);
        });
        resultEl.appendChild(list);
    }

    const disclaimer = document.createElement("p");
    disclaimer.className = "disclaimer";
    disclaimer.textContent = result.disclaimer;
    resultEl.appendChild(disclaimer);

    resultEl.hidden = false;
    resultEl.scrollIntoView({ behavior: "smooth" });
}

function showError(text) {
    resultEl.className = "error";
    resultEl.textContent = text || "Something went wrong. Please try again.";
    resultEl.hidden = false;
}

loadQuestions();
