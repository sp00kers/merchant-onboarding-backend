# Merchant Onboarding Backend

Spring Boot backend for the Merchant Onboarding Platform — a banking system for managing merchant onboarding cases with role-based access, document management, external verification via Kafka, and real-time notifications via WebSocket.

## Tech Stack

- **Java 17** + **Spring Boot 3.5.5**
- **MySQL** (relational database)
- **Apache Kafka** (event-driven external verification)
- **Docker Compose** (Zookeeper, Kafka, Mock External API)
- **JWT** authentication
- **WebSocket (STOMP)** for real-time notifications
- **Tess4J** (OCR), **OpenPDF** (PDF generation)

## Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 17+ |
| Maven | 3.8+ (or use included `mvnw`) |
| MySQL | 8.0+ |
| Docker Desktop / Rancher Desktop | Latest |
| Node.js | 18+ *(only for mock API if running standalone)* |

## Setup Instructions

### 1. Clone the repository

```bash
git clone https://github.com/sp00kers/merchant-onboarding-backend.git
cd merchant-onboarding-backend
```

### 2. Set up MySQL

Create the database (Hibernate auto-creates tables, but the schema must exist):

```sql
CREATE DATABASE IF NOT EXISTS `merchant-onboarding`;
```

> The default credentials in `application.properties` are `root` / `root`. Update them in `src/main/resources/application.properties` if your MySQL uses different credentials:
> ```properties
> spring.datasource.username=root
> spring.datasource.password=root
> ```

### 3. Start Kafka & Mock External API (Docker)

```bash
docker compose up -d
```

This starts three containers:
- **Zookeeper** (port 2181)
- **Kafka broker** (port 9092)
- **Mock External API** (port 3001) — simulates Bank Negara, SSM, and other verification agencies via Kafka

Wait for Kafka to become healthy (~30 seconds):

```bash
docker inspect --format='{{.State.Health.Status}}' kafka
```

> **Windows + WSL2 users (Rancher Desktop):** If Kafka is unreachable at `localhost:9092`, run the port-forwarding script **as Administrator**:
> ```powershell
> .\setup-kafka-proxy.ps1
> ```

### 4. Seed initial data

On **first run only**, enable data seeding by changing this line in `src/main/resources/application.properties`:

```properties
spring.sql.init.mode=always
```

After the first successful startup, change it back to `never` to preserve data between restarts:

```properties
spring.sql.init.mode=never
```

This seeds: roles, permissions, default users, business types, and merchant categories.

### 5. Build & run the backend

```bash
# Using Maven wrapper (no Maven install needed)
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run
```

The server starts at **http://localhost:8080**.

## Default User Accounts

All default passwords are `password123`.

| Email | Role | Description |
|-------|------|-------------|
| `john.doe@bank.com` | Onboarding Officer | Creates new merchant cases |
| `jane.smith@bank.com` | Compliance Reviewer | Reviews cases for regulatory compliance |
| `sarah.lee@bank.com` | System Administrator | Full system access & user management |

## API Base URL

```
http://localhost:8080/api
```

## Project Structure

```
src/main/
├── java/com/merchantonboarding/
│   ├── controller/     # REST API endpoints
│   ├── model/          # JPA entities
│   ├── repository/     # Data access layer
│   ├── service/        # Business logic
│   ├── security/       # JWT & Spring Security config
│   ├── event/          # Kafka event models
│   └── config/         # App configuration
├── resources/
│   ├── application.properties
│   └── data.sql        # Seed data script
docker-compose.yml      # Kafka + Zookeeper + Mock API
mock-api/               # Node.js mock verification service
```

## Stopping Services

```bash
# Stop the Spring Boot app: Ctrl+C

# Stop Docker containers
docker compose down
```
