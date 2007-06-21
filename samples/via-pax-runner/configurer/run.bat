@echo off
if "%1"=="debug" set _VM_=%_VM_% -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
java -jar %USERPROFILE%\.m2\repository\org\ops4j\pax\runner\0.3.2\runner-0.3.2.jar --platform=choose org.ops4j.pax.web.samples.via-pax-runner %_VM_% configurer 0.1.0-SNAPSHOT
