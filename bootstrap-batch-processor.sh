#!/usr/bin/env bash
mvn -U io.quarkus:quarkus-maven-plugin:create \
    -DprojectGroupId=org.cockroachlabs.simulator \
    -DprojectArtifactId=batch-processor \
    -DpackageName="ord.cockroachlabs.simulator" \
    -Dextensions="jdbc-postgresql, hibernate-orm-panache, rest-csrf"