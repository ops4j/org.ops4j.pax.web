Pax Web with Undertow
=====================

OSGi Web Applications based on Undertow. Experimental feature branch of Pax Web.
See the master branch for the stable version of Pax Web with Jetty and Tomcat.

Functional Goals
----------------

  * Implement Web Applications Specification 1.0 (OSGi Enterprise 5, Section 128).
  * Implement Http Service Specification 1.2 (OSGi Enterprise 5, Section 102).
  * Support additional features of Servlet 3.1 Specification.
  * Support JSF, CDI and JSP in Web Application Bundles.
  * Support Equinox and Felix (OSGi 5.0.0 or higher)
  * Karaf Features (3.0.1 or higher)


Design Goals
------------

  * Integrate Undertow servlet container (or an OSGified version).
  * Integrate JSF 2.2 reference implementation (Mojarra).
  * Service dependency tracking based on Declarative Services (including DS annotations).
  * Simplified interaction with Pax CDI.
  
Non-Goals for the short term
----------------------------

  * Support for Jetty or Tomcat (but still provide a SPI for other web containers)
  * API compatibility with Pax Web <= 4.x.
  
Long Term Goal
--------------

  * Merge with master so that a future Pax Web release will support Jetty, Tomcat and Undertow  
  
License
-------

  * Apache Software License, v2.
  
Feedback and Questions
----------------------

Please send e-mail to opsj4@googlegroups.com.
