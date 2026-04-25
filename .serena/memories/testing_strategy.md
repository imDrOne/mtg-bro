# Testing Strategy

| Component | Test Type |
|-----------|-----------|
| SQL Builder / Mapper | Integration (real DB via Testcontainers) |
| RMQ/Kafka handler | Integration |
| RMQ/Kafka producer | Integration |
| Domain Entity | Unit |
| Repository (mapping) | Unit (mock SQL Builder) |
| Use Case / Service | Unit |
| Query Repository | Unit (mock) + integration for complex queries |

- Integration tests extend `AbstractIntegrationTest` base class
- Uses Testcontainers PostgreSQL
- JUnit 5 + Mockito-Kotlin
- No network calls in unit tests, no containers in unit tests

## Task Completion Checklist
1. Run relevant tests: `./gradlew :<module>:test`
2. Full build: `./gradlew build`
3. For mcp-server changes: `./gradlew :mcp-server:installDist`
4. No linting/formatting tool configured (Kotlin compiler strict mode is the guard)
