# Customer Self-Check: Questionnaire Design

Owner: Hassaan Nasim
Status: Iteration 2

## Goal

Give customers a short, non-diagnostic questionnaire that points them to pharmacy services they can book in FraserCare. The self-check routes people to a service. It never says what condition someone has, and it never recommends a drug or a dose.

## Constraints

- **Non-diagnostic.** The proposal says the app will not give medical advice or make medical decisions, so every result shows a disclaimer.
- **Safety first.** The first question screens for emergency symptoms. If the customer answers yes, the self-check stops and tells them to call 9-1-1. It does not suggest booking anything.
- **No real health records.** In Iteration 1 the answers are not saved anywhere (see "Storing answers" below).
- **Services must exist in the catalogue.** Recommendations use the service names from the appointment booking catalogue (`pharmacy_services` in Alfonse's schema), so the "Book" button can link straight to booking later.

## Research notes

- In B.C., pharmacists can assess and prescribe for 21 minor ailments plus contraception under the Minor Ailments and Contraception Service (MACS) [1]. Examples: allergic rhinitis, cold sores (herpes labialis), heartburn/GERD, uncomplicated UTI, and acne. That makes "minor ailment consultation" a realistic service for the questionnaire to suggest.
- HealthLink BC (8-1-1) is the provincial non-emergency health line. It is the right place to send people who are unsure, and it fits better than telling them to "see a doctor".
- Pharmacies commonly offer flu shots, medication reviews, and blood pressure checks [2]. These match the three services already seeded in `database/schema.sql`.
- MedMe Health (the existing system named in our proposal) runs a booking intake form before an appointment. Our self-check plays a similar role but sits *before* the customer picks a service.

## Question flow (v1)

| # | id | Question | Options | Effect |
|---|----|----------|---------|--------|
| 1 | `urgent` | Are you experiencing any of the following right now: chest pain, trouble breathing, severe bleeding, sudden weakness or confusion, or a severe allergic reaction? | Yes / No | Yes → urgent result, stop |
| 2 | `reason` | What brings you to the pharmacy today? | Vaccine, Medications, Blood pressure, Minor health concern, Something else | Primary recommendation |
| 3 | `multiple-meds` | Do you regularly take three or more prescription medications? | Yes / No | Yes → Medication Review |
| 4 | `flu-shot` | Have you had a flu shot this season? *(only asked October–April)* | Yes / No / Not sure | No or Not sure → Flu Vaccination |
| 5 | `bp-checked` | Have you had your blood pressure checked in the last 12 months? | Yes / No / Not sure | No or Not sure → Blood Pressure Check |

### Mapping for `reason`

| Answer | Service |
|--------|---------|
| `vaccine` | Flu Vaccination (in flu season), otherwise General Pharmacist Consultation |
| `medications` | Medication Review |
| `blood-pressure` | Blood Pressure Check |
| `minor-ailment` | Minor Ailment Consultation |
| `other` | General Pharmacist Consultation |

The primary service always comes first in the list. The extra services from questions 3–5 come after it, with no duplicates.

## Flu season (Iteration 2)

In B.C., flu vaccines are usually available starting in October [4], and flu spreads mostly in the late fall and winter [5]. FraserCare treats **October 1 to April 30** as flu season (`FLU_SEASON_START` / `FLU_SEASON_END` in `SelfCheckService`).

Outside flu season:
- Question 4 (`flu-shot`) is not shown and not required.
- Flu Vaccination is never suggested.
- If the customer picked "I want a vaccine", they get General Pharmacist Consultation plus a note that flu shots run October to April.

The date is checked in Pacific time. The service takes a `Clock`, so the tests can pin the date.

## Booking from the result (Iteration 2)

Each recommendation now carries the service's id from the `pharmacy_services` catalogue. "Book Appointment" opens `/?serviceId=<id>#booking`, and `booking.js` preselects that service so the customer only has to pick a date and time. If a service isn't in the catalogue (or is inactive), the result says "Ask a pharmacist in store" instead of showing a broken link.

Minor Ailment Consultation (20 min) and General Pharmacist Consultation (15 min) were added to the seed data, so every possible recommendation can be booked.

## API

- `GET /api/self-check/questions` returns the question list as JSON.
- `POST /api/self-check` takes `{ "answers": { "urgent": "no", "reason": "vaccine", ... } }` and returns `{ urgent, message, recommendedServices: [{ name, serviceId }], note, disclaimer }`. `note` is `null` unless there is something extra to tell the customer.
- An invalid or missing answer returns `400` with a message, following the same pattern as `AppointmentController`.

## Storing answers

The answers are not saved in Iteration 1, for two reasons:
1. Auth isn't merged yet, so there is no customer id to link a submission to.
2. Even "simple" answers (like "I take 3+ medications") are health-related. The proposal promises we will not store real patient health records. If we save anything later, it should probably be just the recommended services and a timestamp, not the raw answers. **Group to decide.**

## Open items / next iteration

- [x] Add "Minor Ailment Consultation" and "General Pharmacist Consultation" to the service catalogue.
- [x] Link "Book" on each recommendation to the booking page.
- [x] Only suggest a flu shot during flu season (October to April).
- [ ] Restrict the page to the CUSTOMER role once role-based access is merged (Ananda).
- [ ] Decide whether staff should see self-check submissions on the dashboard (Sehajvir).
- [ ] Move the questions out of Java code and into the database so staff can edit them (stretch).
- [ ] Add a nav link from the home page.

## References

1. Government of B.C., "Minor Ailments and Contraception Service (MACS)." https://www2.gov.bc.ca/gov/content/health/practitioner-professional-resources/pharmacare/initiatives/ppmac
2. HealthLink BC, "Pharmacy Services in B.C." https://www.healthlinkbc.ca/sites/default/files/documents/pharmacy-services-in-bc_june2026-english.pdf
3. MedMe Health. https://www.medmehealth.com/
4. HealthLink BC, "Inactivated Influenza (Flu) Vaccine" (HealthLinkBC File). https://www.healthlinkbc.ca/healthlinkbc-files/inactivated-influenza-flu-vaccine
5. HealthLink BC, "Influenza (Flu) Season." https://www.healthlinkbc.ca/influenza-flu-season
