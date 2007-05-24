Bundle-Activator: org.ops4j.pax.webserver.Activator

Private-Package: org.ops4j.pax.webserver

Import-Package: org.knopflerfish.service.log; provider="paxlogging";version="[1.1.0,2.0.0)", \
                org.osgi.framework; version="[1.0.0, 2.0.0)", \
                org.osgi.service.cm; version="[1.0.0, 2.0.0)", \
                javax.servlet; version=2.5, \
                javax.servlet.http; version=2.5, \
                org.osgi.service.http; version=1.2, \
                javax.net.ssl, \
                javax.security.cert, \
                javax.xml.parsers, \
                org.xml.sax, \
                org.xml.sax.helpers

Export-Package: javax.servlet; version=2.5, \
                javax.servlet.http; version=2.5, \
                org.mortbay.component; version=6.1, \
                org.mortbay.io; version=6.1, \
                org.mortbay.io.bio; version=6.1, \
                org.mortbay.io.nio; version=6.1, \
                org.mortbay.jetty; version=6.1, \
                org.mortbay.jetty.bio; version=6.1, \
                org.mortbay.jetty.deployer; version=6.1, \
                org.mortbay.jetty.handler; version=6.1, \
                org.mortbay.jetty.nio; version=6.1, \
                org.mortbay.jetty.security; version=6.1, \
                org.mortbay.jetty.servlet; version=6.1, \
                org.mortbay.jetty.webapp; version=6.1, \
                org.mortbay.log; version=6.1, \
                org.mortbay.resource; version=6.1, \
                org.mortbay.thread; version=6.1, \
                org.mortbay.util; version=6.1, \
                org.mortbay.util.ajax; version=6.1, \
                org.mortbay.xml; version=6.1, \
                org.osgi.service.http; version=6.1


