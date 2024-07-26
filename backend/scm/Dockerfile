# Uporabi uradno sliko JDK 21 za gradnjo
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Nastavi delovni direktorij
WORKDIR /app

# Kopiraj pom.xml in prenesi odvisnosti
COPY pom.xml .

# Prenesi odvisnosti
RUN mvn dependency:go-offline

# Kopiraj ostale datoteke aplikacije
COPY src ./src

# Zgradi aplikacijo
RUN mvn clean package -DskipTests

# Uporabi uradno sliko JDK 21 za končno sliko
FROM eclipse-temurin:21-jdk-alpine

# Nastavi delovni direktorij
WORKDIR /app

# Kopiraj zgrajeno aplikacijo iz gradbenega koraka
COPY --from=build /app/target/*.jar app.jar

# Nastavi zagonski ukaz
ENTRYPOINT ["java", "-jar", "app.jar"]