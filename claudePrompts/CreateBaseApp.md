Create a Spring Boot 4.0.5 microservice called "ClaudeSportBets" with the following specifications:

- Java 25, Maven build system
- Group ID: com.skmcore, Artifact ID: order-service
- Layered package structure under com.skmcore.orderservice:
    - controller
    - service
    - repository
    - model (entities)
    - dto (request/response)
    - mapper
    - config
    - exception
    - event

Dependencies to include in pom.xml:
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-boot-starter-actuator
- spring-boot-starter-security
- springdoc-openapi-starter-webmvc-ui (v3.0.2)
- liquibase-core
- postgresql driver
- lombok
- mapstruct (1.6.3) and mapstruct-processor
- spring-boot-starter-test
- testcontainers with postgresql module
- spring-retry and spring-boot-starter-aop
- micrometer-registry-prometheus
- net.logstash.logback:logstash-logback-encoder (v9.0) for structured logging

Create application.properties with profiles:
- default/dev: H2 in-memory for quick local runs, logging level DEBUG
- staging: MySQL connection using env vars (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD), connection pool of 10
- prod: same as staging but connection pool of 30, logging level WARN
