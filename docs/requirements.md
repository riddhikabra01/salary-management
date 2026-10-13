# Salary Management: Requirements

## Goal
Give ACME's HR Manager a web application to manage salary data for roughly 10,000 employees across multiple countries, replacing the current Excel files, and to answer questions about how the organisation pays its people.

## Persona
**HR Manager** (single user). Needs to find any employee quickly, keep records accurate, and see pay patterns across the organisation without building pivot tables.

## Scope and features

### Must have
1. **Employee records**: create, view, edit and delete an employee (name, email, job title, department, country, salary, currency, joining date).
2. **Fast browsing**: paginated list with search by name or email and filters for country, department and job title. It must stay responsive with 10,000 rows, so paging, filtering and sorting are done on the server.
3. **Salary insights**: minimum, average, median-friendly range, maximum and headcount, grouped by country, department or job title.
4. **Seed data**: a script that loads 10,000 realistic employees so the app can be used immediately.

### Should have
5. **Salary history**: each salary change is stored with an effective date, so HR can see how someone's pay changed over time.
6. **Input validation** with clear error messages (for example, salary must be positive, email must be unique).

## Deliberately left out

| Item | Reason |
|---|---|
| Login and role-based access | There is a single persona. Adding auth would take time without showing more about the core problem. It is the first thing I would add before real use. |
| Payroll, tax and deductions | This is a salary *records and insights* tool, not a payroll system. |
| Live currency conversion | Needs an external rate service and raises questions about rate dates. Insights are calculated per currency instead of mixing currencies. |
| Approval workflows | Not needed for one user; adds a lot of complexity. |
| Excel import/export | Useful, but secondary to the main goal. A candidate for the next iteration. |
| Audit logging beyond salary history | Salary history covers the most important traceability need. |

## Assumptions (to be confirmed)
- One organisation, one HR user, no login.
- Salaries are stored in the employee's local currency.
- Backend in Java with Spring Boot and a relational database; UI in Angular.

## Non-functional goals
- List and search requests return in well under a second with 10,000 rows (indexed columns, aggregation in the database).
- Tests are fast and deterministic, with no dependence on the network, the clock or random data.
- Code is organised in clear layers (controller, service, repository) so each part is easy to read and test on its own.

## Success criteria
- HR can find an employee and update their salary in under a minute.
- HR can answer "what is the average pay for a role in a given country?" from one screen.
- The app runs from a fresh clone with the seed script and one command.
