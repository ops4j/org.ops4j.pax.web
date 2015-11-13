package org.ops4j.pax.web.jsf.resourcehandler.extender;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.context.FacesContext;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom resource-implementation because other implementations are tied to
 * either Mojarra or Myfaces.
 */
public class OsgiResource extends Resource {

	private transient Logger logger;
	private final URL bundleResourceUrl;
	private final LocalDateTime lastModified;

	public OsgiResource(URL bundleResourceUrl, String resourceName, String libraryName, LocalDateTime lastModified) {
		if (bundleResourceUrl == null) {
			throw new IllegalArgumentException("URL for resource must not be null");
		}
		logger = LoggerFactory.getLogger(getClass());
		setResourceName(resourceName);
		setLibraryName(libraryName);
		this.bundleResourceUrl = bundleResourceUrl;
		this.lastModified = lastModified;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		try{
			return bundleResourceUrl.openConnection().getInputStream();
		}catch (Exception e){
			logger.error("Cannot open InputStream. This can happen when the bundle that contains the resource was stopped after resource-creation");
			throw new IOException("Resource not available any more because bundle was uninstalled.");
		}
	}

	// TODO extract configured Mapping to FacesServlet
	@Override
	public String getRequestPath() {
		final FacesContext facesContext = FacesContext.getCurrentInstance();

		List<String> optionalParameters = new ArrayList<>(5);
		if (getLibraryName() != null) {
			optionalParameters.add("ln=" + getLibraryName());
		}
		if (!facesContext.isProjectStage(ProjectStage.Production))
        {
            // append stage for all ProjectStages except Production
			optionalParameters.add("stage=" + facesContext.getApplication().getProjectStage().toString());
        }
		
		StringBuilder sb = new StringBuilder(30)
				.append(ResourceHandler.RESOURCE_IDENTIFIER).append('/')
				.append(getResourceName())
				// the mapping has to be added, otherwise resources are not dispatched by the FacesServlet
				.append(".xhtml"); 
		
		String parameterString = optionalParameters.stream().collect(Collectors.joining("&"));
		if(StringUtils.isNotBlank(parameterString)){
			sb.append("?").append(parameterString);
		}
		
		return facesContext.getApplication().getViewHandler().getResourceURL(facesContext, sb.toString());

	}

	@Override
	public Map<String, String> getResponseHeaders() {
		return new HashMap<>(0);
	}

	@Override
	public URL getURL() {
		return bundleResourceUrl;
	}

	@Override
	public boolean userAgentNeedsUpdate(FacesContext fc) {
		// RFC2616 says related to If-Modified-Since header the following:
		//
		// "... The If-Modified-Since request-header field is used with a method
		// to make it conditional: if the requested variant has not been
		// modified since the time specified in this field, an entity will not
		// be returned from the server; instead, a 304 (not modified) response
		// will be returned without any message-body..."
		//
		// This method is called from ResourceHandlerImpl.handleResourceRequest
		// and if returns false send a 304 Not Modified response.
		if (!fc.isProjectStage(ProjectStage.Development)) {
			final Optional<String> ifModSinceHeader = Optional
					.ofNullable(fc.getExternalContext().getRequestHeaderMap().get("If-Modified-Since"));
			if (ifModSinceHeader.isPresent()) {
				LocalDateTime ifModifiedSince = convertIfModifiedSinceToDate(ifModSinceHeader.get());
				if (ifModifiedSince != null)
					return lastModified.isAfter(ifModifiedSince);
			}
		}
		// when in Development, or no if-modified-since header was found
		// (or couldn't be parsed),request a fresh resource
		return true;
	}

	/**
	 * <p>
	 * RFC 2616 allows three different date-formats:
	 * <ul>
	 * <li>RFC 1123</li>
	 * <li>RFC 1036</li>
	 * <li>ASCI-Time</li>
	 * </ul>
	 * </p>
	 * 
	 * @param headerValue
	 *            value transmitted from client
	 * @return the parsed DateTime
	 * @see <a href=
	 *      "http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3">RFC
	 *      2616</a>
	 */
	private LocalDateTime convertIfModifiedSinceToDate(String headerValue) {

		LocalDateTime time = null;
		try {
			time = LocalDateTime.parse(headerValue, DateTimeFormatter.RFC_1123_DATE_TIME);
		} catch (DateTimeParseException e) {
			// do nothing
		}

		if (time == null) {
			try {
				final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";
				time = LocalDateTime.parse(headerValue, DateTimeFormatter.ofPattern(PATTERN_RFC1036));
			} catch (DateTimeParseException e) {
				// do nothing
			}
		}

		if (time == null) {
			try {
				final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
				time = LocalDateTime.parse(headerValue, DateTimeFormatter.ofPattern(PATTERN_ASCTIME));
			} catch (DateTimeParseException e) {
				// do nothing
			}
		}

		if (time == null) {
			logger.error("Could not parse given if-modified-since header with value '{}'", headerValue);
		}
		return time;
	}
}
