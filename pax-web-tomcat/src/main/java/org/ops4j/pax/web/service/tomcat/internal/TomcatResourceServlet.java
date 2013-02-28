package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.Buffer;
import java.util.regex.Matcher;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ResponseFacade;
import org.apache.naming.resources.Resource;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * can be based on org.apache.catalina.servlets.DefaultServlet
 * @author Romain Gilles
 *         Date: 7/26/12
 *         Time: 10:41 AM
 */
public class TomcatResourceServlet extends HttpServlet
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger( TomcatResourceServlet.class );
	
	// header constants
	private static final String IF_NONE_MATCH = "If-None-Match",
			IF_MATCH = "If-Match", IF_MODIFIED_SINCE = "If-Modified-Since",
			IF_RANGE = "If-Range", IF_UNMODIFIED_SINCE = "If-Unmodified-Since",
			KEEP_ALIVE = "Keep-Alive";

	private static final String ETAG = "ETag";
	
    /**
     * The input buffer size to use when serving resources.
     */
    protected int input = 2048;
    
    private final HttpContext httpContext;
	private final String contextName;
	private final String alias;
	private final String name;
    
    public TomcatResourceServlet(final HttpContext httpContext, final String contextName,
			final String alias, final String name) {
    	this.httpContext = httpContext;
    	this.contextName = "/" + contextName;
    	this.alias = alias;
		if ("/".equals(name)) {
			this.name = "";
		} else {
			this.name = name;
		}
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
    	String mapping = null;
		Boolean included = request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null;
		if (included != null && included) {
			String servletPath = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
			String pathInfo = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
			if (servletPath == null) {
				servletPath = request.getServletPath();
				pathInfo = request.getPathInfo();
			}
//			mapping = URIUtil.addPaths(servletPath, pathInfo);
		} else {
			included = Boolean.FALSE;
			if (contextName.equals(alias)) {
				// special handling since resouceServlet has default name
				// attached to it
				if (!"default".equalsIgnoreCase(name))
					mapping = name + request.getRequestURI();
				else
					mapping = request.getRequestURI();
			} else {
				mapping = request.getRequestURI().replaceFirst(contextName, "/");
				if (!"default".equalsIgnoreCase(name)) {
					mapping = mapping.replaceFirst(alias, Matcher.quoteReplacement(name)); //TODO
				}
			}
		}

		final URL url = httpContext.getResource(mapping);
		if (url == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if ("file".equalsIgnoreCase(url.getProtocol())) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		// For Performanceimprovements turn caching on
		final Resource resource = new Resource(url.openStream());
		try {
			/*
			if (!resource.exists()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			if (resource.isDirectory()) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			*/

			// if the request contains an etag and its the same for the
			// resource, we deliver a NOT MODIFIED response
			
			
			//TODO: add lastModified, probably need to use the caching of the DefaultServlet ...
			/*
			String eTag = String.valueOf(resource.lastModified());
			if ((request.getHeader(IF_NONE_MATCH) != null)
					&& (eTag.equals(request.getHeader(IF_NONE_MATCH)))) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			} else if (request.getHeader(IF_MODIFIED_SINCE) != null) {
				long ifModifiedSince = request.getDateHeader(IF_MODIFIED_SINCE);
				if (resource.lastModified() != -1) {
					// resource.lastModified()/1000 <= ifmsl/1000
					if (resource.lastModified() / 1000 <= ifModifiedSince / 1000) {
						response.reset();
						response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						response.flushBuffer();
						return;
					}
				}
			} else if (request.getHeader(IF_UNMODIFIED_SINCE) != null) {
				long modifiedSince = request.getDateHeader(IF_UNMODIFIED_SINCE);

				if (modifiedSince != -1) {
					if (resource.lastModified() / 1000 > modifiedSince / 1000) {
						response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
						return;
					}
				}
			}
			*/
			// set the etag
//			response.setHeader(ETAG, eTag);
//			String mimeType = m_httpContext.getMimeType(mapping);
			String mimeType = getServletContext().getMimeType(url.getFile());
			/*
			 * No Fallback
			if (mimeType == null) {
				Buffer mimeTypeBuf = mimeTypes.getMimeByExtension(mapping);
				mimeType = mimeTypeBuf != null ? mimeTypeBuf.toString() : null;
			}
			*/

			if (mimeType == null) {
				try {
					mimeType = url.openConnection().getContentType();
				} catch (IOException ignore) {
					// we do not care about such an exception as the fact that
					// we are using also the connection for
					// finding the mime type is just a "nice to have" not an
					// requirement
				}
			}
			
			if (mimeType == null) {
				ServletContext servletContext = getServletConfig().getServletContext();
				mimeType = servletContext.getMimeType(mapping);
			}
			
			if (mimeType != null) {
				response.setContentType(mimeType);
				// TODO shall we handle also content encoding?
			}

			ServletOutputStream out = response.getOutputStream();
			if (out != null) // null should be just in unit testing
			{
				ServletResponse r = response;
		        long contentWritten = 0;
		        while (r instanceof ServletResponseWrapper) {
		            r = ((ServletResponseWrapper) r).getResponse();
		        }
		        if (r instanceof ResponseFacade) {
		            contentWritten = ((ResponseFacade) r).getContentWritten();
		        }
		        
				copyRange(url.openStream(), out);
					
			}
			response.setStatus(HttpServletResponse.SC_OK);
		} finally {
			//
		}
    }
    
    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param istream The input stream to read from
     * @param ostream The output stream to write to
     * @return Exception which occurred during processing
     */
    protected IOException copyRange(InputStream istream,
                                  ServletOutputStream ostream) {

        // Copy the input stream to the output stream
        IOException exception = null;
        byte buffer[] = new byte[input];
        int len = buffer.length;
        while (true) {
            try {
                len = istream.read(buffer);
                if (len == -1)
                    break;
                ostream.write(buffer, 0, len);
            } catch (IOException e) {
                exception = e;
                len = -1;
                break;
            }
        }
        return exception;

    }
}
