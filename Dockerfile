# Expense Tracker JSP: build the WAR with Maven, then run it on Tomcat 10.1 with Tesseract
# installed for receipt scanning. Used by Render (see render.yaml); works on any Docker host.

# 1. Build. Tests run locally with `mvnw test`; they are skipped here so a deploy only
#    depends on the code compiling.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests package

# 2. Run.
FROM tomcat:10.1-jdk21-temurin-noble

# Tess4J loads the system Tesseract library; the -dev package provides the unversioned
# libtesseract.so it looks for. tesseract-ocr-eng is the English language data.
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata

# Let the JVM use most of the container's memory (Tomcat's default is a quarter of it).
ENV CATALINA_OPTS="-XX:MaxRAMPercentage=70"

# Serve the app at the site root, as run-web.ps1 does, without Tomcat's sample apps.
RUN rm -rf webapps/* webapps.dist
COPY --from=build /src/target/expense-tracker.war webapps/ROOT.war

# Render terminates HTTPS in front of the container. Trusting its X-Forwarded-Proto header
# lets Tomcat treat requests as secure, so the session cookie is marked Secure.
RUN sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.RemoteIpValve" protocolHeader="X-Forwarded-Proto" />\n      </Host>#' conf/server.xml

# Listen on the port Render assigns in $PORT (8080 elsewhere).
EXPOSE 8080
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" conf/server.xml && exec catalina.sh run"]
