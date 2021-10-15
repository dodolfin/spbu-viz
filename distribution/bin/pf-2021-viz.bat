@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  pf-2021-viz startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and PF_2021_VIZ_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\pf-2021-viz-1.0-SNAPSHOT.jar;%APP_HOME%\lib\kotlinx-coroutines-swing-1.4.1.jar;%APP_HOME%\lib\kotlin-csv-jvm-1.1.0.jar;%APP_HOME%\lib\clikt-jvm-3.3.0.jar;%APP_HOME%\lib\kotlinx-coroutines-core-jvm-1.5.1.jar;%APP_HOME%\lib\kotlin-logging-jvm-2.0.10.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.5.31.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.5.31.jar;%APP_HOME%\lib\kotlin-stdlib-1.5.31.jar;%APP_HOME%\lib\batik-all-1.14.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.5.31.jar;%APP_HOME%\lib\batik-rasterizer-ext-1.14.jar;%APP_HOME%\lib\batik-rasterizer-1.14.jar;%APP_HOME%\lib\batik-svgrasterizer-1.14.jar;%APP_HOME%\lib\batik-codec-1.14.jar;%APP_HOME%\lib\batik-squiggle-ext-1.14.jar;%APP_HOME%\lib\batik-extension-1.14.jar;%APP_HOME%\lib\batik-slideshow-1.14.jar;%APP_HOME%\lib\batik-squiggle-1.14.jar;%APP_HOME%\lib\batik-svgbrowser-1.14.jar;%APP_HOME%\lib\batik-swing-1.14.jar;%APP_HOME%\lib\batik-svgpp-1.14.jar;%APP_HOME%\lib\batik-transcoder-1.14.jar;%APP_HOME%\lib\batik-bridge-1.14.jar;%APP_HOME%\lib\batik-script-1.14.jar;%APP_HOME%\lib\batik-anim-1.14.jar;%APP_HOME%\lib\batik-gvt-1.14.jar;%APP_HOME%\lib\batik-svg-dom-1.14.jar;%APP_HOME%\lib\batik-parser-1.14.jar;%APP_HOME%\lib\batik-ttf2svg-1.14.jar;%APP_HOME%\lib\batik-svggen-1.14.jar;%APP_HOME%\lib\batik-awt-util-1.14.jar;%APP_HOME%\lib\batik-dom-1.14.jar;%APP_HOME%\lib\batik-css-1.14.jar;%APP_HOME%\lib\batik-gui-util-1.14.jar;%APP_HOME%\lib\batik-xml-1.14.jar;%APP_HOME%\lib\batik-util-1.14.jar;%APP_HOME%\lib\batik-constants-1.14.jar;%APP_HOME%\lib\batik-ext-1.14.jar;%APP_HOME%\lib\batik-i18n-1.14.jar;%APP_HOME%\lib\batik-shared-resources-1.14.jar;%APP_HOME%\lib\xml-apis-ext-1.3.04.jar;%APP_HOME%\lib\xmlgraphics-commons-2.6.jar;%APP_HOME%\lib\xalan-2.7.2.jar;%APP_HOME%\lib\serializer-2.7.2.jar;%APP_HOME%\lib\xml-apis-1.4.01.jar;%APP_HOME%\lib\commons-io-1.3.1.jar;%APP_HOME%\lib\commons-logging-1.0.4.jar;%APP_HOME%\lib\slf4j-api-1.7.29.jar


@rem Execute pf-2021-viz
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %PF_2021_VIZ_OPTS%  -classpath "%CLASSPATH%" MainKt %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable PF_2021_VIZ_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%PF_2021_VIZ_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
