# 🛡️ AI-Powered Enterprise Security Platform

An AI-powered security platform that combines **machine learning risk scoring**, **intrusion detection**, and **firewall management** into a unified REST API — built with Spring Boot 3.2 and a Python ML microservice.

---

## ✨ Features

| Module | Capabilities |
|---|---|
| **Authentication** | JWT-based auth, BCrypt password hashing, role-based access (Admin/Analyst/User), auto account locking after failed attempts |
| **ML Risk Scoring** | Isolation Forest anomaly detection on 7 behavioral features per login, with rule-based fallback when ML service is down |
| **Intrusion Detection** | Rate limiting (30 req/min per IP), brute force detection, malicious pattern matching (SQLi, XSS, directory traversal, command injection) |
| **Firewall Engine** | CRUD management for firewall rules, real-time traffic evaluation with priority-based matching, wildcard support |
| **Audit Trail** | Full event logging with JSONB data for every security action |

---

## 🏗️ Architecture

```
┌──────────────┐      ┌──────────────────────────────────┐      ┌─────────────────┐
│   Client     │─────▶│  Spring Boot 3.2 (Java 17)       │─────▶│  PostgreSQL     │
│  (REST API)  │      │                                  │      │  (Supabase)     │
│              │      │  ┌──────────┐  ┌──────────────┐  │      └─────────────────┘
│              │      │  │ Auth     │  │ Feature      │  │
│              │      │  │ Service  │──│ Engineering  │  │      ┌─────────────────┐
│              │      │  └──────────┘  └──────┬───────┘  │      │  Python Flask   │
│              │      │  ┌──────────┐         │          │─────▶│  ML Service     │
│              │      │  │ IDS      │  ┌──────▼───────┐  │      │  (Isolation     │
│              │      │  │ Service  │  │ ML Service   │  │      │   Forest)       │
│              │      │  └──────────┘  │ Client       │  │      └─────────────────┘
│              │      │  ┌──────────┐  └──────────────┘  │
│              │      │  │ Firewall │                     │
│              │      │  │ Service  │                     │
│              │      │  └──────────┘                     │
│              │      └──────────────────────────────────┘
└──────────────┘
```

---

## 🧠 ML Risk Scoring Pipeline

Every login extracts **7 behavioral features** and feeds them into an Isolation Forest model:

| # | Feature | Description |
|---|---|---|
| 1 | `loginFrequency24h` | Login count in the last 24 hours |
| 2 | `isNewIp` | Whether the IP was never seen for this user |
| 3 | `isNewDevice` | Whether the User-Agent was never seen |
| 4 | `failedAttemptRatio` | Ratio of failed to total recent logins |
| 5 | `hourOfDay` | Hour of login (0–23) |
| 6 | `isWeekend` | Weekend flag |
| 7 | `totalUniqueIps` | Historical count of unique IPs |

**Risk levels:** LOW (0–29), MEDIUM (30–69), HIGH (70–100)

> **Note:** The model is currently trained on synthetic behavioral data for demonstration purposes. In production, historical login data would be used via the `/train` endpoint. The rule-based fallback engine provides consistent scoring when the ML service is unavailable.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2, Java 17 |
| Security | Spring Security, JWT (jjwt), BCrypt-12 |
| Database | PostgreSQL, Spring Data JPA |
| ML Service | Python Flask, scikit-learn (Isolation Forest) |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Monitoring | Spring Actuator |
| Containers | Docker, Docker Compose |

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven
- PostgreSQL 15+
- Python 3.11+ (for ML service)

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repo
git clone https://github.com/Gamerboyi/identify-risk-intelligence-platform.git
cd identify-risk-intelligence-platform

# Start all services (API + ML + PostgreSQL)
docker compose up --build
```

The API will be available at `http://localhost:8080` and Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Option 2: Run Locally

```bash
# 1. Set up the database
psql -U postgres -c "CREATE DATABASE securitydb;"
psql -U postgres -d securitydb -f src/main/resources/schema.sql

# 2. Set environment variables
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=your_64_char_hex_secret

# 3. Start the ML service
cd ml-service
pip install -r requirements.txt
python train.py    # Train the model first
python app.py      # Start on port 5000

# 4. Start the Spring Boot API (new terminal)
cd ..
./mvnw spring-boot:run
```

### Run Tests

```bash
./mvnw test
```

---

## 📡 API Endpoints

### Authentication (Public)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT + risk score |
| GET | `/api/auth/health` | Health check |

### Firewall (Admin/Analyst)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/firewall/rules` | Add a firewall rule (Admin) |
| GET | `/api/firewall/rules` | List all rules |
| GET | `/api/firewall/rules/active` | List active rules only |
| PUT | `/api/firewall/rules/{id}/toggle` | Enable/disable a rule (Admin) |
| DELETE | `/api/firewall/rules/{id}` | Delete a rule (Admin) |
| POST | `/api/firewall/evaluate` | Evaluate traffic against rules |

### Intrusion Detection (Admin/Analyst)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/ids/analyze` | Full threat analysis |
| GET | `/api/ids/check/ratelimit` | Check if IP is rate limited |
| GET | `/api/ids/check/bruteforce` | Check for brute force attacks |
| POST | `/api/ids/check/pattern` | Scan for malicious patterns |

---

## 📁 Project Structure

```
eurds/
├── src/main/java/com/vedant/eurds/
│   ├── config/          # Swagger, CORS configuration
│   ├── controller/      # REST controllers (Auth, Firewall, IDS)
│   ├── dto/             # Request/Response DTOs
│   ├── exception/       # Global exception handler
│   ├── model/           # JPA entities
│   ├── repository/      # Spring Data repositories
│   ├── security/        # JWT filter, SecurityConfig
│   └── service/         # Business logic + ML integration
├── src/test/            # Unit tests (JUnit 5 + Mockito)
├── ml-service/          # Python Flask ML microservice
│   ├── app.py           # Flask API (predict, train, health)
│   ├── train.py         # Isolation Forest training script
│   └── model/           # Trained model artifacts (.pkl)
├── docker-compose.yml   # One-command deployment
├── Dockerfile           # Multi-stage Java build
└── .env.example         # Environment variable template
```

---

## 🔒 Security Design

- **Stateless JWT sessions** — no server-side session storage
- **BCrypt-12** password hashing — enterprise-grade cost factor
- **Account auto-locking** after 5 failed login attempts
- **Locked account JWT rejection** — locked users can't use existing tokens
- **Role-based access control** — Admin, Analyst, User roles with method-level security
- **Audit trail** — every security event logged with JSONB event data
- **Request validation** — `@Valid` on all endpoints with proper error responses

---

## 📄 License

MIT
