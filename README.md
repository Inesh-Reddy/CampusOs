# CampusOS

A modular monolith for campus issue reporting, resource booking, and operational analytics. Spring Boot 3 (Java 21) backend with a vanilla-JS frontend served by the same process. Design documents live in `docs/` (requirements, architecture, LLD, API, database, security, testing).

## Before you start

Run every command below from the project root (`/Volumes/Engineering-OS/muse/Wonder_Project`) unless a step says otherwise. The `local` profile seeds demonstration data on first start. Demo logins are local-only: `admin@campus.edu` / `Admin@123`, `dept@campus.edu` / `Dept@123`, `tech1@campus.edu` / `Tech@123`, `faculty@campus.edu` / `Faculty@123`, `student@campus.edu` / `Student@123`.

## Option A — Turn up with Docker (recommended)

You need Docker Desktop running and a local `.env` file. If `.env` does not exist yet, create it with `cp .env.example .env` and set a strong `DB_PASSWORD` in it. That password is used for the `campusos` MySQL user inside Docker and is only applied when the Docker database is first created.

1. Confirm Docker is running:

   ```bash
   docker info
   ```

   If this fails, open Docker Desktop and wait until it reports the engine is running.

2. Build and start the whole project (application plus MySQL):

   ```bash
   docker compose up --build -d
   ```

   Use `docker compose up -d` without `--build` for later starts when the code has not changed.

3. Check that both containers are up:

   ```bash
   docker compose ps
   docker compose logs -f app
   ```

   Wait until `campusos-mysql` shows `healthy` and the app log shows `Started CampusOsApplication`. Press `Ctrl+C` to stop following logs; the containers keep running.

4. Open http://127.0.0.1:8080/login.html in your browser and sign in with a demo login above.

5. Day-to-day Docker commands:

   ```bash
   docker compose restart     # restart both containers, data is kept
   docker compose down        # stop and remove containers, data is kept
   docker compose up -d       # start again after a down
   docker compose down -v     # stop and DELETE all Docker database data — use with care
   ```

   Data lives in the `campusos-mysql-data` Docker volume (physically on the Engineering-OS SSD). To inspect it, connect MySQL Workbench to host `127.0.0.1`, port `3307`, user `campusos`, with the `DB_PASSWORD` from `.env`, or run `docker exec -it campusos-mysql mysql -ucampusos -p campusos`.

## Option B — Turn up without Docker

You need Java 21 and Maven installed (`java -version` and `mvn -version` should both work). Choose B1 for MySQL storage or B2 for a zero-setup file database.

### B1 — Without Docker, using MySQL on the Engineering-OS SSD

1. Start the SSD MySQL server (leave this terminal running):

   ```bash
   /opt/homebrew/opt/mysql/bin/mysqld --defaults-file="/Volumes/Engineering-OS/DevData/Databases/MySQL/conf/my.cnf" --user="$(whoami)"
   ```

   The `campusos` database and `campusos` user must already exist in that server. On a fresh MySQL, create them first as the MySQL root user with `CREATE DATABASE campusos;` and a matching `campusos` user granted all privileges on `campusos.*`.

2. In a second terminal, load the database credentials and build the application:

   ```bash
   set -a; source .env; set +a
   mvn -f backend/pom.xml package
   ```

   The `.env` file is gitignored and holds `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER` for MySQL. Never commit it.

3. Start the application:

   ```bash
   java -jar backend/target/campus-os-1.0.0.jar --spring.profiles.active=local,mysql
   ```

4. Verify it is up, then open the browser page:

   ```bash
   curl http://127.0.0.1:8080/actuator/health
   ```

   You should see `{"status":"UP"}`. Open http://127.0.0.1:8080/login.html and sign in with a demo login above. Stop the app with `Ctrl+C` in its terminal.

### B2 — Without Docker and without any database server (H2 file database)

1. Start the application — no credentials or database setup are needed:

   ```bash
   mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. Open http://127.0.0.1:8080/login.html and sign in with a demo login above. Stop the app with `Ctrl+C`.

   In this mode data is stored in an H2 file at `./data/campusos.mv.db` relative to the folder you launched from. Delete that `data` folder while the app is stopped to start again from an empty database.

## Test

```bash
cd backend
mvn test
```

Tests run on H2 in MySQL-compatibility mode (see `docs/testing-strategy.md` for the MySQL fidelity note). The suite covers the issue workflow and priority scoring, the optimization ranking, issue visibility and role gates, booking overlap/approval, and a two-thread booking concurrency test proving exactly one overlapping booking succeeds.
