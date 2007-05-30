package org.ops4j.pax.web.service.internal.ng;

import org.mortbay.jetty.Connector;

interface JettyFactory
{
    JettyServer createServer();
    Connector createConnector(int port);
}
