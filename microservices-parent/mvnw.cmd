@echo off
:: Maven Wrapper script for Windows

setlocal

set "MAVEN_WRAPPER_PROPERTIES=%~dp0.mvn\wrapper\maven-wrapper.properties"

:: Read distributionUrl from properties file
for /f "tokens=2 delims==" %%a in ('findstr "distributionUrl" "%MAVEN_WRAPPER_PROPERTIES%"') do set "DISTRIBUTION_URL=%%a"

:: Extract Maven version from URL  (e.g. apache-maven-3.9.6-bin.zip -> 3.9.6)
for /f "tokens=3 delims=-" %%a in ("%DISTRIBUTION_URL:apache-maven-=apache-maven-%") do (
  set "RAW=%%a"
)
:: Strip -bin.zip suffix
for /f "tokens=1 delims=-" %%a in ("%RAW%") do set "MAVEN_VERSION=%%a"

set "MAVEN_HOME_PARENT=%USERPROFILE%\.m2\wrapper\dists"
set "MAVEN_DIST_NAME=apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%MAVEN_HOME_PARENT%\%MAVEN_DIST_NAME%\%MAVEN_DIST_NAME%"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Maven %MAVEN_VERSION%...
    set "DIST_DIR=%MAVEN_HOME_PARENT%\%MAVEN_DIST_NAME%"
    if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
    set "DIST_ZIP=%DIST_DIR%\%MAVEN_DIST_NAME%-bin.zip"
    powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%DIST_ZIP%'"
    powershell -Command "Expand-Archive -Path '%DIST_ZIP%' -DestinationPath '%DIST_DIR%' -Force"
    del /f /q "%DIST_ZIP%"
    echo Maven %MAVEN_VERSION% installed at %MAVEN_HOME%
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
endlocal
