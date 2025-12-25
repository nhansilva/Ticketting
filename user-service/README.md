# User Service

User management and authentication service for the Ticketing System.

## Features

- User registration and authentication
- JWT-based authentication
- Email verification
- Password reset functionality
- User profile management
- User preferences
- Account management (activate/deactivate/delete)

## Technology Stack

- Spring Boot 4.0.1
- Spring WebFlux (Reactive)
- R2DBC (Reactive Database Access)
- PostgreSQL
- Redis (Caching)
- Kafka (Event Streaming)
- JWT (Authentication)

## Database Schema

### Users Table
- User basic information
- Authentication data
- Profile information

### User Preferences Table
- Notification preferences
- Language, timezone, currency settings

### Verification Tokens Table
- Email verification tokens
- Password reset tokens

## API Endpoints

See `user-service-endpoints.md` in the root directory for complete API documentation.

## Configuration

### Application Properties

- **Database**: Configured in `application.yml`
- **JWT**: Secret key and expiration settings
- **Redis**: Connection settings
- **Kafka**: Producer/consumer configuration

### Environment Variables

```bash
SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/user_db
SPRING_R2DBC_USERNAME=ticketing
SPRING_R2DBC_PASSWORD=ticketing123
JWT_SECRET=your-secret-key-here
```

## Running the Service

1. **Start infrastructure services:**
```bash
docker-compose up -d
```

2. **Create database:**
```bash
psql -U ticketing -d ticketing_db -c "CREATE DATABASE user_db;"
```

3. **Run migrations:**
The database schema will be created automatically on first run, or you can run the migration script manually.

4. **Run the service:**
```bash
cd user-service
mvn spring-boot:run
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=UserServiceTest
```

## Security

- JWT tokens for authentication
- BCrypt password encoding
- Rate limiting (via common-utils)
- Input validation

## TODO

- [ ] Implement refresh token validation
- [ ] Integrate with actual email service
- [ ] Add admin endpoints
- [ ] Add user search and pagination
- [ ] Add user statistics
- [ ] Add profile image upload functionality
- [ ] Add booking history integration
- [ ] Add comprehensive error handling
- [ ] Add unit and integration tests

