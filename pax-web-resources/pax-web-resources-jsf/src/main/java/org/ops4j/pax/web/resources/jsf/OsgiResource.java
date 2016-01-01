package org.ops4j.pax.web.resources.jsf;

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
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom resource-implementation because other implementations are tied to
 * either Mojarra or Myfaces.
 */
public class OsgiResource extends Resource {

	/**
     * Identifies the FacesServlet mapping in the current request map.
     */
    private static final String CACHED_SERVLET_MAPPING = OsgiResource.class.getName() + ".CACHED_SERVLET_MAPPING";
	
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

	@Override
	public String getRequestPath() {
		final FacesContext facesContext = FacesContext.getCurrentInstance();
		
		FacesServletMapping servletMapping = getFacesServletMapping(facesContext);
        
        String path = ResourceHandler.RESOURCE_IDENTIFIER + '/' + getResourceName();
        
        final StringBuilder pathBuilder;
        if(servletMapping.isExtensionMapping()){
        	pathBuilder = new StringBuilder(path).append(servletMapping.getMapping());
        }else {
        	pathBuilder = new StringBuilder(servletMapping.getMapping()).append(path);
        }
		
        
        List<String> optionalParameters = new ArrayList<>(5);
		if (getLibraryName() != null) {
			optionalParameters.add("ln=" + getLibraryName());
		}
		if (!facesContext.isProjectStage(ProjectStage.Production))
        {
            // append stage for all ProjectStages except Production
			optionalParameters.add("stage=" + facesContext.getApplication().getProjectStage().toString());
        }
		
		// concat optional parameter with & and add as request-parameters
		String parameterString = optionalParameters.stream().collect(Collectors.joining("&"));
		if(StringUtils.isNotBlank(parameterString)){
			pathBuilder.append("?").append(parameterString);
		}
		
		return facesContext.getApplication().getViewHandler().getResourceURL(facesContext, pathBuilder.toString());
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
	public boolean userAgentNeedsUpdate(final FacesContext facesContext) {
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
		if (!facesContext.isProjectStage(ProjectStage.Development)) {
			final Optional<String> ifModSinceHeader = Optional
					.ofNullable(facesContext.getExternalContext().getRequestHeaderMap().get("If-Modified-Since"));
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
			logger.trace("could not parse date with RFC-1123. Will try RFC-1036 format...");
		}

		if (time == null) {
			try {
				final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";
				time = LocalDateTime.parse(headerValue, DateTimeFormatter.ofPattern(PATTERN_RFC1036));
			} catch (DateTimeParseException e) {
                          	logger.trace("could not parse date with RFC-1036. Will try ASCITIME format...");
			}
		}

		if (time == null) {
			try {
				final String PATTERN_ASCITIME = "EEE MMM d HH:mm:ss yyyy";
				time = LocalDateTime.parse(headerValue, DateTimeFormatter.ofPattern(PATTERN_ASCITIME));
			} catch (DateTimeParseException e) {
                            	logger.trace("could not parse date with ASCITIME.");
			}
		}

		if (time == null) {
			logger.error("Could not parse given if-modified-since header with value '{}'", headerValue);
		}
		return time;
	}


	
	private FacesServletMapping getFacesServletMapping(final FacesContext facesContext) {
		Map<Object, Object> attributes = facesContext.getAttributes();

        // Has the mapping already been determined during this request?
        FacesServletMapping mapping = (FacesServletMapping) attributes.get(CACHED_SERVLET_MAPPING);
        if (mapping == null)
        {
            ExternalContext externalContext = facesContext.getExternalContext();
            mapping = FacesServletMapping.calculateFacesServletMapping(externalContext.getRequestServletPath(),
                    externalContext.getRequestPathInfo());

            attributes.put(CACHED_SERVLET_MAPPING, mapping);
        }
        return mapping;
	}

	/**
	 * Represents a mapping entry of the FacesServlet in the web.xml
	 * configuration file.
	 */
	private static final class FacesServletMapping {
		 /**
	     * The path ("/faces", for example) which has been specified in the
	     * url-pattern of the FacesServlet mapping.
	     */
	    private String prefix;

	    /**
	     * The extension (".jsf", for example) which has been specified in the
	     * url-pattern of the FacesServlet mapping.
	     */
	    private String extension;

	    /**
	     * Creates a new FacesServletMapping object using prefix mapping.
	     *
	     * @param path The path ("/faces", for example) which has been specified
	     *             in the url-pattern of the FacesServlet mapping.
	     * @return a newly created FacesServletMapping
	     */
	    public static FacesServletMapping createPrefixMapping(String path)
	    {
	        FacesServletMapping mapping = new FacesServletMapping();
	        mapping.setPrefix(path);
	        return mapping;
	    }

	    /**
	     * Creates a new FacesServletMapping object using extension mapping.
	     *
	     * @param path The extension (".xhtml", for example) which has been
	     *             specified in the url-pattern of the FacesServlet mapping.
	     * @return a newly created FacesServletMapping
	     */
	    public static FacesServletMapping createExtensionMapping(
	        String extension)
	    {
	        FacesServletMapping mapping = new FacesServletMapping();
	        mapping.setExtension(extension);
	        return mapping;
	    }

	    /**
	     * Returns the path ("/faces", for example) which has been specified in
	     * the url-pattern of the FacesServlet mapping. If this mapping is based
	     * on an extension, <code>null</code> will be returned. Note that this
	     * path is not the same as the specified url-pattern as the trailing
	     * "/*" is omitted.
	     *
	     * @return the path which has been specified in the url-pattern
	     */
	    public String getPrefix()
	    {
	        return prefix;
	    }

	    /**
	     * Sets the path ("/faces/", for example) which has been specified in
	     * the url-pattern.
	     *
	     * @param path The path which has been specified in the url-pattern
	     */
	    public void setPrefix(String path)
	    {
	        this.prefix = path;
	    }

	    /**
	     * Returns the extension (".jsf", for example) which has been specified
	     * in the url-pattern of the FacesServlet mapping. If this mapping is
	     * not based on an extension, <code>null</code> will be returned.
	     *
	     * @return the extension which has been specified in the url-pattern
	     */
	    public String getExtension()
	    {
	        return extension;
	    }

	    /**
	     * Sets the extension (".jsf", for example) which has been specified in
	     * the url-pattern of the FacesServlet mapping.
	     *
	     * @param extension The extension which has been specified in the url-pattern
	     */
	    public void setExtension(String extension)
	    {
	        this.extension = extension;
	    }

	    /**
	     * Indicates whether this mapping is based on an extension (e.g.
	     * "*.xhtml").
	     *
	     * @return <code>true</code>, if this mapping is based is on an
	     *         extension, <code>false</code> otherwise
	     */
	    public boolean isExtensionMapping()
	    {
	        return extension != null;
	    }

	    public String getMapping()
	    {
            if (isExtensionMapping())
            {
                return getExtension();
            }
            else
            {
                return getPrefix();
            }
	    }
	    
	    /**
	     * Determines the mapping of the FacesServlet in the web.xml configuration
	     * file. However, there is no need to actually parse this configuration file
	     * as runtime information is sufficient.
	     *
	     * @param servletPath The servletPath of the current request
	     * @param pathInfo    The pathInfo of the current request
	     * @return the mapping of the FacesServlet in the web.xml configuration file
	     */
	    protected static FacesServletMapping calculateFacesServletMapping(
	        String servletPath, String pathInfo)
	    {
	        if (pathInfo != null)
	        {
	            // If there is a "extra path", it's definitely no extension mapping.
	            // Now we just have to determine the path which has been specified
	            // in the url-pattern, but that's easy as it's the same as the
	            // current servletPath. It doesn't even matter if "/*" has been used
	            // as in this case the servletPath is just an empty string according
	            // to the Servlet Specification (SRV 4.4).
	            return FacesServletMapping.createPrefixMapping(servletPath);
	        }
	        else
	        {
	            // In the case of extension mapping, no "extra path" is available.
	            // Still it's possible that prefix-based mapping has been used.
	            // Actually, if there was an exact match no "extra path"
	            // is available (e.g. if the url-pattern is "/faces/*"
	            // and the request-uri is "/context/faces").
	            int slashPos = servletPath.lastIndexOf('/');
	            int extensionPos = servletPath.lastIndexOf('.');
	            if (extensionPos > -1 && extensionPos > slashPos)
	            {
	                String extension = servletPath.substring(extensionPos);
	                return FacesServletMapping.createExtensionMapping(extension);
	            }
	            else
	            {
	                // There is no extension in the given servletPath and therefore
	                // we assume that it's an exact match using prefix-based mapping.
	                return FacesServletMapping.createPrefixMapping(servletPath);
	            }
	        }
	    }
	}
	
}
