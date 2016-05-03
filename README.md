OPS4j Pax Web
=============

[![Build Status](https://travis-ci.org/ops4j/org.ops4j.pax.web.svg?branch=master)](https://travis-ci.org/ops4j/org.ops4j.pax.web)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ops4j.pax/web/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ops4j.pax/web)
[![License](https://img.shields.io/hexpm/l/plug.svg)](https://ops4j1.jira.com/wiki/display/ops4j/Licensing)

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

## Documentation

* <http://team.ops4j.org/wiki/display/PAXEXAM4/Documentation>

## Contributing

In OPS4J, everyone is invited to contribute. We don't require any paperwork or community reputation.
All we ask you is to move carefully and to clean up after yourself: 

* Describe your problem or enhancement request before submitting a solution.
* Submit a JIRA issue before creating a pull request. This is required for the release notes.
* For discussions, the mailing list is more suitable than JIRA.
* Any bugfix or new feature must be covered by regression tests.

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
