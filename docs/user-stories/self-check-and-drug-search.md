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

## Backlog (Iteration 2+)

- **SC-4:** Book a suggested service directly from the self-check result. (3 pts; depends on booking)
- **SC-5:** Only suggest flu shots during flu season. (1 pt)
- **SC-6:** Staff can see anonymized self-check trends. (5 pts; needs a group decision on storage)
- **DS-4:** View a product's active ingredients and strength. (3 pts)
- **DS-5:** Show whether a product is marketed or cancelled, and hide cancelled products by default. (3 pts)
- **DS-6:** Cache API responses to reduce calls to Health Canada. (2 pts)
