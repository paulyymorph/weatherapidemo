# Weather Demo API

Simple Spring Boot API that returns weather for a city using external providers with fallback support and a short in-memory cache.

## Features

- REST endpoint: `GET /v1/weather?city=<city>`
- Provider fallback (tries next provider if one fails)
- In-memory cache with 5-second TTL
- Configurable provider URLs, API keys, and JSON mappings

## Prerequisites

- Java 25
- Internet connection (required to call weather providers)
- API keys for at least one configured provider:
  - `WEATHER_PROVIDER_1_API_KEY` (weatherstack)
  - `WEATHER_PROVIDER_2_API_KEY` (openweathermap)

## Install

1. Clone the repository and enter the project folder.
2. Ensure the Maven wrapper is executable:

```bash
chmod +x mvnw
```

3. (Optional) Build the project:

```bash
./mvnw clean package
```

## Configure API Keys

The app reads provider keys from environment variables in `src/main/resources/application.yaml`.

### Linux/macOS (current shell)

```bash
export WEATHER_PROVIDER_1_API_KEY="your_weatherstack_key"
export WEATHER_PROVIDER_2_API_KEY="your_openweathermap_key"
```

### Linux/macOS (`.env` file)

If you keep keys in a `.env` file:

```bash
set -a
source .env
set +a
```

### Windows PowerShell

```powershell
$env:WEATHER_PROVIDER_1_API_KEY="your_weatherstack_key"
$env:WEATHER_PROVIDER_2_API_KEY="your_openweathermap_key"
```

## Run The App

The API runs on port `8082`.

### Run with Maven wrapper

```bash
./mvnw spring-boot:run
```

### Run packaged artifact

```bash
./mvnw clean package
java -jar target/weatherdemo-0.0.1-SNAPSHOT.war
```

## API Usage

Base URL:

```text
http://localhost:8082
```

### Get weather for a city

```bash
curl "http://localhost:8082/v1/weather?city=Lisbon"
```

Example `200 OK` response:

```json
{
  "wind_speed": 4,
  "temperature_degrees": 23
}
```

### Missing city parameter

```bash
curl "http://localhost:8082/v1/weather"
```

Example `400 Bad Request` response:

```json
{
  "message": "City parameter is required and cannot be empty"
}
```

### Providers unavailable

Example `503 Service Unavailable` response:

```json
{
  "message": "Weather providers unavailable"
}
```

## Run Tests

```bash
./mvnw test
```
