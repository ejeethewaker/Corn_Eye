@echo off
echo Installing Firebase CLI...
npm install -g firebase-tools

echo.
echo Logging in to Firebase (browser will open)...
firebase login

echo.
echo Deploying functions...
cd /d "%~dp0functions"
firebase deploy --only functions

pause
