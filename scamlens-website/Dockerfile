# Used only for putting ScamLens online (e.g. on Render). Not needed on your laptop.
# It installs Java 21, compiles the code and starts the website.
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN javac -d out -sourcepath src/main/java src/main/java/com/scamlens/Main.java
EXPOSE 8765
CMD ["java", "-Duser.timezone=Asia/Kolkata", "-cp", "out", "com.scamlens.Main"]
