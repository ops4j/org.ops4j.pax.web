OPS4j Pax Web
=============

[![CircleCI](https://circleci.com/gh/ops4j/org.ops4j.pax.web.svg?style=svg)](https://circleci.com/gh/ops4j/org.ops4j.pax.web)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ops4j.pax/web/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ops4j.pax/web)
[![License](https://img.shields.io/hexpm/l/plug.svg)](https://ops4j1.jira.com/wiki/display/ops4j/Licensing)

What is Pax Web
===============

* OSGi R7 Http Service Specification implementation ([OSGi CMPN R7, chapter 102](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.http.html))
* OSGi R7 Web Applications Specification implementation ([OSGi CMPN R7, chapter 128](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.war.html))
* OSGi R7 Http Whiteboard Specification implementation ([OSGi CMPN R7, chapter 140](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.http.whiteboard.html))

Pax Web extends OSGi Http Service with better servlet support, filters, listeners, error pages and JSPs and some others in order to meet the latest versions of Servlet specs.
Pax Web facilitates an easy installation of WAR bundles as well as discovery of web elements published as OSGi services. All of this beside the, standard, programmatic registration as detailed in the HTTP Service specs.

There are 3 equivalent (in terms of functionality) implementations in Pax Web:

* pax-web-jetty bundle uses [Jetty](https://www.eclipse.org/jetty/) 9.4.x
* pax-web-tomcat bundle uses [Tomcat](https://tomcat.apache.org/) 9.0.x
* pax-web-undertow bundle uses [Undertow](https://undertow.io/) 2.2.x

## Documentation

* <https://ops4j1.jira.com/wiki/spaces/paxweb/overview>
or alternative (work in progress)
* <http://ops4j.github.io/pax/web/index.html>

## Contributing

In OPS4J, everyone is invited to contribute. We don't require any paperwork or community reputation.
All we ask you is to move carefully and to clean up after yourself:

* Describe your problem or enhancement request before submitting a solution.
* Submit a [GitHub issue](https://github.com/ops4j/org.ops4j.pax.web/issues) before creating a pull request. This is required for the release notes.
* For discussions, the [mailing list](https://groups.google.com/forum/#!forum/ops4j) is more suitable than GH issues.
* Any bugfix or new feature must be covered by regression tests.



Building Pax Web
================

`mvn clean install`

NB: if you want to avoid test execution:
mvn clean install -DskipTests

Releasing Pax Web
=================

`mvn -Prelease -Darguments="-Prelease" release:prepare -DautoVersionSubmodules=true`

`mvn -Prelease -Darguments="-Prelease" -Dgoals=deploy release:perform`

Go to oss.sonatype.org and push pax-web to central.

If you want more information about releasing, please take a look on:

https://ops4j1.jira.com/wiki/spaces/ops4j/pages/12060688/Releasing
