# CampusOS

A modular monolith for campus issue reporting, resource booking, and operational analytics. Spring Boot 3 (Java 21) backend with a vanilla-JS frontend served by the same process. Design documents live in `docs/` (requirements, architecture, LLD, API, database, security, testing).

## Run with Docker (recommended)

Requires Docker Desktop and the local `.env` file (it provides `DB_PASSWORD` for the `campusos` MySQL user; the MySQL root password defaults to `campusroot` and can be overridden with `MYSQL_ROOT_PASSWORD`).

```bash
docker compose up --build -d
```

Then open http://127.0.0.1:8080/login.html in your browser. The stack is two containers: `campusos-app` (port 8080) and `campusos-mysql` (MySQL 9, host port 3307). Demo data is seeded on first start. Data survives `docker compose restart` and `docker compose down` in the `campusos-mysql-data` volume; `docker compose down -v` deletes it. Inspect with Docker only: `docker exec -it campusos-mysql mysql -ucampusos -p campusos`.

## Run locally (H2, no database server needed)

```bash
cd backend
mvn -s /tmp/campus-settings.xml -Dmaven.repo.local=/tmp/campus-m2 spring-boot:run -Dspring-boot.run.profiles=local
```

In a normal environment without a sandboxed Maven cache this is simply `mvn spring-boot:run -Dspring-boot.run.profiles=local`. Then open http://127.0.0.1:8080/login.html in your browser.

The `local` profile seeds demonstration data. Demo passwords are local-only: `admin@campus.edu` / `Admin@123`, `dept@campus.edu` / `Dept@123`, `tech1@campus.edu` / `Tech@123`, `faculty@campus.edu` / `Faculty@123`, `student@campus.edu` / `Student@123`. Never use them outside local development.

## Run with MySQL (Engineering-OS SSD)

MySQL data lives on the external SSD at `/Volumes/Engineering-OS/DevData/Databases/MySQL/data` (see that folder's `CONNECTION.md`). Start that server first, then run CampusOS with the `mysql` profile. The local `.env` file (gitignored, never commit it) holds the `campusos` database password:

```bash
/opt/homebrew/opt/mysql/bin/mysqld --defaults-file="/Volumes/Engineering-OS/DevData/Databases/MySQL/conf/my.cnf" --user="$(whoami)"
set -a; source .env; set +a
java -jar backend/target/campus-os-1.0.0.jar --spring.profiles.active=local,mysql
```

Flyway creates the schema (`users`, `issues`, `issue_activities`, `resources`, `bookings`) in the `campusos` database on first start; Hibernate only validates it. The `local` profile seeds the demo accounts into MySQL. Verify in MySQL Workbench or CLI with `USE campusos; SHOW TABLES;`

## Test

```bash
cd backend
mvn test
```

Tests run on H2 in MySQL-compatibility mode (see `docs/testing-strategy.md` for the MySQL fidelity note). The suite covers the issue workflow and priority scoring, the optimization ranking, issue visibility and role gates, booking overlap/approval, and a two-thread booking concurrency test proving exactly one overlapping booking succeeds.
