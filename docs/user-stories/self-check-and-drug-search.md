# User Stories: Self-Check and Drug Product Search

Owner: Hassaan Nasim

Story points use the Fibonacci scale (1, 2, 3, 5, 8).

## Iteration 1

### SC-1: Take the self-check (5 pts)
**As a** customer, **I want to** answer a few quick questions about why I'm visiting the pharmacy **so that** I know which service to book.

Acceptance criteria:
- Given I am logged in, when I open the self-check page, then I see the questions loaded from the server.
- Given I answer every question and submit, then I see a list of suggested services.
- Given I submit without answering a question, then I am asked to answer it.

### SC-2: Emergency screening (2 pts)
**As a** customer with emergency symptoms, **I want to** be told right away to get emergency help **so that** I don't wait for a pharmacy appointment.

Acceptance criteria:
- Given I answer "Yes" to the emergency question, then the other questions are disabled.
- When I submit, I see a message telling me to call 9-1-1, and no services are suggested.

### SC-3: Non-diagnostic disclaimer (1 pt)
**As a** pharmacy, **we want** every self-check result to say it is not a medical assessment **so that** customers don't treat it as a diagnosis.

Acceptance criteria:
- Every result, urgent or not, shows the disclaimer and the HealthLink BC 8-1-1 number.

### DS-1: Research the drug database API (3 pts)
**As a** developer, **I want to** know how Health Canada's Drug Product Database API works **so that** the team can decide how to integrate it.

Acceptance criteria:
- Research notes in `docs/research/health-canada-dpd-api.md` cover the endpoints, response format, quirks, and the integration approach.

### DS-2: Search drug products by brand name (5 pts)
**As a** customer, **I want to** search Canadian drug products by brand name **so that** I can see the official product names and DINs.

Acceptance criteria:
- Given I search "advil", then I see matching products with brand name, DIN, and company.
- Given I search fewer than 3 characters, then I see a validation message.
- Given Health Canada's API is down, then I see a friendly error instead of a crash.

### DS-3: Look up a product by DIN (3 pts)
**As a** customer, **I want to** look up a product using the DIN on my medication box **so that** I can confirm what it is.

Acceptance criteria:
- Given I enter a valid DIN, then I see that product.
- Given I enter a DIN without its leading zeros, then it still works.
- Given no product matches, then I see "No products found".

## Iteration 3

### DS-5: Marketed vs cancelled status (3 pts)
**As a** customer, **I want to** see whether a product is still sold in Canada, with cancelled products hidden by default, **so that** I only see products I can actually buy.

Acceptance criteria:
- Given I search by brand name, then I only see products Health Canada lists as marketed, each labelled "Marketed".
- Given I tick "Show cancelled products", then I also see products that are cancelled, dormant, or not yet marketed, each with its status, listed after the marketed ones.
- Given I look up a DIN, then I see the product and its status even if it has been cancelled.

### DS-6: Cache Health Canada responses (2 pts)
**As a** pharmacy, **we want** repeat drug searches to reuse earlier Health Canada responses **so that** the page is faster and we don't overload a free public API.

Acceptance criteria:
- Given the same search or product lookup is made twice, then Health Canada is only called once.
- Given a call to Health Canada fails, then the failure isn't cached and the next request tries again.
- Cached responses expire after 12 hours so data stays current.

## Backlog

- **SC-4:** Book a suggested service directly from the self-check result. (3 pts; depends on booking)
- **SC-5:** Only suggest flu shots during flu season. (1 pt)
- **SC-6:** Staff can see anonymized self-check trends. (5 pts; needs a group decision on storage)
- **DS-4:** View a product's active ingredients and strength. (3 pts)
