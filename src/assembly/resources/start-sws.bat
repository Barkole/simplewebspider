@REM
@REM Simple Web Spider - <http://simplewebspider.sourceforge.net/>
@REM Copyright (C) 2009  <berendona@users.sourceforge.net>
@REM
@REM This program is free software: you can redistribute it and/or modify
@REM it under the terms of the GNU General Public License as published by
@REM the Free Software Foundation, either version 3 of the License, or
@REM (at your option) any later version.
@REM
@REM This program is distributed in the hope that it will be useful,
@REM but WITHOUT ANY WARRANTY; without even the implied warranty of
@REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
@REM GNU General Public License for more details.
@REM
@REM You should have received a copy of the GNU General Public License
@REM along with this program.  If not, see <http://www.gnu.org/licenses/>.
@REM
@echo off
setlocal EnableDelayedExpansion

SET JAVA=java
SET JAVANMAIN=simplespider.simplespider.Main
SET JAVA_ARGS=-Xmx512m -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Djava.util.logging.config.file=simple-web-spider.java-logger.properties

// Determine all available jars for classpath
SET JARS=
FOR %%j IN (.\lib\*.jar) DO (
	if "!JARS!"=="" SET JARS=%%j
	if "!JARS!" NEQ "" SET JARS=!JARS!;%%j
)

SET PARAMETER=

SET CMDLINE=%JAVA% %JAVA_ARGS% -classpath %JARS% %JAVANMAIN% %PARAMETER%

::echo %CMDLINE%
call %CMDLINE%

endlocal
