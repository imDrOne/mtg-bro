FROM liquibase/liquibase:latest
ARG CHANGELOG_DIR
COPY ${CHANGELOG_DIR} /liquibase/changelog/
RUN curl -L https://jdbc.postgresql.org/download/postgresql-42.7.3.jar -o /liquibase/lib/postgresql.jar
