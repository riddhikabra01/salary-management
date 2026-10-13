# Salary Management: Requirements

## Goal
Give ACME's HR Manager a web application to manage salary data for roughly 10,000 employees across multiple countries, replacing the current Excel files, and to answer questions about how the organisation pays its people.

## Persona
**HR Manager** (single user). Needs to find any employee quickly, keep records accurate, and see pay patterns without building pivot tables. There is no login.

## Scope and features

### Must have
1. **Employee records**: create, view, update and delete an employee (full name, email, job title, department, country, salary, currency, joining date).
2. **Fast browsing**: paginated list with search by name or email and filters for country, department and job title. Paging, filtering and sorting are done on the server so the list stays responsive with 10,000 rows.
3. **Core salary insights** (required):
   - Minimum, maximum and average salary in a country.
   - Average salary for a job title in a country.
4. **Seed data**: a script that loads 10,000 realistic employees so the app is usable immediately.
5. **Input validation** with clear error messages (for example, salary must be positive and email must be unique).

### Additional insights (my choice)
- **Headcount per country and per job title**: an average means little without knowing how many people it is based on. A country with 3 people and one with 3,000 should not look equally reliable.
- **Salary distribution per country**: salary bands show whether pay is concentrated or widely spread, which an average hides.

## Assumptions
- Single HR Manager, no login.
- Salaries are stored in local currency. Insights are reported per country, so no conversion is needed. Cross-country comparison is not built; if added later, fixed documented exchange rates would be used.
- Department is part of the data model, so it is available as a filter. Insights are not grouped by department because the required insights are by country and job title.
- One currency per country in the data.
- Job titles are matched as exact text, so "Software Engineer" and "software engineer" are treated the same after trimming and case normalisation.

## Deliberately left out
| Item | Reason |
|---|---|
| Login and role-based access | Single persona. See "Future work". |
| Salary history | Not required. See "Future work". |
| Currency conversion | Insights are per country. |
| Payroll, tax and deductions | This is a records and insights tool, not payroll. |
| Approval workflows | Not needed for one user. |
| Excel import and export | Useful, but secondary to the main goal. |

## Future work
- **Authentication and roles**: add Spring Security with token-based login and two roles (HR Manager with full access, read-only viewer). Endpoints would be protected by role, and the Angular app would add a login page and route guards.
- **Salary history**: add a `salary_history` table (employee id, salary, currency, effective date) and write a row whenever the salary changes. The current salary stays on the employee record for fast reads, and the history is shown on the employee detail page.

## Non-functional goals
- List, search and insight requests return in well under a second with 10,000 rows (indexed columns, aggregation done in the database).
- Tests are fast and deterministic, with no dependence on the network, the clock or random data.
- Code is organised in clear layers (controller, service, repository) so each part is easy to read and test on its own.

## Success criteria
- HR can find an employee and update their salary in under a minute.
- HR can see the minimum, maximum and average salary for a country, and the average for a job title within a country, from one screen.
- The app runs from a fresh clone with the seed script and one command.
