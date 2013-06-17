/*
 * Copyright 2013 Achim Nierbeck
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Globals;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.Base64;
import org.apache.catalina.util.DateTool;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anierbeck
 *
 */
public class OSGiAuthenticatorValve extends AuthenticatorBase {
	
	private static final Logger LOG = LoggerFactory.getLogger(OSGiAuthenticatorValve.class);

	private String authenticationType;

	private final HttpContext httpContext;
	
	public OSGiAuthenticatorValve(HttpContext httpContext) {
		this.httpContext = httpContext;
	}
	
	@Override
	public void invoke(Request request, Response response) throws IOException,
			ServletException {
		authenticationType = (String) request.getAttribute(HttpContext.AUTHENTICATION_TYPE);
		String remoteUser = (String) request.getAttribute(HttpContext.REMOTE_USER);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Security checking request " +
                request.getMethod() + " " + request.getRequestURI());
		}
        LoginConfig config = this.context.getLoginConfig();

        // Have we got a cached authenticated Principal to record?
        if (cache) {
            Principal principal = request.getUserPrincipal();
            if (principal == null) {
                Session session = request.getSessionInternal(false);
                if (session != null) {
                    principal = session.getPrincipal();
                    if (principal != null) {
                        if (LOG.isDebugEnabled()) { //CHECKSTYLE:SKIP
							LOG.debug("We have cached auth type " +
                                session.getAuthType() +
                                " for principal " +
                                session.getPrincipal());
						}
                        request.setAuthType(session.getAuthType());
                        request.setUserPrincipal(principal);
                    }
                }
            }
        }

        // Special handling for form-based logins to deal with the case
        // where the login form (and therefore the "j_security_check" URI
        // to which it submits) might be outside the secured area
        String contextPath = this.context.getPath();
        String requestURI = request.getDecodedRequestURI();
        if (requestURI.startsWith(contextPath) &&
            requestURI.endsWith(Constants.FORM_ACTION)) {
            if (!authenticate(request, response, config)) {
                if (LOG.isDebugEnabled()) {
					LOG.debug(" Failed authenticate() test ??" + requestURI );
				}
                return;
            }
        }

        // The Servlet may specify security constraints through annotations.
        // Ensure that they have been processed before constraints are checked
        Wrapper wrapper = (Wrapper) request.getMappingData().wrapper;
        if (wrapper != null) {
            wrapper.servletSecurityAnnotationScan();
        }

        Realm realm = this.context.getRealm();
        // Is this request URI subject to a security constraint?
        SecurityConstraint [] constraints
            = realm.findSecurityConstraints(request, this.context);
       
        if (constraints == null && !context.getPreemptiveAuthentication()) {
            if (LOG.isDebugEnabled()) {
				LOG.debug(" Not subject to any constraint");
			}
            getNext().invoke(request, response);
            return;
        }

        // Make sure that constrained resources are not cached by web proxies
        // or browsers as caching can provide a security hole
        if (constraints != null && disableProxyCaching && 
            !"POST".equalsIgnoreCase(request.getMethod())) {
            if (securePagesWithPragma) {
                // Note: These can cause problems with downloading files with IE
                response.setHeader("Pragma", "No-cache");
                response.setHeader("Cache-Control", "no-cache");
            } else {
                response.setHeader("Cache-Control", "private");
            }
            response.setHeader("Expires", (new SimpleDateFormat(DateTool.HTTP_RESPONSE_DATE_HEADER,
                    Locale.US)).format(new Date(1)));
        }

        int i;
        if (constraints != null) {
            // Enforce any user data constraint for this security constraint
            if (LOG.isDebugEnabled()) {
                LOG.debug(" Calling hasUserDataPermission()");
            }
            if (!realm.hasUserDataPermission(request, response,
                                             constraints)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" Failed hasUserDataPermission() test");
                }
                /*
                 * ASSERT: Authenticator already set the appropriate
                 * HTTP status code, so we do not have to do anything special
                 */
                return;
            }
        }

        // Since authenticate modifies the response on failure,
        // we have to check for allow-from-all first.
        boolean authRequired;
        if (constraints == null) {
            authRequired = false;
        } else {
            authRequired = true;
            for (i = 0; i < constraints.length && authRequired; i++) {
                if (!constraints[i].getAuthConstraint()) {
                    authRequired = false;
                } else if (!constraints[i].getAllRoles()) {
                    String [] roles = constraints[i].findAuthRoles();
                    if (roles == null || roles.length == 0) {
                        authRequired = false;
                    }
                }
            }
        }

        if (!authRequired && context.getPreemptiveAuthentication()) {
            authRequired =
                request.getCoyoteRequest().getMimeHeaders().getValue(
                        "authorization") != null;
        }

        if (!authRequired && context.getPreemptiveAuthentication()) {
            X509Certificate[] certs = (X509Certificate[]) request.getAttribute(
                    Globals.CERTIFICATES_ATTR);
            authRequired = certs != null && certs.length > 0;
        }

        if (authRequired) {  
            if (LOG.isDebugEnabled()) {
                LOG.debug(" Calling authenticate()");
            }
            if (!authenticate(request, response, config)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" Failed authenticate() test");
                }
                /*
                 * ASSERT: Authenticator already set the appropriate
                 * HTTP status code, so we do not have to do anything
                 * special
                 */
                return;
            } 
            
        }
    
        if (constraints != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(" Calling accessControl()");
            }
            if (!realm.hasResourcePermission(request, response,
                                             constraints,
                                             this.context)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" Failed accessControl() test");
                }
                /*
                 * ASSERT: AccessControl method has already set the
                 * appropriate HTTP status code, so we do not have to do
                 * anything special
                 */
                return;
            }
        }
    
        // Any and all specified constraints have been satisfied
        if (LOG.isDebugEnabled()) {
            LOG.debug(" Successfully passed all security constraints");
        }
        getNext().invoke(request, response);

	}
	
	@Override
	public boolean authenticate(Request request, HttpServletResponse response,
			LoginConfig config) throws IOException {
		
        // Have we already authenticated someone?
        Principal principal = request.getUserPrincipal();
        String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
        if (principal != null) {
            if (LOG.isDebugEnabled()) {
				LOG.debug("Already authenticated '" + principal.getName() + "'");
			}
            // Associate the session with any existing SSO session
            if (ssoId != null) {
				associate(ssoId, request.getSessionInternal(true));
			}
            return (true);
        }

        // Is there an SSO session against which we can try to reauthenticate?
        if (ssoId != null) {
            if (LOG.isDebugEnabled()) {
				LOG.debug("SSO Id " + ssoId + " set; attempting " +
                          "reauthentication");
			}
            /* Try to reauthenticate using data cached by SSO.  If this fails,
               either the original SSO logon was of DIGEST or SSL (which
               we can't reauthenticate ourselves because there is no
               cached username and password), or the realm denied
               the user's reauthentication for some reason.
               In either case we have to prompt the user for a logon */
            if (reauthenticateFromSSO(ssoId, request)) {
				return true;
			}
        }

        // Validate any credentials already included with this request
        String username = null;
        String password = null;

        MessageBytes authorization = 
            request.getCoyoteRequest().getMimeHeaders()
            .getValue("authorization");
        
        if (authorization != null) {
            authorization.toBytes();
            ByteChunk authorizationBC = authorization.getByteChunk();
            if (authorizationBC.startsWithIgnoreCase("basic ", 0)) {
                authorizationBC.setOffset(authorizationBC.getOffset() + 6);
                // FIXME: Add trimming
                // authorizationBC.trim();
                
                CharChunk authorizationCC = authorization.getCharChunk();
                Base64.decode(authorizationBC, authorizationCC);
                
                // Get username and password
                int colon = authorizationCC.indexOf(':');
                if (colon < 0) {
                    username = authorizationCC.toString();
                } else {
                    char[] buf = authorizationCC.getBuffer();
                    username = new String(buf, 0, colon);
                    password = new String(buf, colon + 1, 
                            authorizationCC.getEnd() - colon - 1);
                }
                
                authorizationBC.setOffset(authorizationBC.getOffset() - 6);
            }

            principal = context.getRealm().authenticate(username, password);
            if (principal != null) {
                register(request, response, principal,
                        HttpServletRequest.BASIC_AUTH, username, password);
                return (true);
            }
        }
        
        StringBuilder value = new StringBuilder(16);
        value.append("Basic realm=\"");
        if (config.getRealmName() == null) {
            value.append(REALM_NAME);
        } else {
            value.append(config.getRealmName());
        }
        value.append('\"');        
        response.setHeader(AUTH_HEADER_NAME, value.toString());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return (false);
		
	}

	@Override
	protected String getAuthMethod() {
		return authenticationType;
	}

}
