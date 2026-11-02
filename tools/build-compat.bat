@echo off
rem Rebuild libs/wheelchair-compat.jar from src/compat/java (ported InfiniStack, MIT).
rem Classpath: forgeBin + lwjgl + jsr305 + log4j + netty + unimixins + compat-libs (modpack jars).
setlocal
set CACHE=%USERPROFILE%\.gradle\caches
set FG=%CACHE%\minecraft\net\minecraftforge\forge\1.7.10-10.13.4.1614-1.7.10\forgeBin-1.7.10-10.13.4.1614-1.7.10.jar
set LWJGL=%CACHE%\modules-2\files-2.1\org.lwjgl.lwjgl\lwjgl\2.9.1\f58c5aabcef0e41718a564be9f8e412fff8db847\lwjgl-2.9.1.jar
set JSR=%CACHE%\modules-2\files-2.1\com.google.code.findbugs\jsr305\2.0.1\516c03b21d50a644d538de0f0369c620989cd8f0\jsr305-2.0.1.jar
set L4J=%CACHE%\modules-2\files-2.1\org.apache.logging.log4j\log4j-api\2.0-beta9\1dd66e68cccd907880229f9e2de1314bd13ff785\log4j-api-2.0-beta9.jar
set NETTY=%CACHE%\modules-2\files-2.1\io.netty\netty-all\4.0.10.Final\9e50bd52ffe257a0e2cd8d971688d6ce7d174325\netty-all-4.0.10.Final.jar
set CP=%FG%;%LWJGL%;%JSR%;%L4J%;%NETTY%;%~dp0..\libs\unimixins-all-1.7.10-0.2.1.jar;
for %%j in ("%~dp0..\compat-libs\*.jar") do call set CP=%%CP%%;%%~fj
if exist "%~dp0..\build\compat-classes" rmdir /s /q "%~dp0..\build\compat-classes"
mkdir "%~dp0..\build\compat-classes"
cd /d "%~dp0..\src\compat\java"
dir /s /b *.java > "%TEMP%\compat-srcs.txt"
javac -proc:none -source 1.8 -target 1.8 -encoding UTF-8 -nowarn -d "%~dp0..\build\compat-classes" -cp "%CP%" "%TEMP%\compat-srcs.txt"
if errorlevel 1 ( echo COMPILE FAILED & exit /b 1 )
cd /d "%~dp0..\build\compat-classes"
jar cf ..\..\libs\wheelchair-compat.jar .
echo OK: libs\wheelchair-compat.jar rebuilt
