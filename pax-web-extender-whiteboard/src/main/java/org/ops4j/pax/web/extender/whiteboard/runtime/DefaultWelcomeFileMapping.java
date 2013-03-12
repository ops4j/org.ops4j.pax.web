package org.ops4j.pax.web.extender.whiteboard.runtime;

import java.util.Arrays;

import org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping;

/**
 * Default implementation of
 * {@link org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping}.
 * 
 * @author dsklyut
 * @since 0.7.0
 */
public class DefaultWelcomeFileMapping implements WelcomeFileMapping {

	/**
	 * Http Context id.
	 */
	private String httpContextId;

	/**
	 * welcome files
	 */
	private String[] welcomeFiles;

	/**
	 * redirect flag true - send redirect false - use forward
	 */
	private boolean redirect;

	/**
	 * @see org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping#getHttpContextId()
	 */
	public String getHttpContextId() {
		return httpContextId;
	}

	/**
	 * @see org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping#isRedirect()
	 */
	public boolean isRedirect() {
		return redirect;
	}

	/**
	 * @see org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping#getWelcomeFiles()
	 */
	public String[] getWelcomeFiles() {
		return welcomeFiles;
	}

	/**
	 * Setter.
	 * 
	 * @param httpContextId
	 *            id of the http context these welcome pages belongs to
	 */
	public void setHttpContextId(String httpContextId) {
		this.httpContextId = httpContextId;
	}

	/**
	 * Setter
	 * 
	 * @param welcomeFiles
	 *            welcome files
	 */
	public void setWelcomeFiles(String[] welcomeFiles) {
		this.welcomeFiles = welcomeFiles;
	}

	/**
	 * Setter
	 * 
	 * @param redirect
	 *            weather to redirect or forward.
	 */
	public void setRedirect(boolean redirect) {
		this.redirect = redirect;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("httpContextId=").append(httpContextId)
				.append(",welcomeFiles=")
				.append(Arrays.deepToString(welcomeFiles)).append(",redirect=")
				.append(redirect).append("}").toString();
	}
}
