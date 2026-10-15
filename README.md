# Salary Management

A web app for an HR manager to manage salary data for about 10,000 employees across several
countries, and to see how the organisation pays its people.

**Live demo:** https://YOUR-APP.onrender.com
(Render's free tier sleeps after about 15 minutes idle, so the first visit can take up to a minute.)

## What it does
- **Employees:** add, edit and delete people. Search by name or email, filter by country,
  department and job title, and sort by any column. Every column header also has a small filter icon
  that opens a menu for a second sort level, worded to suit the column (A–Z / Z–A, low to high,
  oldest first), for example country A–Z, then salary high to low.
- **Insights:** minimum, average and maximum salary per country, a salary distribution, and the
  average salary for a job title within a country.
- Paging, filtering, sorting and statistics all run in the database, so the app stays responsive with 10,000 rows.

Scope, assumptions and what was left out on purpose are in [`docs/requirements.md`](docs/requirements.md).

## Tech
| Part | Choice |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Data JPA, H2 |
| Frontend | Angular (standalone components, signals), Angular Material |
| Deploy | Docker, Render blueprint (`render.yaml`) |

## Project layout
```
backend/    Spring Boot API (employee, insights, seed, web packages)
frontend/   Angular app
docs/       Requirements and design notes
Dockerfile  Builds the UI, bundles it into the jar, runs one container
render.yaml Render deployment blueprint
```

## Run it locally
You need Java 17, Maven and Node 20.

**Backend** (loads 10,000 sample employees on first start):
```
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments=--app.seed.enabled=true
```
**Frontend** (in a second terminal; proxies `/api` to the backend):
```
cd frontend
npm install
npx ng serve
```
Open http://localhost:4200. Data is kept in `backend/data/`; delete that folder to start fresh.

## Run the tests
```
cd backend && mvn test
cd frontend && npx ng test
```

## Run it as one app (like production)
```
docker build -t salary-management .
docker run -p 8080:8080 -e APP_SEED_ENABLED=true salary-management
```
Open http://localhost:8080. Spring Boot serves both the API and the built Angular app.

## Design decisions
- **Layers:** controller → service → repository, so each piece is easy to read and test alone.
- **Business rules live in the entity.** `Employee` refuses blank text, a bad email or a non-positive
  salary, so invalid data can't get in through any route. The API's validation messages are the friendly first layer on top.
- **Money is `BigDecimal`,** stored with its own currency. Insights are per country, so no conversion is needed.
- **Stable sorting:** every list query ends with name, then id, so rows never jump between pages.
- **Whitelisted sort fields and a capped page size** keep the API safe from odd input.
- **Seeded with plain JDBC batch inserts** and a fixed random seed: fast, and the same data every time.
- **One deployable:** Spring Boot serves the Angular build, and a small controller forwards page URLs
  such as `/insights` to the app so refreshing works.

## Trade-offs
- **No login.** The brief has a single HR user. A real deployment needs authentication and roles.
- **Demo data resets on Render.** The free tier has no persistent disk, so an in-memory database is used and
  the 10,000 employees are re-seeded on each start. Production would use managed Postgres.
- **Per-country insights only.** No cross-country comparison, because that needs exchange rates.
- **Department changes overwrite.** Moving someone (say, Engineering to Human Resources) is a normal edit,
  but the old department isn't kept, and job title and department aren't cross-checked.
- **Simple email check.** Shape only (`name@domain.tld`); no deliverability check.

## Ideas for next steps
- Authentication and roles (HR Manager, read-only viewer).
- Salary history table, shown on an employee detail page.
- Put sort and filters in the URL so views can be shared and bookmarked.
- Database migrations (Flyway) instead of `ddl-auto=update`; Postgres in production.
- Full-text or trigram search for name/email at larger scale; keyset pagination for deep pages.
- OpenAPI docs, an end-to-end test (Playwright) and CI on every push.
- Excel import and export.
