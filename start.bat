@echo off
cd /d "%~dp0"
echo Starting FoodRescue...
echo When the server starts, open http://localhost:8080 in your browser.
mvn spring-boot:run
pause
