# Farm Fresh

An online store for fresh farm produce. Customers can browse products, sign up with an email code (OTP), add items to a cart, and place cash-on-delivery orders.

- **Frontend:** React + Vite + Tailwind CSS (the website you see in the browser)
- **Backend:** Spring Boot (Java 17) with JWT login and email OTP (the server that handles accounts and orders)
- **Database:** MySQL 8 (stores users, products and orders)

This guide walks you through getting the app running on your own computer, step by step. You don't need to know Java or React to follow it.

---

## Contents

1. [How the app fits together](#1-how-the-app-fits-together)
2. [Get the code](#2-get-the-code)
3. [Create your `.env` file](#3-create-your-env-file)
4. [Option A: Run everything with Docker (easiest)](#4-option-a-run-everything-with-docker-easiest)
5. [Option B: Run each part yourself (no Docker)](#5-option-b-run-each-part-yourself-no-docker)
6. [Try it out](#6-try-it-out)
7. [Troubleshooting](#7-troubleshooting)
8. [Settings reference](#8-settings-reference)
9. [Project layout](#9-project-layout)

---

## 1. How the app fits together

There are three parts, and all three have to be running for the site to work:

```
 Browser  ──►  Frontend (React)  ──►  Backend (Spring Boot)  ──►  MySQL database
                                            │
                                            └──►  Gmail (sends OTP codes and order emails)
```

- The **frontend** is the website. It asks the backend for data through URLs that start with `/api`.
- The **backend** checks logins, saves orders and sends emails.
- The **database** keeps everything. It starts with sample categories and products from `Untitled.sql`.

You can start all three with one Docker command (Option A), or install and start each one yourself (Option B).

---

## 2. Get the code

The repository is private, so the owner first has to give you access: on GitHub, open the repo, go to **Settings → Collaborators**, and add your GitHub username. Accept the email invite.

Then, in a terminal:

```sh
git clone https://github.com/nithishramurthy23/ms-harvests.git
cd ms-harvests
```

All commands below are run from inside this `ms-harvests` folder unless a step says otherwise.

> **New to the terminal?** On macOS, open the **Terminal** app (press `Cmd + Space`, type "Terminal", press Enter). On Windows, use **PowerShell** or **Git Bash**.

---

## 3. Create your `.env` file

The app needs a few passwords and secrets. These are **never** stored in the repository, so everyone creates their own. They live in a file called `.env`, which Git ignores so it can't be committed by accident.

**Step 1.** Make your own copy of the example file:

```sh
cp .env.example .env
```

**Step 2.** Open `.env` in any text editor (for example `open -e .env` on macOS, or VS Code) and fill in these values:

| Setting | What to put there |
| --- | --- |
| `MAIL_USERNAME` | A Gmail address the app will send emails from, e.g. `yourname@gmail.com`. |
| `MAIL_PASSWORD` | A Gmail **app password** for that address (not your normal Gmail password). See below. |
| `DB_PASSWORD` | A password for the MySQL `root` user. With Docker, pick anything you like and Docker sets it up. Without Docker, use the password you gave MySQL when installing it. |
| `JWT_SECRET` | A long random string used to sign login tokens. Generate one with the command below. |

Leave the other lines as they are unless you know you need to change them.

**Generating a JWT secret.** Run this and paste the output after `JWT_SECRET=` (all on one line):

```sh
openssl rand -base64 64 | tr -d '\n'; echo
```

**Getting a Gmail app password.**

1. Turn on **2-Step Verification** for the Gmail account at https://myaccount.google.com/security (Google only allows app passwords when this is on).
2. Go to https://myaccount.google.com/apppasswords.
3. Type a name such as "Farm Fresh" and click **Create**.
4. Google shows a 16-character password like `abcd efgh ijkl mnop`. Copy it into `MAIL_PASSWORD` **without the spaces**: `MAIL_PASSWORD=abcdefghijklmnop`.

A finished `.env` looks roughly like this (with your own values):

```ini
MAIL_USERNAME=yourname@gmail.com
MAIL_PASSWORD=abcdefghijklmnop
DB_URL=jdbc:mysql://localhost:3306/farm_fresh?serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=my-db-password
JWT_SECRET=Zm9vYmFyZm9vYmFy...a-long-random-string...
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
VITE_API_URL=http://localhost:8080/api
```

> Without working Gmail settings the app still starts, but sign-up codes and order emails won't arrive. See [Troubleshooting](#7-troubleshooting) for a way to test without email.

---

## 4. Option A: Run everything with Docker (easiest)

Docker downloads and runs MySQL, the backend and the frontend for you, so you don't have to install Java, Maven, Node or MySQL.

### 4.1 Install Docker

- **macOS / Windows:** install [Docker Desktop](https://www.docker.com/products/docker-desktop/), open it, and wait until it says Docker is running.
- **Linux:** install Docker Engine and the Compose plugin ([instructions](https://docs.docker.com/engine/install/)).

Check it works:

```sh
docker --version
docker compose version
```

### 4.2 Start the app

Make sure you've created `.env` (step 3), then run:

```sh
docker compose up --build
```

The first run takes several minutes, because it downloads base images and builds the Java and React code. You'll see a lot of log output. It's ready when the logs settle and you see lines like `Started FarmFreshApplication`.

Open **http://localhost:8081** in your browser.

### 4.3 What's running

| Container | What it does |
| --- | --- |
| `mysql` | The database. The first time it starts, it loads `Untitled.sql` (tables + sample products). Data is kept in a Docker volume, so it survives restarts. |
| `backend` (2 copies) | The Spring Boot server. Two copies run so that if one crashes, the site keeps working. |
| `web` | nginx: serves the website on port 8081 and passes `/api` requests to the backend copies. |

### 4.4 Everyday Docker commands

```sh
docker compose up -d --build     # start in the background (-d) after code changes
docker compose logs -f backend   # watch backend logs (Ctrl + C to stop watching)
docker compose ps                # see what's running
docker compose down              # stop everything (your data is kept)
docker compose down -v           # stop and DELETE the database, so it reloads Untitled.sql next time
BACKEND_REPLICAS=4 docker compose up -d   # run 4 backend copies instead of 2
```

If you change `DB_PASSWORD` after the first run, run `docker compose down -v` once, because MySQL only reads that password when the database is first created.

---

## 5. Option B: Run each part yourself (no Docker)

Use this if you'd rather not install Docker, or you want to work on the code with fast reloads. You'll install the tools once, then keep **two terminal windows** open: one for the backend, one for the frontend.

### 5.1 Install the tools

You need **Java 17**, **Maven**, **Node.js 18 or newer**, and **MySQL 8**.

**macOS** (using [Homebrew](https://brew.sh); install it first if you don't have it):

```sh
brew install openjdk@17 maven node mysql
brew services start mysql
```

Homebrew prints a line telling you how to put Java 17 on your `PATH`; run it. Then set a MySQL root password (use the same one you put in `DB_PASSWORD`):

```sh
mysql_secure_installation
```

**Windows:** install [Temurin JDK 17](https://adoptium.net/temurin/releases/?version=17), [Maven](https://maven.apache.org/download.cgi), [Node.js LTS](https://nodejs.org/) and [MySQL Community Server 8](https://dev.mysql.com/downloads/mysql/) (the installer asks you to set a root password).

**Check everything is installed:**

```sh
java -version     # should say 17
mvn -v
node -v           # should be v18 or higher
mysql --version
```

### 5.2 Set up the database

This creates the `farm_fresh` database with all tables and sample products. Enter your MySQL root password when asked:

```sh
mysql -u root -p < Untitled.sql
```

> ⚠️ This script drops and recreates the tables, so running it again **wipes all users and orders**. Only run it for first-time setup or when you want a clean start.

### 5.3 Start the backend (terminal 1)

From the project folder (it must be this folder, so the backend finds your `.env`):

```sh
mvn spring-boot:run
```

The first run downloads dependencies and takes a few minutes. It's ready when you see `Started FarmFreshApplication`. The backend now listens on **http://localhost:8080**. Leave this terminal open.

Quick check in another terminal: `curl http://localhost:8080/actuator/health` should print `{"status":"UP"}`.

### 5.4 Start the frontend (terminal 2)

Open a second terminal in the same project folder:

```sh
npm install     # first time only, downloads the frontend libraries
npm start
```

Open **http://localhost:5173** in your browser. Edits to the React code show up instantly.

To stop either part, press `Ctrl + C` in its terminal.

---

## 6. Try it out

1. Open the site (http://localhost:8081 with Docker, http://localhost:5173 without).
2. Browse categories and products, and add some to your cart.
3. Sign up with your details, then check your inbox for a 6-digit code. Enter it to activate your account.
4. Go to checkout and place an order. Payment is **cash on delivery** only.
5. Your order appears in your order history, and a confirmation email is sent.

---

## 7. Troubleshooting

**The verification code email never arrives.**
Check `MAIL_USERNAME` and `MAIL_PASSWORD` in `.env` (app password, no spaces, 2-Step Verification on), then restart the backend. Look in the spam folder too. Backend logs show mail errors.

**I want to test without email.** Sign up on the site, then activate the account directly in the database:

```sh
# Without Docker
mysql -u root -p farm_fresh -e "UPDATE users SET enabled = 1 WHERE email = 'you@example.com';"
# With Docker
docker compose exec mysql mysql -uroot -p farm_fresh -e "UPDATE users SET enabled = 1 WHERE email = 'you@example.com';"
```

Then log in with your email and password.

**`Access denied for user 'root'@'localhost'`.**
`DB_PASSWORD` in `.env` doesn't match your MySQL root password. With Docker, run `docker compose down -v` and start again so the database is recreated with the current password.

**`Communications link failure` or the backend can't reach the database.**
MySQL isn't running. Without Docker: `brew services start mysql` (macOS) or start the MySQL service (Windows). With Docker, wait for the `mysql` container to be healthy (`docker compose ps`).

**`Port 8080 (or 8081, 5173, 3306) is already in use`.**
Something else is using that port, often an older copy of this app. Find it with `lsof -i :8080` (macOS/Linux) and stop it. If you already run MySQL on your computer, that's fine for Docker, because the Docker database doesn't use port 3306 on your machine.

**`WeakKeyException` or other JWT errors on login.**
`JWT_SECRET` is empty or too short. Generate one with the `openssl` command in step 3.

**The page loads but products, login or orders fail ("Network Error").**
The backend isn't running or crashed. Check its terminal (or `docker compose logs backend`). Without Docker, make sure you opened the site at `http://localhost:5173`, which is allowed by `CORS_ALLOWED_ORIGINS`.

**`mvn: command not found`, or `java -version` shows the wrong version.**
Reinstall per step 5.1 and open a new terminal so the `PATH` change takes effect.

**I want to start over with a clean database.**
With Docker: `docker compose down -v`, then `docker compose up --build`. Without Docker: `mysql -u root -p < Untitled.sql`.

---

## 8. Settings reference

All settings come from `.env` (or real environment variables).

| Variable | Used by | Default |
| --- | --- | --- |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | backend email (OTP, order confirmations, contact form) | none |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | backend database | `localhost:3306/farm_fresh`, `root`, none |
| `JWT_SECRET`, `JWT_EXPIRATION` | backend login tokens | none, 24h (`86400000` milliseconds) |
| `CORS_ALLOWED_ORIGINS` | backend, comma-separated frontend URLs allowed to call it | `http://localhost:5173,http://localhost:3000` |
| `PORT` | backend port | `8080` |
| `VITE_API_URL` | frontend, where to find the backend | `http://localhost:8080/api` |
| `BACKEND_REPLICAS` | Docker only, number of backend copies | `2` |

With Docker, `DB_URL`, `DB_USERNAME` and `CORS_ALLOWED_ORIGINS` are set automatically in `docker-compose.yml`, so you only need `MAIL_*`, `DB_PASSWORD` and `JWT_SECRET`.

---

## 9. Project layout

```
ms-harvests/
├── src/                     React frontend (pages, components, cart and login state)
│   ├── api/axiosInstance.js   every call to the backend goes through here
│   ├── pages/                 one file per page (Home, Products, Checkout, ...)
│   └── main/                  the Spring Boot backend lives here, inside src/
│       ├── java/com/pooja/farm/fresh/           controllers, services, entities, security
│       └── resources/application.properties    backend settings (reads .env)
├── Untitled.sql             database tables + sample categories, products, discounts
├── pom.xml                  backend dependencies (Maven)
├── package.json             frontend dependencies (npm)
├── docker-compose.yml       runs MySQL + backend copies + nginx together
├── Dockerfile.backend       how the backend image is built
├── Dockerfile.frontend      how the frontend image is built
├── deploy/nginx.conf        nginx: serves the site and load-balances /api
├── .env.example             template for your own .env
└── server/                  an old, unused Express + MongoDB prototype (ignore it)
```

---

## Before putting this online

This setup is for running the app on your own computer. Before real customers use it, serve it over HTTPS, use fresh secrets that have never been shared, and set up regular database backups.
