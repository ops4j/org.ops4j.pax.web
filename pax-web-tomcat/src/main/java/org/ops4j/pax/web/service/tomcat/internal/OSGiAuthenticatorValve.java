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
import java.nio.charset.StandardCharsets;
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
import org.apache.catalina.authenticator.SavedRequest;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anierbeck
 */
public class OSGiAuthenticatorValve extends AuthenticatorBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(OSGiAuthenticatorValve.class);

	/**
	 * "Expires" header always set to Date(1), so generate once only
	 */
	private static final String DATE_ONE = (new SimpleDateFormat(
			FastHttpDateFormat.RFC1123_DATE, Locale.US)).format(new Date(1));

	private String authenticationType;

	private final HttpContext httpContext;

	public OSGiAuthenticatorValve(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

	@Override
	public void invoke(Request request, Response response) throws IOException,
			ServletException {
		authenticationType = (String) request
				.getAttribute(HttpContext.AUTHENTICATION_TYPE);
		String remoteUser = (String) request
				.getAttribute(HttpContext.REMOTE_USER);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Security checking request " + request.getMethod() + " "
					+ request.getRequestURI());
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
						//CHECKSTYLE:OFF
						if (LOG.isDebugEnabled()) {
							LOG.debug("We have cached auth type "
									+ session.getAuthType() + " for principal "
									+ session.getPrincipal());
						}
						//CHECKSTYLE:ON
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
		if (requestURI.startsWith(contextPath)
				&& requestURI.endsWith(Constants.FORM_ACTION)) {
			if (!authenticate(request, response)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(" Failed authenticate() test ??" + requestURI);
				}
				return;
			}
		}

		// Special handling for form-based logins to deal with the case where
		// a resource is protected for some HTTP methods but not protected for
		// GET which is used after authentication when redirecting to the
		// protected resource.
		// TODO: This is similar to the FormAuthenticator.matchRequest() logic
		// Is there a way to remove the duplication?
		Session session = request.getSessionInternal(false);
		if (session != null) {
			SavedRequest savedRequest = (SavedRequest) session
					.getNote(Constants.FORM_REQUEST_NOTE);
			if (savedRequest != null) {
				String decodedRequestURI = request.getDecodedRequestURI();
				if (decodedRequestURI != null
						&& decodedRequestURI.equals(savedRequest
						.getDecodedRequestURI())) {
					if (!authenticate(request, response)) {
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
		SecurityConstraint[] constraints = realm.findSecurityConstraints(
				request, this.context);

		if (constraints == null && !context.getPreemptiveAuthentication()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(" Not subject to any constraint");
			}
			getNext().invoke(request, response);
			return;
		}

		// Make sure that constrained resources are not cached by web proxies
		// or browsers as caching can provide a security hole
		if (constraints != null && disableProxyCaching
				&& !"POST".equalsIgnoreCase(request.getMethod())) {
			if (securePagesWithPragma) {
				// Note: These can cause problems with downloading files with IE
				response.setHeader("Pragma", "No-cache");
				response.setHeader("Cache-Control", "no-cache");
			} else {
				response.setHeader("Cache-Control", "private");
			}
			response.setHeader("Expires", DATE_ONE);
		}

		int i;
		if (constraints != null) {
			// Enforce any user data constraint for this security constraint
			if (LOG.isDebugEnabled()) {
				LOG.debug(" Calling hasUserDataPermission()");
			}
			if (!realm.hasUserDataPermission(request, response, constraints)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(" Failed hasUserDataPermission() test");
				}
				/*
				 * ASSERT: Authenticator already set the appropriate HTTP status
				 * code, so we do not have to do anything special
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
					String[] roles = constraints[i].findAuthRoles();
					if (roles == null || roles.length == 0) {
						authRequired = false;
					}
				}
			}
		}

		if (!authRequired && context.getPreemptiveAuthentication()) {
			authRequired = request.getCoyoteRequest().getMimeHeaders()
					.getValue("authorization") != null;
		}

		if (!authRequired && context.getPreemptiveAuthentication()) {
			X509Certificate[] certs = (X509Certificate[]) request
					.getAttribute(Globals.CERTIFICATES_ATTR);
			authRequired = certs != null && certs.length > 0;
		}

		if (authRequired) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(" Calling authenticate()");
			}
			if (!authenticate(request, response)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(" Failed authenticate() test");
				}
				/*
				 * ASSERT: Authenticator already set the appropriate HTTP status
				 * code, so we do not have to do anything special
				 */
				return;
			}

		}

		if (constraints != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(" Calling accessControl()");
			}
			if (!realm.hasResourcePermission(request, response, constraints,
					this.context)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(" Failed accessControl() test");
				}
				/*
				 * ASSERT: AccessControl method has already set the appropriate
				 * HTTP status code, so we do not have to do anything special
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
	public boolean authenticate(Request request, HttpServletResponse response)
			throws IOException {

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
				LOG.debug("SSO Id " + ssoId + " set; attempting "
						+ "reauthentication");
			}
			/*
			 * Try to reauthenticate using data cached by SSO. If this fails,
			 * either the original SSO logon was of DIGEST or SSL (which we
			 * can't reauthenticate ourselves because there is no cached
			 * username and password), or the realm denied the user's
			 * reauthentication for some reason. In either case we have to
			 * prompt the user for a logon
			 */
			if (reauthenticateFromSSO(ssoId, request)) {
				return true;
			}
		}

		// Validate any credentials already included with this request
		MessageBytes authorization = request.getCoyoteRequest()
				.getMimeHeaders().getValue("authorization");

		if (authorization != null) {
			authorization.toBytes();
			ByteChunk authorizationBC = authorization.getByteChunk();
			BasicCredentials credentials = null;
			try {
				credentials = new BasicCredentials(authorizationBC);
				String username = credentials.getUsername();
				String password = credentials.getPassword();

				principal = context.getRealm().authenticate(username, password);
				if (principal != null) {
					register(request, response, principal,
							HttpServletRequest.BASIC_AUTH, username, password);
					return (true);
				}
			} catch (IllegalArgumentException iae) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Invalid Authorization" + iae.getMessage());
				}
			}
		}

		// the request could not be authenticated, so reissue the challenge
		StringBuilder value = new StringBuilder(16);
		value.append("Basic realm=\"");
		value.append(getRealmName(context));
		value.append('\"');
		response.setHeader(AUTH_HEADER_NAME, value.toString());
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		return (false);

	}

	@Override
	protected String getAuthMethod() {
		return authenticationType;
	}

	/**
	 * Parser for an HTTP Authorization header for BASIC authentication as per
	 * RFC 2617 section 2, and the Base64 encoded credentials as per RFC 2045
	 * section 6.8.
	 */
	protected static class BasicCredentials {

		// the only authentication method supported by this parser
		// note: we include single white space as its delimiter
		private static final String METHOD = "basic ";

		private ByteChunk authorization;
		private int initialOffset;
		private int base64blobOffset;
		private int base64blobLength;

		private String username = null;
		private String password = null;

		/**
		 * Parse the HTTP Authorization header for BASIC authentication as per
		 * RFC 2617 section 2, and the Base64 encoded credentials as per RFC
		 * 2045 section 6.8.
		 *
		 * @param input The header value to parse in-place
		 * @throws IllegalArgumentException If the header does not conform to RFC 2617
		 */
		public BasicCredentials(ByteChunk input)
				throws IllegalArgumentException {
			authorization = input;
			initialOffset = input.getOffset();
			parseMethod();
			byte[] decoded = parseBase64();
			parseCredentials(decoded);
		}

		/**
		 * Trivial accessor.
		 *
		 * @return the decoded username token as a String, which is never be
		 * <code>null</code>, but can be empty.
		 */
		public String getUsername() {
			return username;
		}

		/**
		 * Trivial accessor.
		 *
		 * @return the decoded password token as a String, or <code>null</code>
		 * if no password was found in the credentials.
		 */
		public String getPassword() {
			return password;
		}

		/*
		 * The authorization method string is case-insensitive and must hae at
		 * least one space character as a delimiter.
		 */
		private void parseMethod() throws IllegalArgumentException {
			if (authorization.startsWithIgnoreCase(METHOD, 0)) {
				// step past the auth method name
				base64blobOffset = initialOffset + METHOD.length();
				base64blobLength = authorization.getLength() - METHOD.length();
			} else {
				// is this possible, or permitted?
				throw new IllegalArgumentException(
						"Authorization header method is not \"Basic\"");
			}
		}

		/*
		 * Decode the base64-user-pass token, which RFC 2617 states can be
		 * longer than the 76 characters per line limit defined in RFC 2045. The
		 * base64 decoder will ignore embedded line break characters as well as
		 * surplus surrounding white space.
		 */
		private byte[] parseBase64() throws IllegalArgumentException {
			byte[] decoded = Base64.decodeBase64(authorization.getBuffer(),
					base64blobOffset, base64blobLength);
			// restore original offset
			authorization.setOffset(initialOffset);
			if (decoded == null) {
				throw new IllegalArgumentException(
						"Basic Authorization credentials are not Base64");
			}
			return decoded;
		}

		/*
		 * Extract the mandatory username token and separate it from the
		 * optional password token. Tolerate surplus surrounding white space.
		 */
		private void parseCredentials(byte[] decoded)
				throws IllegalArgumentException {

			int colon = -1;
			for (int i = 0; i < decoded.length; i++) {
				if (decoded[i] == ':') {
					colon = i;
					break;
				}
			}

			if (colon < 0) {
				username = new String(decoded, StandardCharsets.ISO_8859_1);
				// password will remain null!
			} else {
				username = new String(decoded, 0, colon,
						StandardCharsets.ISO_8859_1);
				password = new String(decoded, colon + 1, decoded.length
						- colon - 1, StandardCharsets.ISO_8859_1);
				// tolerate surplus white space around credentials
				if (password.length() > 1) {
					password = password.trim();
				}
			}
			// tolerate surplus white space around credentials
			if (username.length() > 1) {
				username = username.trim();
			}
		}
	}
}
