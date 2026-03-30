# XeNow - Vehicle Rental Management System

## Setup

1. Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

2. Update `.env` with your actual credentials:
```properties
DB_PASSWORD=your_actual_password
FPT_AI_API_KEY=your_actual_api_key
```

3. Run the application:
```bash
./mvnw spring-boot:run
```

## Environment Variables

- `DB_URL`: Database connection URL
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `FPT_AI_API_KEY`: FPT.AI eKYC API key

## Default Admin Account

- Username: `admin`
- Password: `admin123`
