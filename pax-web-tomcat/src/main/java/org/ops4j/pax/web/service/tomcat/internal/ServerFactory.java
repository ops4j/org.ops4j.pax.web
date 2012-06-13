package org.ops4j.pax.web.service.tomcat.internal;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/9/12
 * Time: 7:46 AM
 * To change this template use File | Settings | File Templates.
 */
interface ServerFactory {
    ServerWrapper newServer();
}
