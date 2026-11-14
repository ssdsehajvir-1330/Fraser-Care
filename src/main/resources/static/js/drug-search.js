const form = document.getElementById("drug-search-form");
const typeEl = document.getElementById("search-type");
const queryEl = document.getElementById("search-query");
const statusEl = document.getElementById("status");
const table = document.getElementById("results");
const tbody = table.querySelector("tbody");

typeEl.addEventListener("change", () => {
    queryEl.placeholder = typeEl.value === "din" ? "e.g. 01933531" : "e.g. Advil";
});

form.addEventListener("submit", async e => {
    e.preventDefault();
    const params = new URLSearchParams({ [typeEl.value]: queryEl.value.trim() });
    statusEl.textContent = "Searching...";
    table.hidden = true;

    try {
        const res = await fetch("/api/drugs?" + params);
        if (!res.ok) {
            statusEl.textContent = await res.text();
            return;
        }
        showResults(await res.json());
    } catch {
        statusEl.textContent = "Could not reach FraserCare. Check your connection and try again.";
    }
});

function showResults(products) {
    tbody.replaceChildren();
    if (!products.length) {
        statusEl.textContent = "No products found.";
        return;
    }
    products.forEach(p => {
        const row = tbody.insertRow();
        [p.brandName, p.din, p.company, p.productClass].forEach(value => {
            row.insertCell().textContent = value;
        });
        const button = document.createElement("button");
        button.type = "button";
        button.className = "details-button";
        button.textContent = "Details";
        button.setAttribute("aria-expanded", "false");
        button.addEventListener("click", () => toggleDetails(row, button, p.drugCode));
        row.insertCell().appendChild(button);
    });
    statusEl.textContent = products.length + " product(s) found.";
    table.hidden = false;
}

async function toggleDetails(row, button, drugCode) {
    const next = row.nextElementSibling;
    if (next?.classList.contains("details-row")) {
        next.remove();
        button.setAttribute("aria-expanded", "false");
        return;
    }
    const cell = tbody.insertRow(row.sectionRowIndex + 1).insertCell();
    cell.parentElement.className = "details-row";
    cell.colSpan = row.cells.length;
    cell.textContent = "Loading details...";
    button.setAttribute("aria-expanded", "true");

    try {
        const res = await fetch("/api/drugs/" + drugCode);
        if (!res.ok) {
            cell.textContent = await res.text();
            return;
        }
        cell.replaceChildren(renderDetails(await res.json()));
    } catch {
        cell.textContent = "Could not load details. Please try again.";
    }
}

function renderDetails(d) {
    const list = document.createElement("dl");
    const ingredients = d.activeIngredients.map(i => i.strength ? `${i.name} ${i.strength}` : i.name);
    [
        ["Active ingredients", ingredients],
        ["Form", d.dosageForms],
        ["Route", d.routes],
        ["Schedule", d.schedules],
        ["Status", d.status ? [d.status] : []]
    ].forEach(([label, values]) => {
        const term = document.createElement("dt");
        term.textContent = label;
        const desc = document.createElement("dd");
        desc.textContent = values.length ? values.join(", ") : "Not listed";
        list.append(term, desc);
    });
    return list;
}
