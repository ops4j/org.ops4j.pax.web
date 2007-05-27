#!/bin/sh

java -jar $HOME/.m2/repository/org/ops4j/pax/runner/0.3.2-SNAPSHOT/runner-0.3.2-SNAPSHOT.jar --platform=felix --vmopts="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" org.ops4j.pax.web demo 0.2.0-SNAPSHOT
