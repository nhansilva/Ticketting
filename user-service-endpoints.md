# User Service - API Endpoints Specification

## Overview
This document outlines all REST API endpoints for the User Service in the Ticketing System.

**Base URL**: `/api/v1/users`  
**Technology**: Spring WebFlux (Reactive)  
**Response Format**: `ApiResponse<T>`

---

## 1. Authentication & Registration

### 1.1 Register New User
- **Endpoint**: `POST /api/v1/users/register`
- **Description**: Register a new user account
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "dateOfBirth": "1990-01-15"
}
```
- **Response**: `201 Created`
```json
{
  "success": true,
  "message": "User registered successfully. Please verify your email.",
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "status": "PENDING_VERIFICATION"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### 1.2 Login
- **Endpoint**: `POST /api/v1/users/login`
- **Description**: Authenticate user and return JWT token
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "jwt-token-here",
    "refreshToken": "refresh-token-here",
    "expiresIn": 3600,
    "user": {
      "userId": "uuid",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe"
    }
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### 1.3 Refresh Token
- **Endpoint**: `POST /api/v1/users/refresh-token`
- **Description**: Refresh access token using refresh token
- **Request Body**:
```json
{
  "refreshToken": "refresh-token-here"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "token": "new-jwt-token",
    "expiresIn": 3600
  }
}
```

### 1.4 Logout
- **Endpoint**: `POST /api/v1/users/logout`
- **Description**: Logout user and invalidate tokens
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

## 2. Email Verification

### 2.1 Send Verification Email
- **Endpoint**: `POST /api/v1/users/verification/send`
- **Description**: Send email verification link
- **Request Body**:
```json
{
  "email": "user@example.com"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Verification email sent"
}
```

### 2.2 Verify Email
- **Endpoint**: `GET /api/v1/users/verification/verify`
- **Description**: Verify user email with token
- **Query Parameters**: `token={verification-token}`
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "verified": true
  }
}
```

### 2.3 Resend Verification Email
- **Endpoint**: `POST /api/v1/users/verification/resend`
- **Description**: Resend verification email
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Verification email resent"
}
```

---

## 3. Password Management

### 3.1 Forgot Password
- **Endpoint**: `POST /api/v1/users/password/forgot`
- **Description**: Request password reset link
- **Request Body**:
```json
{
  "email": "user@example.com"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Password reset link sent to your email"
}
```

### 3.2 Reset Password
- **Endpoint**: `POST /api/v1/users/password/reset`
- **Description**: Reset password with token
- **Request Body**:
```json
{
  "token": "reset-token",
  "newPassword": "NewSecurePassword123!",
  "confirmPassword": "NewSecurePassword123!"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Password reset successfully"
}
```

### 3.3 Change Password
- **Endpoint**: `PUT /api/v1/users/password/change`
- **Description**: Change password (authenticated user)
- **Headers**: `Authorization: Bearer {token}`
- **Request Body**:
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewSecurePassword123!",
  "confirmPassword": "NewSecurePassword123!"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Password changed successfully"
}
```

---

## 4. User Profile Management

### 4.1 Get Current User Profile
- **Endpoint**: `GET /api/v1/users/profile`
- **Description**: Get authenticated user's profile
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "dateOfBirth": "1990-01-15",
    "profileImage": "https://...",
    "emailVerified": true,
    "status": "ACTIVE",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

### 4.2 Get User Profile by ID
- **Endpoint**: `GET /api/v1/users/{userId}`
- **Description**: Get user profile by ID (admin or self)
- **Headers**: `Authorization: Bearer {token}`
- **Path Parameters**: `userId` (UUID)
- **Response**: `200 OK` (same as 4.1)

### 4.3 Update User Profile
- **Endpoint**: `PUT /api/v1/users/profile`
- **Description**: Update authenticated user's profile
- **Headers**: `Authorization: Bearer {token}`
- **Request Body**:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "dateOfBirth": "1990-01-15",
  "profileImage": "https://..."
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    ...
  }
}
```

### 4.4 Upload Profile Image
- **Endpoint**: `POST /api/v1/users/profile/image`
- **Description**: Upload user profile image
- **Headers**: `Authorization: Bearer {token}`, `Content-Type: multipart/form-data`
- **Request Body**: Form data with `file` field
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Profile image uploaded successfully",
  "data": {
    "imageUrl": "https://..."
  }
}
```

### 4.5 Delete Profile Image
- **Endpoint**: `DELETE /api/v1/users/profile/image`
- **Description**: Delete user profile image
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Profile image deleted successfully"
}
```

---

## 5. User Preferences

### 5.1 Get User Preferences
- **Endpoint**: `GET /api/v1/users/preferences`
- **Description**: Get user preferences
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "userId": "uuid",
    "emailNotifications": true,
    "smsNotifications": false,
    "pushNotifications": true,
    "preferredLanguage": "en",
    "timezone": "UTC",
    "currency": "USD",
    "marketingEmails": false
  }
}
```

### 5.2 Update User Preferences
- **Endpoint**: `PUT /api/v1/users/preferences`
- **Description**: Update user preferences
- **Headers**: `Authorization: Bearer {token}`
- **Request Body**:
```json
{
  "emailNotifications": true,
  "smsNotifications": false,
  "pushNotifications": true,
  "preferredLanguage": "en",
  "timezone": "UTC",
  "currency": "USD",
  "marketingEmails": false
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Preferences updated successfully",
  "data": { ... }
}
```

---

## 6. User Booking History

### 6.1 Get User Bookings
- **Endpoint**: `GET /api/v1/users/bookings`
- **Description**: Get authenticated user's booking history
- **Headers**: `Authorization: Bearer {token}`
- **Query Parameters**:
  - `page` (default: 0)
  - `size` (default: 20)
  - `status` (optional: PENDING, CONFIRMED, CANCELLED, EXPIRED)
  - `sortBy` (default: createdAt)
  - `sortDir` (default: DESC)
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "bookingId": "uuid",
        "eventId": "uuid",
        "eventName": "Concert Name",
        "eventDate": "2024-02-15T19:00:00",
        "ticketCount": 2,
        "totalAmount": 150.00,
        "status": "CONFIRMED",
        "createdAt": "2024-01-15T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 10,
    "totalPages": 1
  }
}
```

### 6.2 Get Booking Details
- **Endpoint**: `GET /api/v1/users/bookings/{bookingId}`
- **Description**: Get detailed booking information
- **Headers**: `Authorization: Bearer {token}`
- **Path Parameters**: `bookingId` (UUID)
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "bookingId": "uuid",
    "eventId": "uuid",
    "eventName": "Concert Name",
    "eventDate": "2024-02-15T19:00:00",
    "venue": "Venue Name",
    "tickets": [
      {
        "ticketId": "uuid",
        "section": "A",
        "row": "5",
        "seat": "12",
        "price": 75.00
      }
    ],
    "totalAmount": 150.00,
    "status": "CONFIRMED",
    "paymentStatus": "PAID",
    "createdAt": "2024-01-15T10:00:00"
  }
}
```

---

## 7. Account Management

### 7.1 Deactivate Account
- **Endpoint**: `POST /api/v1/users/account/deactivate`
- **Description**: Deactivate user account
- **Headers**: `Authorization: Bearer {token}`
- **Request Body**:
```json
{
  "password": "SecurePassword123!",
  "reason": "No longer using the service"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Account deactivated successfully"
}
```

### 7.2 Reactivate Account
- **Endpoint**: `POST /api/v1/users/account/reactivate`
- **Description**: Reactivate deactivated account
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Account reactivated successfully"
}
```

### 7.3 Delete Account
- **Endpoint**: `DELETE /api/v1/users/account`
- **Description**: Permanently delete user account
- **Headers**: `Authorization: Bearer {token}`
- **Request Body**:
```json
{
  "password": "SecurePassword123!",
  "confirmation": "DELETE"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Account deleted successfully"
}
```

---

## 8. User Search & Admin Operations

### 8.1 Search Users (Admin)
- **Endpoint**: `GET /api/v1/users/search`
- **Description**: Search users (admin only)
- **Headers**: `Authorization: Bearer {admin-token}`
- **Query Parameters**:
  - `email` (optional)
  - `firstName` (optional)
  - `lastName` (optional)
  - `status` (optional: ACTIVE, INACTIVE, PENDING_VERIFICATION, SUSPENDED)
  - `page` (default: 0)
  - `size` (default: 20)
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 8.2 Update User Status (Admin)
- **Endpoint**: `PUT /api/v1/users/{userId}/status`
- **Description**: Update user status (admin only)
- **Headers**: `Authorization: Bearer {admin-token}`
- **Path Parameters**: `userId` (UUID)
- **Request Body**:
```json
{
  "status": "SUSPENDED",
  "reason": "Violation of terms"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "User status updated successfully"
}
```

### 8.3 Get User Statistics (Admin)
- **Endpoint**: `GET /api/v1/users/statistics`
- **Description**: Get user statistics (admin only)
- **Headers**: `Authorization: Bearer {admin-token}`
- **Query Parameters**: `startDate`, `endDate` (optional)
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "totalUsers": 1000,
    "activeUsers": 850,
    "pendingVerification": 50,
    "suspendedUsers": 10,
    "newUsersToday": 25,
    "newUsersThisMonth": 200
  }
}
```

---

## 9. Health & Status

### 9.1 Health Check
- **Endpoint**: `GET /api/v1/users/health`
- **Description**: Service health check
- **Response**: `200 OK`
```json
{
  "status": "UP",
  "database": "UP",
  "redis": "UP",
  "kafka": "UP"
}
```

### 9.2 Check Email Availability
- **Endpoint**: `GET /api/v1/users/check-email`
- **Description**: Check if email is available
- **Query Parameters**: `email={email}`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "email": "user@example.com",
    "available": true
  }
}
```

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "success": false,
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": [
    {
      "field": "email",
      "message": "Email is required",
      "rejectedValue": null
    }
  ],
  "timestamp": "2024-01-15T10:30:00",
  "path": "/api/v1/users/register"
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "errorCode": "UNAUTHORIZED",
  "message": "Invalid or expired token",
  "timestamp": "2024-01-15T10:30:00"
}
```

### 403 Forbidden
```json
{
  "success": false,
  "errorCode": "FORBIDDEN",
  "message": "Insufficient permissions",
  "timestamp": "2024-01-15T10:30:00"
}
```

### 404 Not Found
```json
{
  "success": false,
  "errorCode": "USER_NOT_FOUND",
  "message": "User not found",
  "timestamp": "2024-01-15T10:30:00"
}
```

### 409 Conflict
```json
{
  "success": false,
  "errorCode": "EMAIL_ALREADY_EXISTS",
  "message": "Email already registered",
  "timestamp": "2024-01-15T10:30:00"
}
```

### 429 Too Many Requests
```json
{
  "success": false,
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later.",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## Notes

1. **Authentication**: Most endpoints require JWT token in `Authorization` header
2. **Rate Limiting**: All endpoints are subject to rate limiting (configured per endpoint)
3. **Validation**: All input data is validated using Bean Validation
4. **Pagination**: List endpoints support pagination with default page size of 20
5. **Sorting**: List endpoints support sorting by various fields
6. **Caching**: User profile data is cached in Redis for performance
7. **Events**: User registration, updates, and status changes are published to Kafka

---

## Implementation Priority

### Phase 1 (Core Functionality)
1. Register New User
2. Login
3. Get Current User Profile
4. Update User Profile
5. Email Verification (Send & Verify)
6. Forgot/Reset Password

### Phase 2 (Enhanced Features)
7. Refresh Token
8. Logout
9. Change Password
10. User Preferences
11. Profile Image Upload
12. Get User Bookings

### Phase 3 (Advanced Features)
13. Account Deactivation/Reactivation
14. Delete Account
15. Admin Operations
16. User Statistics

