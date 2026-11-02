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
    });
    statusEl.textContent = products.length + " product(s) found.";
    table.hidden = false;
}
