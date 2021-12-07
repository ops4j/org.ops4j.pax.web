/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.undertow.internal.configuration;

import java.io.StringWriter;
import java.util.Deque;
import java.util.LinkedList;

import org.ops4j.pax.web.service.undertow.configuration.model.Interface;
import org.ops4j.pax.web.service.undertow.configuration.model.IoSubsystem;
import org.ops4j.pax.web.service.undertow.configuration.model.SecurityRealm;
import org.ops4j.pax.web.service.undertow.configuration.model.Server;
import org.ops4j.pax.web.service.undertow.configuration.model.ServletContainer;
import org.ops4j.pax.web.service.undertow.configuration.model.SocketBinding;
import org.ops4j.pax.web.service.undertow.configuration.model.UndertowConfiguration;
import org.ops4j.pax.web.service.undertow.configuration.model.UndertowSubsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class UnmarshallingContentHandler implements ContentHandler {

	public static final Logger LOG = LoggerFactory.getLogger(UnmarshallingContentHandler.class);

	public static final String NS_PAXWEB_UNDERTOW = "urn:org.ops4j.pax.web:undertow:1.1";
	public static final String NS_IO = "urn:jboss:domain:io:3.0";
	public static final String NS_UNDERTOW = "urn:jboss:domain:undertow:12.0";
	public static final String NS_WILDFLY = "urn:jboss:domain:17.0";

	private Locator locator;

	private UndertowConfiguration configuration;

	private final Deque<Object> stack = new LinkedList<>();
	private StringWriter text;

	public UndertowConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	@Override
	public void startDocument() {
		configuration = new UndertowConfiguration();
	}

	@Override
	public void endDocument() {
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) {
	}

	@Override
	public void endPrefixMapping(String prefix) {
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (uri == null) {
			throw new SAXException("Unexpected element \"" + localName + "\" without XML namespace");
		}
		switch (uri) {
			case NS_PAXWEB_UNDERTOW:
				handlePaxWebElement(localName, atts, true);
				break;
			case NS_UNDERTOW:
				handleUndertowElement(localName, atts, true);
				break;
			case NS_WILDFLY:
				handleWildflyElement(localName, atts, true);
				break;
			case NS_IO:
				handleIOElement(localName, atts, true);
				break;
			default:
				throw new SAXException("Unexpected element \"{" + uri + "} + localName.");
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (uri == null) {
			throw new SAXException("Unexpected end of element \"" + localName + "\" without XML namespace");
		}
		switch (uri) {
			case NS_PAXWEB_UNDERTOW:
				handlePaxWebElement(localName, null, false);
				break;
			case NS_UNDERTOW:
				handleUndertowElement(localName, null, false);
				break;
			case NS_WILDFLY:
				handleWildflyElement(localName, null, false);
				break;
			case NS_IO:
				handleIOElement(localName, null, false);
				break;
			default:
				throw new SAXException("Unexpected end of element \"{" + uri + "} + localName.");
		}
	}

	private void handlePaxWebElement(String localName, Attributes atts, boolean start) throws SAXParseException {
		switch (localName) {
			case "security-realm":
				if (start) {
					stack.push(SecurityRealm.create(ParserUtils.toMap(atts)));
				} else {
					configuration.getSecurityRealms().add(ParserUtils.ensureStack(stack, SecurityRealm.class, localName, locator, true));
				}
				break;
			case "interface":
				if (start) {
					stack.push(Interface.create(ParserUtils.toMap(atts)));
				} else {
					configuration.getInterfaces().add(ParserUtils.ensureStack(stack, Interface.class, localName, locator, true));
				}
				break;
			case "socket-binding":
				if (start) {
					stack.push(SocketBinding.create(ParserUtils.toMap(atts), locator));
				} else {
					configuration.getSocketBindings().add(ParserUtils.ensureStack(stack, SocketBinding.class, localName, locator, true));
				}
				break;
			case "user-principal-class-name": {
				if (start) {
					text = new StringWriter();
				} else {
					SecurityRealm realm = ParserUtils.ensureStack(stack, SecurityRealm.class, localName, locator);
					realm.setUserPrincipalClassName(text.toString());
				}
				break;
			}
			case "role-principal-class-name": {
				if (start) {
					text = new StringWriter();
				} else {
					SecurityRealm realm = ParserUtils.ensureStack(stack, SecurityRealm.class, localName, locator);
					realm.getRolePrincipalClassNames().add(text.toString());
				}
				break;
			}
			default:
				break;
		}
	}

	private void handleWildflyElement(String localName, Attributes atts, boolean start) throws SAXParseException {
		switch (localName) {
			case "inet-address":
				if (start) {
					stack.push(Interface.InetAddress.create(ParserUtils.toMap(atts), locator));
				} else {
					Interface.InetAddress address = ParserUtils.ensureStack(stack, Interface.InetAddress.class, localName, locator, true);
					Interface iface = ParserUtils.ensureStack(stack, Interface.class, localName, locator);
					iface.getAddresses().add(address);
				}
				break;
			case "server-identities":
				if (start) {
					stack.push(new SecurityRealm.ServerIdentities());
				} else {
					SecurityRealm.ServerIdentities identities = ParserUtils.ensureStack(stack, SecurityRealm.ServerIdentities.class, localName, locator, true);
					SecurityRealm realm = ParserUtils.ensureStack(stack, SecurityRealm.class, localName, locator);
					realm.setIdentities(identities);
				}
				break;
			case "ssl":
				if (start) {
					stack.push(new SecurityRealm.SSLConfig());
				} else {
					SecurityRealm.SSLConfig sslConfig = ParserUtils.ensureStack(stack, SecurityRealm.SSLConfig.class, localName, locator, true);
					SecurityRealm.ServerIdentities identities = ParserUtils.ensureStack(stack, SecurityRealm.ServerIdentities.class, localName, locator);
					identities.setSsl(sslConfig);
				}
				break;
			case "engine":
				if (start) {
					stack.push(SecurityRealm.Engine.create(ParserUtils.toMap(atts), locator));
				} else {
					SecurityRealm.Engine engine = ParserUtils.ensureStack(stack, SecurityRealm.Engine.class, localName, locator, true);
					SecurityRealm.SSLConfig sslConfig = ParserUtils.ensureStack(stack, SecurityRealm.SSLConfig.class, localName, locator);
					sslConfig.setEngine(engine);
				}
				break;
			case "keystore":
				if (start) {
					stack.push(SecurityRealm.Keystore.create(ParserUtils.toMap(atts), locator));
				} else {
					SecurityRealm.Keystore keystore = ParserUtils.ensureStack(stack, SecurityRealm.Keystore.class, localName, locator, true);
					SecurityRealm.SSLConfig sslConfig = ParserUtils.ensureStack(stack, SecurityRealm.SSLConfig.class, localName, locator);
					sslConfig.setKeystore(keystore);
				}
				break;
			case "authentication":
				if (start) {
					stack.push(new SecurityRealm.Authentication());
				} else {
					SecurityRealm.Authentication auth = ParserUtils.ensureStack(stack, SecurityRealm.Authentication.class, localName, locator, true);
					SecurityRealm realm = ParserUtils.ensureStack(stack, SecurityRealm.class, localName, locator);
					realm.setAuthentication(auth);
				}
				break;
			case "truststore":
				if (start) {
					stack.push(SecurityRealm.Truststore.create(ParserUtils.toMap(atts), locator));
				} else {
					SecurityRealm.Truststore truststore = ParserUtils.ensureStack(stack, SecurityRealm.Truststore.class, localName, locator, true);
					SecurityRealm.Authentication auth = ParserUtils.ensureStack(stack, SecurityRealm.Authentication.class, localName, locator);
					auth.setTruststore(truststore);
				}
				break;
			case "jaas":
				if (start) {
					stack.push(SecurityRealm.JaasAuth.create(ParserUtils.toMap(atts), locator));
				} else {
					SecurityRealm.JaasAuth jaas = ParserUtils.ensureStack(stack, SecurityRealm.JaasAuth.class, localName, locator, true);
					SecurityRealm.Authentication auth = ParserUtils.ensureStack(stack, SecurityRealm.Authentication.class, localName, locator);
					auth.setJaas(jaas);
				}
				break;
			case "properties":
				if (start) {
					stack.push(SecurityRealm.PropertiesAuth.create(ParserUtils.toMap(atts), locator));
				} else {
					SecurityRealm.PropertiesAuth propertiesAuth = ParserUtils.ensureStack(stack, SecurityRealm.PropertiesAuth.class, localName, locator, true);
					SecurityRealm.Authentication auth = ParserUtils.ensureStack(stack, SecurityRealm.Authentication.class, localName, locator);
					auth.setProperties(propertiesAuth);
				}
				break;
			case "users":
				if (start) {
					stack.push(new SecurityRealm.UsersAuth());
				} else {
					SecurityRealm.UsersAuth usersAuth = ParserUtils.ensureStack(stack, SecurityRealm.UsersAuth.class, localName, locator, true);
					SecurityRealm.Authentication auth = ParserUtils.ensureStack(stack, SecurityRealm.Authentication.class, localName, locator);
					auth.setUsers(usersAuth);
				}
				break;
			case "user":
				if (start) {
					stack.push(SecurityRealm.User.create(ParserUtils.toMap(atts), locator));
				} else {
					SecurityRealm.User user = ParserUtils.ensureStack(stack, SecurityRealm.User.class, localName, locator, true);
					SecurityRealm.UsersAuth auth = ParserUtils.ensureStack(stack, SecurityRealm.UsersAuth.class, localName, locator);
					auth.getUsers().add(user);
				}
				break;
			case "password":
				if (start) {
					text = new StringWriter();
				} else {
					SecurityRealm.User user = ParserUtils.ensureStack(stack, SecurityRealm.User.class, localName, locator);
					user.setPassword(text.toString());
				}
				break;
			default:
				break;
		}
	}

	private void handleUndertowElement(String localName, Attributes atts, boolean start) throws SAXParseException {
		switch (localName) {
			case "subsystem":
				if (start) {
					stack.push(new UndertowSubsystem());
				} else {
					configuration.setSubsystem(ParserUtils.ensureStack(stack, UndertowSubsystem.class, localName, locator, true));
				}
				break;
			case "buffer-cache":
				if (start) {
					stack.push(UndertowSubsystem.BufferCache.create(ParserUtils.toMap(atts), locator));
				} else {
					UndertowSubsystem.BufferCache cache = ParserUtils.ensureStack(stack, UndertowSubsystem.BufferCache.class, localName, locator, true);
					UndertowSubsystem subsystem = ParserUtils.ensureStack(stack, UndertowSubsystem.class, localName, locator);
					subsystem.setBufferCache(cache);
				}
				break;
			case "handlers":
				if (start) {
					stack.push(new LinkedList<UndertowSubsystem.FileHandler>());
				} else {
					@SuppressWarnings("unchecked")
					LinkedList<UndertowSubsystem.FileHandler> handlers = ParserUtils.ensureStack(stack, LinkedList.class, localName, locator, true);
					UndertowSubsystem subsystem = ParserUtils.ensureStack(stack, UndertowSubsystem.class, localName, locator);
					subsystem.getFileHandlers().addAll(handlers);
				}
				break;
			case "file":
				if (start) {
					stack.push(UndertowSubsystem.FileHandler.create(ParserUtils.toMap(atts), locator));
				} else {
					UndertowSubsystem.FileHandler fileHandler = ParserUtils.ensureStack(stack, UndertowSubsystem.FileHandler.class, localName, locator, true);
					@SuppressWarnings("unchecked")
					LinkedList<UndertowSubsystem.FileHandler> handlers = ParserUtils.ensureStack(stack, LinkedList.class, localName, locator);
					handlers.add(fileHandler);
				}
				break;
			case "filters":
				if (start) {
					stack.push(new UndertowSubsystem.Filters());
				} else {
					UndertowSubsystem.Filters filters = ParserUtils.ensureStack(stack, UndertowSubsystem.Filters.class, localName, locator, true);
					UndertowSubsystem subsystem = ParserUtils.ensureStack(stack, UndertowSubsystem.class, localName, locator);
					subsystem.setFilters(filters);
				}
				break;
			case "response-header":
				if (start) {
					stack.push(UndertowSubsystem.ResponseHeaderFilter.create(ParserUtils.toMap(atts), locator));
				} else {
					UndertowSubsystem.ResponseHeaderFilter filter = ParserUtils.ensureStack(stack, UndertowSubsystem.ResponseHeaderFilter.class, localName, locator, true);
					UndertowSubsystem.Filters filters = ParserUtils.ensureStack(stack, UndertowSubsystem.Filters.class, localName, locator);
					filters.getResponseHeaders().add(filter);
				}
				break;
			case "error-page":
				if (start) {
					stack.push(UndertowSubsystem.ErrorPageFilter.create(ParserUtils.toMap(atts), locator));
				} else {
					UndertowSubsystem.ErrorPageFilter filter = ParserUtils.ensureStack(stack, UndertowSubsystem.ErrorPageFilter.class, localName, locator, true);
					UndertowSubsystem.Filters filters = ParserUtils.ensureStack(stack, UndertowSubsystem.Filters.class, localName, locator);
					filters.getErrorPages().add(filter);
				}
				break;
			case "filter":
				if (start) {
					stack.push(UndertowSubsystem.CustomFilter.create(ParserUtils.toMap(atts), locator));
				} else {
					UndertowSubsystem.CustomFilter filter = ParserUtils.ensureStack(stack, UndertowSubsystem.CustomFilter.class, localName, locator, true);
					UndertowSubsystem.Filters filters = ParserUtils.ensureStack(stack, UndertowSubsystem.Filters.class, localName, locator);
					filters.getCustomFilters().add(filter);
				}
				break;
			case "expression-filter":
				if (start) {
					stack.push(UndertowSubsystem.ExpressionFilter.create(ParserUtils.toMap(atts), locator));
				} else {
					UndertowSubsystem.ExpressionFilter filter = ParserUtils.ensureStack(stack, UndertowSubsystem.ExpressionFilter.class, localName, locator, true);
					UndertowSubsystem.Filters filters = ParserUtils.ensureStack(stack, UndertowSubsystem.Filters.class, localName, locator);
					filters.getExpressionFilters().add(filter);
				}
				break;
			case "gzip":
				if (start) {
					stack.push(UndertowSubsystem.GzipFilter.create(ParserUtils.toMap(atts), locator));
				} else {
					UndertowSubsystem.GzipFilter filter = ParserUtils.ensureStack(stack, UndertowSubsystem.GzipFilter.class, localName, locator, true);
					UndertowSubsystem.Filters filters = ParserUtils.ensureStack(stack, UndertowSubsystem.Filters.class, localName, locator);
					filters.getGzipFilters().add(filter);
				}
				break;
			case "request-limit":
				if (start) {
					stack.push(UndertowSubsystem.RequestLimitFilter.create(ParserUtils.toMap(atts), locator));
				} else {
					UndertowSubsystem.RequestLimitFilter filter = ParserUtils.ensureStack(stack, UndertowSubsystem.RequestLimitFilter.class, localName, locator, true);
					UndertowSubsystem.Filters filters = ParserUtils.ensureStack(stack, UndertowSubsystem.Filters.class, localName, locator);
					filters.getRequestLimitFilters().add(filter);
				}
				break;
			case "rewrite":
				if (start) {
					stack.push(UndertowSubsystem.RewriteFilter.create(ParserUtils.toMap(atts), locator));
				} else {
					UndertowSubsystem.RewriteFilter filter = ParserUtils.ensureStack(stack, UndertowSubsystem.RewriteFilter.class, localName, locator, true);
					UndertowSubsystem.Filters filters = ParserUtils.ensureStack(stack, UndertowSubsystem.Filters.class, localName, locator);
					filters.getRewriteFilters().add(filter);
				}
				break;
			case "servlet-container":
				if (start) {
					stack.push(ServletContainer.create(ParserUtils.toMap(atts), locator));
				} else {
					ServletContainer container = ParserUtils.ensureStack(stack, ServletContainer.class, localName, locator, true);
					UndertowSubsystem subsystem = ParserUtils.ensureStack(stack, UndertowSubsystem.class, localName, locator);
					subsystem.setServletContainer(container);
				}
				break;
			case "jsp-config":
				if (start) {
					stack.push(new ServletContainer.JspConfig());
				} else {
					ServletContainer.JspConfig config = ParserUtils.ensureStack(stack, ServletContainer.JspConfig.class, localName, locator, true);
					ServletContainer container = ParserUtils.ensureStack(stack, ServletContainer.class, localName, locator);
					container.setJspConfig(config);
				}
				break;
			case "persistent-sessions":
				if (start) {
					stack.push(ServletContainer.PersistentSessionsConfig.create(ParserUtils.toMap(atts), locator));
				} else {
					ServletContainer.PersistentSessionsConfig config = ParserUtils.ensureStack(stack, ServletContainer.PersistentSessionsConfig.class, localName, locator, true);
					ServletContainer container = ParserUtils.ensureStack(stack, ServletContainer.class, localName, locator);
					container.setPersistentSessions(config);
				}
				break;
			case "websockets":
				if (start) {
					stack.push(ServletContainer.Websockets.create(ParserUtils.toMap(atts), locator));
				} else {
					ServletContainer.Websockets websockets = ParserUtils.ensureStack(stack, ServletContainer.Websockets.class, localName, locator, true);
					ServletContainer container = ParserUtils.ensureStack(stack, ServletContainer.class, localName, locator);
					container.setWebsockets(websockets);
				}
				break;
			case "welcome-files":
				if (start) {
					stack.push(new LinkedList<ServletContainer.WelcomeFile>());
				} else {
					@SuppressWarnings("unchecked")
					LinkedList<ServletContainer.WelcomeFile> files = ParserUtils.ensureStack(stack, LinkedList.class, localName, locator, true);
					ServletContainer container = ParserUtils.ensureStack(stack, ServletContainer.class, localName, locator);
					container.getWelcomeFiles().addAll(files);
				}
				break;
			case "welcome-file":
				if (start) {
					stack.push(ServletContainer.WelcomeFile.create(ParserUtils.toMap(atts), locator));
				} else {
					ServletContainer.WelcomeFile wf = ParserUtils.ensureStack(stack, ServletContainer.WelcomeFile.class, localName, locator, true);
					@SuppressWarnings("unchecked")
					LinkedList<ServletContainer.WelcomeFile> files = ParserUtils.ensureStack(stack, LinkedList.class, localName, locator);
					files.add(wf);
				}
				break;
			case "session-cookie":
				if (start) {
					stack.push(ServletContainer.SessionCookie.create(ParserUtils.toMap(atts), locator));
				} else {
					ServletContainer.SessionCookie cookie = ParserUtils.ensureStack(stack, ServletContainer.SessionCookie.class, localName, locator, true);
					ServletContainer container = ParserUtils.ensureStack(stack, ServletContainer.class, localName, locator);
					container.setSessionCookie(cookie);
				}
				break;
			case "server":
				if (start) {
					stack.push(Server.create(ParserUtils.toMap(atts), locator));
				} else {
					Server server = ParserUtils.ensureStack(stack, Server.class, localName, locator, true);
					UndertowSubsystem subsystem = ParserUtils.ensureStack(stack, UndertowSubsystem.class, localName, locator);
					subsystem.setServer(server);
				}
				break;
			case "http-listener":
				if (start) {
					stack.push(Server.HttpListener.create(ParserUtils.toMap(atts), locator));
				} else {
					Server.HttpListener listener = ParserUtils.ensureStack(stack, Server.HttpListener.class, localName, locator, true);
					Server server = ParserUtils.ensureStack(stack, Server.class, localName, locator);
					server.getHttpListeners().add(listener);
				}
				break;
			case "https-listener":
				if (start) {
					stack.push(Server.HttpsListener.create(ParserUtils.toMap(atts), locator));
				} else {
					Server.HttpsListener listener = ParserUtils.ensureStack(stack, Server.HttpsListener.class, localName, locator, true);
					Server server = ParserUtils.ensureStack(stack, Server.class, localName, locator);
					server.getHttpsListeners().add(listener);
				}
				break;
			case "host":
				if (start) {
					stack.push(Server.Host.create(ParserUtils.toMap(atts), locator));
				} else {
					Server.Host host = ParserUtils.ensureStack(stack, Server.Host.class, localName, locator, true);
					Server server = ParserUtils.ensureStack(stack, Server.class, localName, locator);
					server.setHost(host);
				}
				break;
			case "location":
				if (start) {
					stack.push(Server.Host.Location.create(ParserUtils.toMap(atts), locator));
				} else {
					Server.Host.Location location = ParserUtils.ensureStack(stack, Server.Host.Location.class, localName, locator, true);
					Server.Host host = ParserUtils.ensureStack(stack, Server.Host.class, localName, locator);
					host.getLocations().add(location);
				}
				break;
			case "access-log":
				if (start) {
					stack.push(Server.Host.AccessLog.create(ParserUtils.toMap(atts), locator));
				} else {
					Server.Host.AccessLog accessLog = ParserUtils.ensureStack(stack, Server.Host.AccessLog.class, localName, locator, true);
					Server.Host host = ParserUtils.ensureStack(stack, Server.Host.class, localName, locator);
					host.setAccessLog(accessLog);
				}
				break;
			case "filter-ref":
				if (start) {
					stack.push(Server.Host.FilterRef.create(ParserUtils.toMap(atts), locator));
				} else {
					Server.Host.FilterRef filter = ParserUtils.ensureStack(stack, Server.Host.FilterRef.class, localName, locator, true);
					if (stack.peek() instanceof Server.Host) {
						Server.Host host = ParserUtils.ensureStack(stack, Server.Host.class, localName, locator);
						host.getFilterRefs().add(filter);
					} else if (stack.peek() instanceof Server.Host.Location) {
						Server.Host.Location location = ParserUtils.ensureStack(stack, Server.Host.Location.class, localName, locator);
						location.getFilterRefs().add(filter);
					}
				}
				break;
			default:
				break;
		}
	}

	private void handleIOElement(String localName, Attributes atts, boolean start) throws SAXParseException {
		switch (localName) {
			case "subsystem":
				if (start) {
					stack.push(new IoSubsystem());
				} else {
					configuration.setIoSubsystem(ParserUtils.ensureStack(stack, IoSubsystem.class, localName, locator, true));
				}
				break;
			case "worker": {
				if (start) {
					stack.push(IoSubsystem.Worker.create(ParserUtils.toMap(atts), locator));
				} else {
					IoSubsystem.Worker worker = ParserUtils.ensureStack(stack, IoSubsystem.Worker.class, localName, locator, true);
					IoSubsystem ioSubsystem = ParserUtils.ensureStack(stack, IoSubsystem.class, localName, locator);
					ioSubsystem.getWorkers().add(worker);
				}
				break;
			}
			case "buffer-pool": {
				if (start) {
					stack.push(IoSubsystem.BufferPool.create(ParserUtils.toMap(atts), locator));
				} else {
					IoSubsystem.BufferPool pool = ParserUtils.ensureStack(stack, IoSubsystem.BufferPool.class, localName, locator, true);
					IoSubsystem ioSubsystem = ParserUtils.ensureStack(stack, IoSubsystem.class, localName, locator);
					ioSubsystem.getBufferPools().add(pool);
				}
				break;
			}
			default:
				break;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (text != null) {
			text.write(ch, start, length);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) {
	}

	@Override
	public void processingInstruction(String target, String data) {
	}

	@Override
	public void skippedEntity(String name) {
	}

}
