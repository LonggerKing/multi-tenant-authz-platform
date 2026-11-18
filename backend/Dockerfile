# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先复制 POM 以复用依赖缓存
COPY pom.xml .
COPY platform-common/pom.xml platform-common/pom.xml
COPY platform-tenant/pom.xml platform-tenant/pom.xml
COPY platform-permission/pom.xml platform-permission/pom.xml
COPY platform-auth/pom.xml platform-auth/pom.xml
COPY platform-boot/pom.xml platform-boot/pom.xml
RUN mvn -B -DskipTests dependency:go-offline || true

COPY . .
RUN mvn -B -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /build/platform-boot/target/permission-platform.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
