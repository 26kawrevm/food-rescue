# FoodRescue — IDT Project Prototype

A responsive food-surplus sharing web application based on the team's Food Wastage Solutions presentations.

**Developer credit in the website:** Vinay Kawre
**Team leader:** Alok Pimpalkhede
**Team:** Anandi Jambulkar, Anushka Bagekar, Atharva Raut, Rudrani Chandore, Sujal Mahajan and Vinay Kawre.

## Technology
- Frontend: HTML, CSS, vanilla JavaScript
- Backend: Java 17 + Spring Boot REST API
- Database: H2 in file mode (data persists in `./data/foodrescue-db`)
- Build: Maven

## Run on Windows
1. Install **Java JDK 17 or newer** and **Apache Maven**.
2. Extract this ZIP to a folder.
3. Open PowerShell in the extracted `FoodRescue` folder.
4. Run:
   ```powershell
   mvn spring-boot:run
   ```
5. Wait until Spring Boot says it has started. Open **http://localhost:8080** in Chrome or Edge.

If PowerShell says `mvn` is not recognized, install Maven and reopen PowerShell. Check installations with `java -version` and `mvn -version`.

The checked-in `.env.example` documents the local defaults. The workspace `.env` is loaded automatically by Spring Boot and is excluded from Git; do not put real credentials in `.env.example`.

## Use it on other devices on the same Wi-Fi
1. Keep the computer running the app connected to the Wi-Fi.
2. Find that computer's local IPv4 address using `ipconfig` in Command Prompt (look under the active Wi-Fi adapter).
3. On another device connected to the same Wi-Fi, open `http://YOUR-COMPUTER-IP:8080` (replace with the IPv4 address).
4. If Windows Firewall asks, allow Java on **Private networks**. Do not expose the database console to an untrusted network.

## Deploy with Render and Vercel
- **Render** runs the Spring Boot API and bundled website from `Dockerfile`. The included `render.yaml` configures a persistent disk at `/var/data`, the H2 database, and a health check. The disk requires a paid Render service plan.
- **Vercel** serves the static frontend and rewrites `/api/*` to the Render service URL configured in `vercel.json`.
- Connect this GitHub repository to Render using its Blueprint flow and to Vercel as a project. Keep the Render service name `food-rescue-api-26kawrevm`, or update the API rewrite in `vercel.json` if you choose a different service name.
- The H2 file database is for a classroom prototype and a single running server. For public multi-user use, move to managed PostgreSQL and add authentication, access controls, backups, and moderation before accepting real donations.

## Prototype features
- Create, search and filter surplus food listings.
- Record quantity, unit, location, pickup deadline, storage, allergen and expiry details.
- Recipients can submit pickup requests.
- Update requests through Pending, Approved, Collected and Rejected.
- Listing status follows request status.
- Dashboard counters update from saved records.
- Data is stored by the Java backend, not browser local storage.

## API endpoints
- `GET /api/listings`, `POST /api/listings`, `DELETE /api/listings/{id}`
- `GET /api/requests`, `POST /api/requests`
- `PATCH /api/requests/{id}/status` with JSON such as `{"status":"COLLECTED"}`
- `GET /api/dashboard`

## Important food-safety note
This is an educational coordination prototype, not a food-safety approval system. A checkbox records that the provider says details were checked; it does not certify food as safe. Users must follow applicable local regulations, safe storage/handling practices and verify allergens, expiry and suitability before collection.
