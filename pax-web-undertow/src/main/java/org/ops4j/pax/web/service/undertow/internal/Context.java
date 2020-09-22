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
package org.ops4j.pax.web.service.undertow.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.SessionPersistenceManager;
import io.undertow.servlet.api.TransportGuaranteeType;
import io.undertow.servlet.api.WebResourceCollection;
import org.ops4j.pax.web.service.AuthenticatorService;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioWorker;

/**
 * @author Guillaume Nodet
 */
public class Context /*implements org.ops4j.pax.web.service.spi.LifeCycle, HttpHandler, ResourceManager */ {

	private static final Logger LOG = LoggerFactory.getLogger(Context.class);

	private IdentityManager identityManager;
	private ContextAwarePathHandler path;
	private OsgiContextModel contextModel;
	private final Set<SecurityConstraintMappingModel> securityConstraintMappings = new LinkedHashSet<>();
	private final List<ServiceRegistration<ServletContext>> registeredServletContexts = new ArrayList<>();
	private /*final */ClassLoader classLoader;
	private volatile HttpHandler handler;

	private DeploymentManager manager;

	private Configuration configuration;
	private XnioWorker wsXnioWorker;

	private SessionPersistenceManager sessionPersistenceManager;

	private void doCreateHandler(Consumer<ServletContext> consumer) throws ServletException {
		// org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService#createServletConfig
		DeploymentInfo deployment = new DeploymentInfo();
//		if (contextModel.getRealmName() != null && contextModel.getAuthMethod() != null) {
//			ServletExtension authenticator = getAuthenticator(contextModel.getAuthMethod());
//			if (authenticator != null) {
//				deployment.getServletExtensions().add(authenticator);
//			}
//			LoginConfig cfg = new LoginConfig(
//					contextModel.getAuthMethod(),
//					contextModel.getRealmName(),
//					contextModel.getFormLoginPage(),
//					contextModel.getFormErrorPage());
//			deployment.setLoginConfig(cfg);
//		}
		boolean defaultServletAdded = false;
		ServletModel fallbackDefaultServlet = null;

//		for (Entry<ServletContainerInitializer, Set<Class<?>>> entry : contextModel.getContainerInitializers().entrySet()) {
//			deployment.addServletContainerInitalizer(new ServletContainerInitializerInfo(
//					clazz(null, entry.getKey()),
//					factory(null, entry.getKey()),
//					entry.getValue()
//			));
//		}

		for (SecurityConstraintMappingModel securityConstraintMapping : securityConstraintMappings) {
			SecurityConstraint info = new SecurityConstraint();
//            if (securityConstraintMapping.isAuthentication()) {
//                info.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.AUTHENTICATE);
//            }
			info.addRolesAllowed(securityConstraintMapping.getRoles());
			String dataConstraint = securityConstraintMapping.getDataConstraint();
			if (dataConstraint == null || "NONE".equals(dataConstraint)) {
				info.setTransportGuaranteeType(TransportGuaranteeType.NONE);
			} else if ("INTEGRAL".equals(dataConstraint)) {
				info.setTransportGuaranteeType(TransportGuaranteeType.INTEGRAL);
			} else {
				info.setTransportGuaranteeType(TransportGuaranteeType.CONFIDENTIAL);
			}
			WebResourceCollection wr = new WebResourceCollection();
			if (securityConstraintMapping.getMapping() != null) {
				wr.addHttpMethod(securityConstraintMapping.getMapping());
			}
			if (securityConstraintMapping.getUrl() != null) {
				wr.addUrlPattern(securityConstraintMapping.getUrl());
			}
			info.addWebResourceCollection(wr);
			deployment.addSecurityConstraint(info);
		}

//		if (isJspAvailable()) { // use JasperClassloader
//			try {
//				@SuppressWarnings("unchecked")
//				Class<ServletContainerInitializer> clazz = (Class<ServletContainerInitializer>)
//						classLoader.loadClass("org.ops4j.pax.web.jsp.JasperInitializer");
//				deployment.addServletContainerInitializer(new ServletContainerInitializerInfo(
//						clazz, factory(clazz, null), null));
//			} catch (ClassNotFoundException e) {
////                LOG.error("Unable to load JasperInitializer", e);
//				e.printStackTrace();
//			}
//		}

//		if (isWebSocketAvailable()) {
//			wsXnioWorker = UndertowUtil.createWorker(contextModel.getClassLoader());
//			if (wsXnioWorker != null) {
//				deployment.addServletContextAttribute(
//						io.undertow.websockets.jsr.WebSocketDeploymentInfo.ATTRIBUTE_NAME,
//						new io.undertow.websockets.jsr.WebSocketDeploymentInfo()
//								.setWorker(wsXnioWorker)
//								.setBuffers(new DefaultByteBufferPool(true, 100))
//				);
//			}
//		}

		deployment.setSessionPersistenceManager(sessionPersistenceManager);
	}

	private ServletExtension getAuthenticator(String method) {
		ServiceLoader<AuthenticatorService> sl = ServiceLoader.load(AuthenticatorService.class, getClass().getClassLoader());
		for (AuthenticatorService svc : sl) {
			try {
				ServletExtension auth = svc.getAuthenticatorService(method, ServletExtension.class);
				if (auth != null) {
					return auth;
				}
			} catch (Throwable t) {
				LOG.debug("Unable to load AuthenticatorService for: " + method, t);
			}
		}
		return null;
	}

//	public void addSecurityConstraintMapping(SecurityConstraintMappingModel model) throws ServletException {
//		if (securityConstraintMappings.add(model)) {
//			if (started.get()) {
//				destroyHandler();
//			}
//		}
//	}
//
//	public void removeSecurityConstraintMapping(SecurityConstraintMappingModel model) throws ServletException {
//		if (securityConstraintMappings.remove(model)) {
//			if (started.get()) {
//				destroyHandler();
//			}
//		}
//	}

}
