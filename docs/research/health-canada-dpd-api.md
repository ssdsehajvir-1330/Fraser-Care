# Research: Health Canada Drug Product Database (DPD) API

Owner: Hassaan Nasim
Status: Iteration 1 research spike

## Why this API

The proposal commits to "integrate Health Canada's Drug Product Database REST API, allowing users to search for publicly available drug-product information, including brand names and Drug Identification Numbers."

Before writing code I checked whether the API is usable for us:

- Free and public, with **no API key** needed.
- Returns JSON.
- Canadian data, which fits a B.C. pharmacy app better than US sources.
- Covers about 47,000 products (human, veterinary, disinfectant, and Schedule C) [1].

### Alternatives considered

| Option | Why not (for now) |
|--------|-------------------|
| DPD data extract (bulk zip files) [2] | Would need importing into Postgres and refreshing ourselves. More work, and the data goes stale. Could work as a fallback if the API is unreliable. |
| openFDA drug API | US products only. Wrong country, and US NDC codes don't match Canadian DINs. |
| NLM RxNorm | Also US-focused. |

**Decision:** use the DPD REST API, called from our Spring Boot backend.

## API summary

Base URL: `https://health-products.canada.ca/api/drug/`

Common parameters: `lang=en|fr`, `type=json|xml` [1].

| Endpoint | Useful params | What we'd use it for |
|----------|---------------|----------------------|
| `drugproduct/` | `brandname`, `din`, `id`, `status` | Main search (brand name / DIN) |
| `activeingredient/` | `id` (drug code), `ingredientname` | Ingredient + strength on a details view |
| `status/` | `id` | Marketed vs. cancelled |
| `form/` | `id` | Tablet, capsule, etc. |
| `route/` | `id` | Oral, topical, etc. |
| `schedule/` | `id` | Prescription vs. OTC |
| `company/` | `id` (company code) | Probably not needed |

`drugproduct` response fields: `drug_code`, `drug_identification_number`, `brand_name`, `descriptor`, `class_name`, `number_of_ais`, `ai_group_no`, `company_name`, `last_update_date`.

Status codes for the `status` filter: 1 Approved, 2 Marketed, 3 Cancelled Pre Market, 4 Cancelled Post Market, 6 Dormant, 9 Cancelled (Unreturned Annual), 10 Cancelled (Safety Issue), plus a few interim-order codes (11–15) [1].

## What I found testing it

I called the endpoints directly on Nov 1, 2026.

1. **The response shape depends on the parameter.**
   - `drugproduct/?brandname=advil` returns a **JSON array** (48 results).
   - `drugproduct/?din=...` returns an **array**.
   - `drugproduct/?id=2049` returns a **single object**, not an array.
   - `status/?id=13452` also returns a single object.
   - `activeingredient/?id=2049` returns an array, even with one ingredient.

   So the Java client can't use one response type for every endpoint.

2. **No match gives `[]`**, not a 404 (tested `din=99999999`). We only need to handle the empty list.

3. **Brand name search is a partial, case-insensitive match.** "advil" matches "ADVIL CAPLETS", "ADVIL TABLETS", and so on. Results come back in uppercase.

4. **Search results include cancelled and dormant products** unless you filter. Customers probably only care about marketed ones, so we need `status/` or the `status` filter later.

5. **A DIN is an 8-digit string with leading zeros** (e.g. `01933531`), so it has to stay a `String` in Java, not an `int`. Users might type it without the zeros, so we pad it.

6. **Field types are inconsistent.** `drug_code` is a number, but `number_of_ais` is a string ("1"), and `descriptor` is often `""`.

7. **No documented rate limit or uptime guarantee** [1]. We shouldn't call the API on every keystroke. Search on submit, and add caching later.

8. Company contact information is not included in the API [1], which is fine for us.

9. The official jQuery examples reference `data.panels[0].text`, which doesn't exist in any response, so the examples look outdated. I didn't use them.

## Integration design (proposed)

```
Browser (drug-search.html)
   │  GET /api/drugs?brand=advil
   ▼
DrugSearchController ──► DrugSearchService ──► HealthCanadaDrugClient ──► DPD API
                        (validation, limit)    (RestClient, raw DTOs)
```

Why call it from the backend instead of from the browser's JavaScript:
- We can validate input and cap the result count in one place.
- We control the JSON shape the frontend sees. Our `DrugProductSummary` uses camelCase and drops fields we don't use, so the frontend doesn't depend on Health Canada's format.
- We can add caching and status filtering later without touching the frontend.
- CORS isn't documented for this API, so browser calls might be blocked anyway.

HTTP client: Spring's `RestClient` (already included with `spring-boot-starter-webmvc`, so no new dependency). The base URL can be overridden with the `dpd.api.base-url` property.

Error handling: if Health Canada is down or times out, our endpoint returns **502** with a friendly message instead of a 500 stack trace.

## Licence / terms

The DPD dataset is published on the Open Government Portal [3]. Health Canada's DPD pages point to the "Drug Product Database Terms and Conditions". We only display public product info and add a note that it comes from Health Canada. **TODO:** read the full terms and confirm the attribution wording.

## Plan

**Iteration 1 (this PR)**
- [x] Research spike (this document)
- [ ] Java client for brand name search and DIN lookup
- [ ] `GET /api/drugs?brand=` and `GET /api/drugs?din=`
- [ ] Basic search page
- [ ] Unit tests with mocked HTTP

**Iteration 2–3**
- [ ] Product details view: active ingredients + strength (`activeingredient/`)
- [ ] Show marketed/cancelled status and hide cancelled products by default
- [ ] Cache responses (Spring `@Cacheable`)
- [ ] French support (`lang=fr`)
- [ ] Link from the home page / nav
- [ ] Maybe link to self-check results (e.g. "Medication Review → look up your meds")

## References

1. Health Canada, "Drug Product Database API – Documentation." https://health-products.canada.ca/api/documentation/dpd-documentation-en.html
2. Health Canada, "Drug Product Database: What is the data extract." https://www.canada.ca/en/health-canada/services/drugs-health-products/drug-products/drug-product-database/what-data-extract-drug-product-database.html
3. Open Government Portal, "Drug Product Database – All Files." https://open.canada.ca/data/en/dataset/bf55e42a-63cb-4556-bfd8-44f26e5a36fe
