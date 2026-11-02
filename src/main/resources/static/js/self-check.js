const form = document.getElementById("self-check-form");
const questionsEl = document.getElementById("questions");

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

form.addEventListener("submit", e => {
    e.preventDefault();
    const answers = Object.fromEntries(new FormData(form));
    // TODO: POST answers once the recommendation endpoint exists
    console.log(answers);
});

loadQuestions();
