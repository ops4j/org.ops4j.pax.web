@echo off
set _VM_=--vmopts=-DaQute.fileinstall.dir=demo.minimal/target/classes/config
if "%1"=="debug" set _VM_=%_VM_% -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
if not "%_VM_%"=="" set _VM_="%_VM_%"
java -jar %USERPROFILE%\.m2\repository\org\ops4j\pax\runner\0.3.2-SNAPSHOT\runner-0.3.2-SNAPSHOT.jar --platform=choose org.ops4j.pax.web %_VM_% demo.minimal 0.1.0-SNAPSHOT
