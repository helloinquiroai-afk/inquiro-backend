Inquiro MVP UI

Open http://localhost:8080/ after starting the Spring Boot application.

The UI uses the existing website conversation endpoint:
POST /api/conversations/message
DELETE /api/conversations/{sessionId}

The browser generates and persists a demo session ID in localStorage.