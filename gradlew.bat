@ECHO OFF
SETLOCAL

SET APP_HOME=%~dp0
SET WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
SET WRAPPER_URL=https://repo.gradle.org/gradle/libs-releases-local/org/gradle/gradle-wrapper/8.7/gradle-wrapper-8.7.jar

IF NOT EXIST "%WRAPPER_JAR%" (
    ECHO Gradle wrapper JAR missing. Attempting download...
    IF EXIST "%ProgramFiles%\PowerShell\7\pwsh.exe" (
        "%ProgramFiles%\PowerShell\7\pwsh.exe" -NoProfile -Command "New-Item -ItemType Directory -Path (Split-Path '%WRAPPER_JAR%') -Force | Out-Null; Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
    ) ELSE (
        powershell -NoProfile -Command "New-Item -ItemType Directory -Path (Split-Path '%WRAPPER_JAR%') -Force | Out-Null; Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
    )
)

SET JAVA_EXE=java.exe
IF NOT "%JAVA_HOME%"=="" (
    SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*

ENDLOCAL
