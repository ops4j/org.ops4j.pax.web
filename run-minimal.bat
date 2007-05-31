if "%1"=="debug" set _DEBUG_=--vmopts="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
java -jar %USERPROFILE%\.m2\repository\org\ops4j\pax\runner\0.3.2-SNAPSHOT\runner-0.3.2-SNAPSHOT.jar --platform=choose org.ops4j.pax.web %_DEBUG_% demo.minimal 0.1.0-SNAPSHOT
