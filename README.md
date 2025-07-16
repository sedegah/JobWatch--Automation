# Job Watch — AI-Powered Remote Job Aggregator with MongoDB

**Job Watch** is a Java-based job aggregation and enrichment system that scrapes real tech jobs from online portals, deduplicates them, enriches each job with AI-generated tags using **Groq's LLaMA 3**, and stores the final result in **MongoDB**.

---

## Features

- Aggregates remote software/tech jobs from:

  - **RemoteOK** (HTML scraping)
  - **Jobgether** (HTML scraping)
  - **JSearch (RapidAPI)** — curated tech jobs via API
  - **Adzuna** (optional API-based feed)

- Uses **Groq's LLaMA 3** API to generate relevant tags per job
- Handles rate limits and fallbacks gracefully
- Deduplicates jobs based on apply link or (title + company + location)
- Persists final enriched job listings into **MongoDB**

---

## Technology Stack

- **Java 17+**
- **Spring Boot 3**
- **Spring Data MongoDB**
- **Groq LLaMA 3 API**
- **Jsoup (HTML scraping)**
- **OkHttp (HTTP requests)**
- **Maven**

---

## Example Output (Stored in MongoDB)

```json
{
  "_id": "66a2c3e4f1...",
  "title": "Remote Backend Developer",
  "company": "Acme Inc.",
  "location": "Remote - USA",
  "description": "...",
  "applyUrl": "https://remoteok.io/remote-jobs/12345",
  "source": "RemoteOK",
  "postedDate": "2025-07-16T21:10:00",
  "remote": true,
  "tags": ["Java", "Spring Boot", "Backend", "Remote", "REST APIs"]
}
```

---

## Setup Instructions

### 1. Prerequisites

- Java 17 or higher
- Maven
- MongoDB (local or Atlas)
- Accounts + API keys:

  - [Groq Console](https://console.groq.com) (for LLaMA-3 inference)
  - [RapidAPI - JSearch](https://rapidapi.com) (for tech job APIs)
  - [Adzuna API](https://developer.adzuna.com) (optional)

---

### 2. Clone Repository

```bash
git clone https://github.com/your-username/jobwatch.git
cd jobwatch
```

---

### 3. Configure Application Properties

Create or edit `src/main/resources/application.properties`:

```properties
# MongoDB (local or remote)
spring.data.mongodb.uri=mongodb://localhost:27017/jobwatchdb

# Groq API
groq.api.key=your_groq_api_key_here

# RapidAPI (JSearch)
rapidapi.key=your_rapidapi_key_here
rapidapi.host=jsearch.p.rapidapi.com

# Adzuna (optional)
adzuna.app_id=your_adzuna_app_id
adzuna.api_key=your_adzuna_api_key

# Job source toggles
jobwatch.fetch.remoteok.enabled=true
jobwatch.fetch.jsearch.enabled=true
jobwatch.fetch.adzuna.enabled=false
```

---

### 4. Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

> On startup, jobs will be fetched, tagged, deduplicated, and stored in MongoDB.

---

### 5. View in MongoDB

Use MongoDB Compass or the shell to inspect jobs:

```bash
use jobwatchdb
db.jobs.find().pretty()
```

---

## Code Structure

```
src/
├── main/
│   ├── java/
│   │   └── com.jobwatch/
│   │       ├── JobWatchApplication.java
│   │       ├── model/
│   │       │   └── Job.java
│   │       ├── repository/
│   │       │   └── JobRepository.java
│   │       └── service/
│   │           ├── JobFetcherService.java
│   │           └── GroqService.java
│   └── resources/
│       └── application.properties
```

---

## Supported Commands (Developer Use)

| Action                         | Description                               |
| ------------------------------ | ----------------------------------------- |
| `mvn spring-boot:run`          | Run the entire job fetch + tag pipeline   |
| `mvn clean install`            | Build the app                             |
| `JobFetcherService.fetchAll()` | Programmatically fetch and tag jobs       |
| `GroqService.tag(jobs)`        | Tag any list of `Job` objects with LLaMA3 |

---

## Extending the System

- Add a REST API (`/jobs`) to query stored jobs
- Add a simple frontend (e.g., React or Thymeleaf) to browse jobs
- Add keyword/job-title filtering during fetch
- Integrate with resume matcher or alert bot

---

## License

MIT License
