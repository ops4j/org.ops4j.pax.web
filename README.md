What is Pax Web
===============

OSGi R4 Http Service and Web Applications (OSGi Enterprise Release chapter 128) implementation using Jetty 9 and Tomcat 7.   
Pax Web extends OSGi Http Service with better servlet support, filters, listeners, error pages and JSPs and some others in order to meet the latest versions of Servlet specs.    
Pax Web facilitates an easy installation of WAR bundles as well as discovery of web elements published as OSGi services. All of this beside the, standard, programmatic registration as detailed in the HTTP Service specs.

Currently it supports the following:    
* Servlet 3.0   
* JSP 1.1.2   
* JSF 2.1   
* Jetty 9.x   
* Tomcat 7.x  
* support of CDI (through Pax-CDI)  
* support of only Servlet 3.0 annotated Servlets in JAR   

Building Pax Web
================

mvn clean install

NB: if you want to avoid test execution:
mvn clean install -DskipTests

Releasing Pax Web
=================

mvn -Prelease -Darguments="-Prelease" release:prepare -DautoVersionSubmodules=true

mvn -Prelease -Darguments="-Prelease" -Dgoals=deploy release:perform

Go to oss.sonatype.org and push pax-web to central.

If you want more information about releasing, please take a look on:

http://team.ops4j.org/wiki/display/ops4j/Releasing
