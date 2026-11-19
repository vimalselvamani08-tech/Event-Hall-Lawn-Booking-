# Marriage Hall and Community Center Management System

A Java 17 / Maven WAR application for venue scheduling, customer records, advance payments, installments, and admin reports. It uses Jakarta Servlet/JSP on Tomcat 10.1 and SQLite through JDBC.

## Requirements

- JDK 17
- Maven 3.9 or newer
- Apache Tomcat 10.1

## Database setup

The application creates `%USERPROFILE%\event_hall_management.db` on first startup, creates its tables, and inserts sample customers and venues. The sample admin is `admin` / `password`. Passwords are checked with BCrypt.

Visitors can browse venues from `/home`, create a customer account at `/user/signup`, sign in at `/user/login`, and reserve venues from `/user/bookings`. Administrators sign in at `/login`; a signed-in administrator can create additional admin accounts at `/admin/signup`. Admin creation is intentionally not open to unauthenticated visitors.

Change `db.url` in `src/main/resources/db.properties` or set `-Deventhall.db.url=jdbc:sqlite:C:/path/to/event_hall_management.db` to choose another file. No database server or separate schema import is required.

## Build and run

Run the unit tests and package the WAR:

```powershell
mvn clean test package
```

Copy `target/event-hall-management.war` to Tomcat's `webapps` directory and start Tomcat. Open `http://localhost:8080/event-hall-management/`; the app redirects admins to `/login`.

## Main workflows

- Add venues and customers from the corresponding admin screens.
- Create a booking by selecting a customer, active venue, future date, time slot, guest count, and positive advance.
- Venue rent is calculated by its concrete venue type. Guest counts over venue capacity are rejected.
- Booking creation performs the service availability check and inserts the booking and advance payment in one transaction. SQLite's unique key on `(venue_id, event_date, slot)` resolves concurrent attempts safely.
- Record later installments from a booking's payment link. The service acquires SQLite's write lock, checks the current balance, writes the payment, and marks a zero-balance booking `FULLY_PAID`.
- Receipts are written to `receipts/`; CSV exports are written to `exports/` and streamed to the browser. Audit events are appended to `logs/audit.log`.
- Reports include pending balances, upcoming events, booking history, and selected-customer payment history.
- Customer passwords and administrator passwords are BCrypt hashes. Customer login uses email; new passwords must be at least 10 characters. An existing customer record without a password can be linked during signup only when the supplied name and phone match the stored record.

Cancelled booking rows retain their original venue/date/slot because the required unique key applies to all booking rows. Therefore that slot cannot be booked again while the cancelled record remains; the booking history remains intact.

## Project layout

- `src/main/java/com/eventhall/model`: domain objects and venue subclasses
- `src/main/java/com/eventhall/dao`: DAO interfaces and JDBC implementations
- `src/main/java/com/eventhall/service`: authentication and booking/payment/report rules
- `src/main/java/com/eventhall/servlet` and `filter`: HTTP controllers and admin session access control
- `src/main/webapp/WEB-INF/views`: JSP screens, kept out of direct public access
- `src/main/resources/schema.sql`: schema and demo data
- `src/test/java`: booking availability and payment-balance tests

## Security notes

The admin filter protects every `/admin/*` route. Deploy behind HTTPS, replace the sample credentials, and provide database credentials through deployment-specific JVM properties or a protected config file. The checked-in properties file is a local-development template, not a production secret store.