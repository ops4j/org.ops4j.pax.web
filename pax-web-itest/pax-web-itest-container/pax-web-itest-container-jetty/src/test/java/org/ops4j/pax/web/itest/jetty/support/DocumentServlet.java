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
 package org.ops4j.pax.web.itest.jetty.support;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.io.WriterOutputStream;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.*;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

@Component(immediate = true, provide = Servlet.class, properties = "alias=/document")
public class DocumentServlet extends HttpServlet implements ResourceFactory {
	private static final long serialVersionUID = 4930458713846881193L;

	private static final PreEncodedHttpField ACCEPT_RANGES = new PreEncodedHttpField(HttpHeader.ACCEPT_RANGES, "bytes");
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private String resourcePath;

	private ServletContext servletContext;
	private ContextHandler contextHandler;

	private boolean acceptRanges = true;
	private boolean dirAllowed = true;
	private boolean welcomeServlets;
	private boolean welcomeExactServlets;
	private boolean redirectWelcome;
	private boolean gzip;
	private boolean pathInfoOnly;
	private boolean etags = false;

	private Resource resourceBase;
	private ResourceCache cache;

	private MimeTypes mimeTypes;
	private String[] welcomes;
	private Resource stylesheet;
	private boolean useFileMappedBuffer;
	private HttpField cacheControl;
	private String relativeResourceBase;
	private ServletHandler servletHandler;
	private ServletHolder defaultHolder;
	private List<String> gzipEquivalentFileExtensions;

	@Activate
	public void activate() {
		logger.info("Document servlet started");
		resourcePath = "./target";
	}

	/* ------------------------------------------------------------ */
	@Override
	public void init() throws UnavailableException {
		servletContext = getServletContext();
		contextHandler = initContextHandler(servletContext);

		mimeTypes = contextHandler.getMimeTypes();

		welcomes = contextHandler.getWelcomeFiles();
		if (welcomes == null) {
			welcomes = new String[] { "index.html", "index.jsp" };
		}

		acceptRanges = getInitBoolean("acceptRanges", acceptRanges);
		dirAllowed = getInitBoolean("dirAllowed", dirAllowed);
		redirectWelcome = getInitBoolean("redirectWelcome", redirectWelcome);
		gzip = getInitBoolean("gzip", gzip);
		pathInfoOnly = getInitBoolean("pathInfoOnly", pathInfoOnly);

		if ("exact".equals(getInitParameter("welcomeServlets"))) {
			welcomeExactServlets = true;
			welcomeServlets = false;
		} else {
			welcomeServlets = getInitBoolean("welcomeServlets", welcomeServlets);
		}

		useFileMappedBuffer = getInitBoolean("useFileMappedBuffer",
				useFileMappedBuffer);

		relativeResourceBase = getInitParameter("relativeResourceBase");

		String rb = resourcePath;
		if (rb != null) {
			if (relativeResourceBase != null) {
				throw new UnavailableException(
						"resourceBase & relativeResourceBase");
			}
			try {
				resourceBase = contextHandler.newResource(rb);
				//CHECKSTYLE:OFF
			} catch (Exception e) { 
				logger.warn(Log.EXCEPTION, e);
				throw new UnavailableException(e.toString());
			}
			//CHECKSTYLE:ON
		}

		String css = getInitParameter("stylesheet");
		try {
			if (css != null) {
				stylesheet = Resource.newResource(css);
				if (!stylesheet.exists()) {
					logger.warn("!" + css);
					stylesheet = null;
				}
			}
			if (stylesheet == null) {
				stylesheet = Resource.newResource(this.getClass().getResource(
						"/jetty-dir.css"));
			}
			//CHECKSTYLE:OFF
		} catch (Exception e) {
			logger.warn(e.toString(), e);
		}
		//CHECKSTYLE:ON

		String cc = getInitParameter("cacheControl");
		if (cc != null)
			cacheControl = new PreEncodedHttpField(HttpHeader.CACHE_CONTROL, cc);

		String resourceCache = getInitParameter("resourceCache");
		int maxCacheSize = getInitInt("maxCacheSize", -2);
		int maxCachedFileSize = getInitInt("maxCachedFileSize", -2);
		int maxCachedFiles = getInitInt("maxCachedFiles", -2);
		if (resourceCache != null) {
			if (maxCacheSize != -1 || maxCachedFileSize != -2
					|| maxCachedFiles != -2) {
				logger.debug("ignoring resource cache configuration, using resourceCache attribute");
			}
			if (relativeResourceBase != null || resourceBase != null) {
				throw new UnavailableException(
						"resourceCache specified with resource bases");
			}
			cache = (ResourceCache) servletContext.getAttribute(resourceCache);

			logger.debug("Cache {}={}", resourceCache, cache);
		}

		etags = getInitBoolean("etags", etags);

		try {
			if (cache == null && maxCachedFiles > 0) {
				cache = new ResourceCache(null, this, mimeTypes,
						useFileMappedBuffer, true, gzip);

				if (maxCacheSize > 0) {
					cache.setMaxCacheSize(maxCacheSize);
				}
				if (maxCachedFileSize >= -1) {
					cache.setMaxCachedFileSize(maxCachedFileSize);
				}
				if (maxCachedFiles >= -1) {
					cache.setMaxCachedFiles(maxCachedFiles);
				}
			}
			//CHECKSTYLE:OFF
		} catch (Exception e) { 
			logger.warn(Log.EXCEPTION, e);
			throw new UnavailableException(e.toString());
		}
		//CHECKSTYLE:ON

		gzipEquivalentFileExtensions = new ArrayList<String>();
		String otherGzipExtensions = getInitParameter("otherGzipFileExtensions");
		if (otherGzipExtensions != null) {
			// comma separated list
			StringTokenizer tok = new StringTokenizer(otherGzipExtensions, ",",
					false);
			while (tok.hasMoreTokens()) {
				String s = tok.nextToken().trim();
				gzipEquivalentFileExtensions.add((s.charAt(0) == '.' ? s : "."
						+ s));
			}
		} else {
			// .svgz files are gzipped svg files and must be served with
			// Content-Encoding:gzip
			gzipEquivalentFileExtensions.add(".svgz");
		}

		servletHandler = contextHandler
				.getChildHandlerByClass(ServletHandler.class);
		for (ServletHolder h : servletHandler.getServlets()) {
			if (h.getServletInstance() == this) {
				defaultHolder = h;
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("resource base = " + resourceBase);
		}
	}

	/**
	 * Compute the field _contextHandler.<br/>
	 * In the case where the DefaultServlet is deployed on the HttpService it is
	 * likely that this method needs to be overwritten to unwrap the
	 * ServletContext facade until we reach the original jetty's ContextHandler.
	 * 
	 * @param servletContext
	 *            The servletContext of this servlet.
	 * @return the jetty's ContextHandler for this servletContext.
	 */
	protected ContextHandler initContextHandler(ServletContext servletContext) {
		ContextHandler.Context scontext = ContextHandler.getCurrentContext();
		if (scontext == null) {
			if (servletContext instanceof ContextHandler.Context) {
				return ((ContextHandler.Context) servletContext)
						.getContextHandler();
			} else {
				throw new IllegalArgumentException("The servletContext "
						+ servletContext + " "
						+ servletContext.getClass().getName() + " is not "
						+ ContextHandler.Context.class.getName());
			}
		} else {
			return ContextHandler.getCurrentContext().getContextHandler();
		}
	}

	/* ------------------------------------------------------------ */
	@Override
	public String getInitParameter(String name) {
		String value = getServletContext().getInitParameter(
				"org.eclipse.jetty.servlet.Default." + name);
		if (value == null) {
			value = super.getInitParameter(name);
		}
		return value;
	}

	/* ------------------------------------------------------------ */
	private boolean getInitBoolean(String name, boolean dft) {
		String value = getInitParameter(name);
		if (value == null || value.length() == 0) {
			return dft;
		}
		return (value.startsWith("t") || value.startsWith("T")
				|| value.startsWith("y") || value.startsWith("Y") || value
					.startsWith("1"));
	}

	/* ------------------------------------------------------------ */
	private int getInitInt(String name, int dft) {
		String value = getInitParameter(name);
		if (value == null) {
			value = getInitParameter(name);
		}
		if (value != null && value.length() > 0) {
			return Integer.parseInt(value);
		}
		return dft;
	}

	/* ------------------------------------------------------------ */
	/**
	 * get Resource to serve. Map a path to a resource. The default
	 * implementation calls HttpContext.getResource but derived servlets may
	 * provide their own mapping.
	 * 
	 * @param pathInContext
	 *            The path to find a resource for.
	 * @return The resource to serve.
	 */
	@Override
	public Resource getResource(String pathInContext) {
		Resource r = null;
		if (relativeResourceBase != null) {
			pathInContext = URIUtil.addPaths(relativeResourceBase,
					pathInContext);
		}

		try {
			if (resourceBase != null) {
				r = resourceBase.addPath(pathInContext);
			} else {
				URL u = servletContext.getResource(pathInContext);
				r = contextHandler.newResource(u);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Resource " + pathInContext + "=" + r);
			}
		} catch (IOException e) {
			// do nothing
		}

		if ((r == null || !r.exists())
				&& pathInContext.endsWith("/jetty-dir.css")) {
			r = stylesheet;
		}

		return r;
	}

	/* ------------------------------------------------------------ */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String servletPath = null;
		String pathInfo = null;
		Enumeration<String> reqRanges = null;
		Boolean included = request
				.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null;
		if (included != null && included.booleanValue()) {
			servletPath = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
			pathInfo = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
			if (servletPath == null) {
				servletPath = request.getServletPath();
				pathInfo = request.getPathInfo();
			}
		} else {
			included = Boolean.FALSE;
			servletPath = pathInfoOnly ? "/" : request.getServletPath();
			pathInfo = request.getPathInfo();

			// Is this a Range request?
			reqRanges = request.getHeaders(HttpHeader.RANGE.asString());
			if (!hasDefinedRange(reqRanges))
				reqRanges = null;
		}

		String pathInContext = URIUtil.addPaths(servletPath, pathInfo);
		boolean endsWithSlash = (pathInfo == null ? request.getServletPath()
				: pathInfo).endsWith(URIUtil.SLASH);

		// Find the resource and content
		Resource resource = null;
		HttpContent content = null;
		try {
			// is gzip enabled?
			String pathInContextGz = null;
			boolean gzipDefault = false;
			if (!included.booleanValue() && gzip && reqRanges == null
					&& !endsWithSlash) {
				// Look for a gzip resource
				pathInContextGz = pathInContext + ".gz";
				if (cache == null)
					resource = getResource(pathInContextGz);
				else {
					content = cache.lookup(pathInContextGz);
					resource = (content == null) ? null : content.getResource();
				}

				// Does a gzip resource exist?
				if (resource != null && resource.exists()
						&& !resource.isDirectory()) {
					// Tell caches that response may vary by accept-encoding
					response.addHeader(HttpHeader.VARY.asString(),
							HttpHeader.ACCEPT_ENCODING.asString());

					// Does the client accept gzip?
					String accept = request
							.getHeader(HttpHeader.ACCEPT_ENCODING.asString());
					if (accept != null && accept.indexOf("gzip") >= 0)
						gzipDefault = true;
				}
			}

			// find resource
			if (!gzipDefault) {
				if (cache == null)
					resource = getResource(pathInContext);
				else {
					content = cache.lookup(pathInContext);
					resource = content == null ? null : content.getResource();
				}
			}

			if (logger.isDebugEnabled())
				logger.debug("uri=" + request.getRequestURI() + " resource="
						+ resource + (content != null ? " content" : ""));

			// Handle resource
			if (resource == null || !resource.exists()) {
				if (included)
					throw new FileNotFoundException("!" + pathInContext);
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} else if (!resource.isDirectory()) {
				if (endsWithSlash && pathInContext.length() > 1) {
					String q = request.getQueryString();
					pathInContext = pathInContext.substring(0,
							pathInContext.length() - 1);
					if (q != null && q.length() != 0)
						pathInContext += "?" + q;
					response.sendRedirect(response.encodeRedirectURL(URIUtil
							.addPaths(servletContext.getContextPath(),
									pathInContext)));
				} else {
					// ensure we have content
					if (content == null)
						content=new ResourceHttpContent(resource,mimeTypes.getMimeByExtension(resource.toString()),response.getBufferSize());

					if (included.booleanValue()
							|| passConditionalHeaders(request, response,
									resource, content)) {
						if (gzipDefault) {
							response.setHeader(
									HttpHeader.CONTENT_ENCODING.asString(),
									"gzip");
							String mt = servletContext
									.getMimeType(pathInContext);
							if (mt != null)
								response.setContentType(mt);
						}
						sendData(request, response, included.booleanValue(),
								resource, content, reqRanges);
					}
				}
			} else {
				String welcome = null;

				if (!endsWithSlash
						|| (pathInContext.length() == 1 && request
								.getAttribute("org.eclipse.jetty.server.nullPathInfo") != null)) {
					StringBuffer buf = request.getRequestURL();
					synchronized (buf) {
						int param = buf.lastIndexOf(";");
						if (param < 0)
							buf.append('/');
						else
							buf.insert(param, '/');
						String q = request.getQueryString();
						if (q != null && q.length() != 0) {
							buf.append('?');
							buf.append(q);
						}
						response.setContentLength(0);
						response.sendRedirect(response.encodeRedirectURL(buf
								.toString()));
					}
				}
				// else look for a welcome file
				else if (null != (welcome = getWelcomeFile(pathInContext))) {
					logger.debug("welcome={}", welcome);
					if (redirectWelcome) {
						// Redirect to the index
						response.setContentLength(0);
						String q = request.getQueryString();
						if (q != null && q.length() != 0)
							response.sendRedirect(response
									.encodeRedirectURL(URIUtil.addPaths(
											servletContext.getContextPath(),
											welcome)
											+ "?" + q));
						else
							response.sendRedirect(response
									.encodeRedirectURL(URIUtil.addPaths(
											servletContext.getContextPath(),
											welcome)));
					} else {
						// Forward to the index
						RequestDispatcher dispatcher = request
								.getRequestDispatcher(welcome);
						if (dispatcher != null) {
							if (included.booleanValue())
								dispatcher.include(request, response);
							else {
								request.setAttribute(
										"org.eclipse.jetty.server.welcome",
										welcome);
								dispatcher.forward(request, response);
							}
						}
					}
				} else {
					content = new ResourceHttpContent(resource,mimeTypes.getMimeByExtension(resource.toString()));
					if (included.booleanValue()
							|| passConditionalHeaders(request, response,
									resource, content))
						sendDirectory(request, response, resource,
								pathInContext);
				}
			}
		} catch (IllegalArgumentException e) {
			logger.warn(Log.EXCEPTION, e);
			if (!response.isCommitted())
				response.sendError(500, e.getMessage());
		} finally {
			if (content != null)
				content.release();
			else if (resource != null)
				resource.release();
		}

	}

	/* ------------------------------------------------------------ */
	private boolean hasDefinedRange(Enumeration<String> reqRanges) {
		return (reqRanges != null && reqRanges.hasMoreElements());
	}

	/* ------------------------------------------------------------ */
	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	/* ------------------------------------------------------------ */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doTrace(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/* ------------------------------------------------------------ */
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setHeader("Allow", "GET,HEAD,POST,OPTIONS");
	}

	/* ------------------------------------------------------------ */
	/**
	 * Finds a matching welcome file for the supplied {@link Resource}. This
	 * will be the first entry in the list of configured {@link #welcomes
	 * welcome files} that existing within the directory referenced by the
	 * <code>Resource</code>. If the resource is not a directory, or no matching
	 * file is found, then it may look for a valid servlet mapping. If there is
	 * none, then <code>null</code> is returned. The list of welcome files is
	 * read from the {@link ContextHandler} for this servlet, or
	 * <code>"index.jsp" , "index.html"</code> if that is <code>null</code>.
	 * 
	 * @param resource
	 * @return The path of the matching welcome file in context or null.
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private String getWelcomeFile(String pathInContext) throws IOException {
		if (welcomes == null) {
			return null;
		}

		String welcomeServlet = null;
		for (int i = 0; i < welcomes.length; i++) {
			String welcomeInContext = URIUtil.addPaths(pathInContext,
					welcomes[i]);
			Resource welcome = getResource(welcomeInContext);
			if (welcome != null && welcome.exists()) {
				return welcomes[i];
			}

			if ((welcomeServlets || welcomeExactServlets)
					&& welcomeServlet == null) {
				Map.Entry<?, ?> entry = servletHandler
						.getHolderEntry(welcomeInContext);
				if (entry != null
						&& entry.getValue() != defaultHolder
						&& (welcomeServlets || (welcomeExactServlets && entry
								.getKey().equals(welcomeInContext)))) {
					welcomeServlet = welcomeInContext;
				}

			}
		}
		return welcomeServlet;
	}

	/* ------------------------------------------------------------ */
	/*
	 * Check modification date headers.
	 */
	protected boolean passConditionalHeaders(HttpServletRequest request,
			HttpServletResponse response, Resource resource, HttpContent content)
			throws IOException {
		try
        {
            String ifm=null;
            String ifnm=null;
            String ifms=null;
            long ifums=-1;
            
            if (request instanceof Request)
            {
                HttpFields fields = ((Request)request).getHttpFields();
                for (int i=fields.size();i-->0;)
                {
                    HttpField field=fields.getField(i);
                    if (field.getHeader() != null)
                    {
                        switch (field.getHeader())
                        {
                            case IF_MATCH:
                                ifm=field.getValue();
                                break;
                            case IF_NONE_MATCH:
                                ifnm=field.getValue();
                                break;
                            case IF_MODIFIED_SINCE:
                                ifms=field.getValue();
                                break;
                            case IF_UNMODIFIED_SINCE:
                                ifums=DateParser.parseDate(field.getValue());
                                break;
                            default:
                        }
                    }
                }
            }
            else
            {
                ifm=request.getHeader(HttpHeader.IF_MATCH.asString());
                ifnm=request.getHeader(HttpHeader.IF_NONE_MATCH.asString());
                ifms=request.getHeader(HttpHeader.IF_MODIFIED_SINCE.asString());
                ifums=request.getDateHeader(HttpHeader.IF_UNMODIFIED_SINCE.asString());
            }
            
            if (!HttpMethod.HEAD.is(request.getMethod()))
            {
                if (etags)
                {
                    if (ifm!=null)
                    {
                        boolean match=false;
                        if (content.getETagValue()!=null)
                        {
                            QuotedStringTokenizer quoted = new QuotedStringTokenizer(ifm,", ",false,true);
                            while (!match && quoted.hasMoreTokens())
                            {
                                String tag = quoted.nextToken();
                                if (content.getETagValue().equals(tag))
                                    match=true;
                            }
                        }

                        if (!match)
                        {
                            response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                            return false;
                        }
                    }
                    
                    if (ifnm!=null && content.getETagValue()!=null)
                    {
                        // Look for Gzip'd version of etag
                        if (content.getETagValue().equals(request.getAttribute("o.e.j.s.Gzip.ETag")))
                        {
                            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                            response.setHeader(HttpHeader.ETAG.asString(),ifnm);
                            return false;
                        }
                        
                        // Handle special case of exact match.
                        if (content.getETagValue().equals(ifnm))
                        {
                            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                            response.setHeader(HttpHeader.ETAG.asString(),content.getETagValue());
                            return false;
                        }

                        // Handle list of tags
                        QuotedStringTokenizer quoted = new QuotedStringTokenizer(ifnm,", ",false,true);
                        while (quoted.hasMoreTokens())
                        {
                            String tag = quoted.nextToken();
                            if (content.getETagValue().equals(tag))
                            {
                                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                                response.setHeader(HttpHeader.ETAG.asString(),content.getETagValue());
                                return false;
                            }
                        }
                        
                        // If etag requires content to be served, then do not check if-modified-since
                        return true;
                    }
                }
                
                // Handle if modified since
                if (ifms!=null)
                {
                    //Get jetty's Response impl
                    String mdlm=content.getLastModifiedValue();
                    if (mdlm!=null && ifms.equals(mdlm))
                    {
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                        if (etags)
                            response.setHeader(HttpHeader.ETAG.asString(),content.getETagValue());
                        response.flushBuffer();
                        return false;
                    }

                    long ifmsl=request.getDateHeader(HttpHeader.IF_MODIFIED_SINCE.asString());
                    if (ifmsl!=-1 && resource.lastModified()/1000 <= ifmsl/1000)
                    { 
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                        if (etags)
                            response.setHeader(HttpHeader.ETAG.asString(),content.getETagValue());
                        response.flushBuffer();
                        return false;
                    }
                }

                // Parse the if[un]modified dates and compare to resource
                if (ifums!=-1 && resource.lastModified()/1000 > ifums/1000)
                {
                    response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                    return false;
                }

            }
        }
        catch(IllegalArgumentException iae)
        {
            if(!response.isCommitted())
                response.sendError(400, iae.getMessage());
            throw iae;
        }
        return true;
	}

	/* ------------------------------------------------------------------- */
	protected void sendDirectory(HttpServletRequest request,
			HttpServletResponse response, Resource resource,
			String pathInContext) throws IOException {
		if (!dirAllowed) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		byte[] data = null;
		String base = URIUtil.addPaths(request.getRequestURI(), URIUtil.SLASH);

		// handle ResourceCollection
		if (resourceBase instanceof ResourceCollection) {
			resource = resourceBase.addPath(pathInContext);
		} else if (contextHandler.getBaseResource() instanceof ResourceCollection) {
			resource = contextHandler.getBaseResource().addPath(pathInContext);
		}

		String dir = resource.getListHTML(base, pathInContext.length() > 1);
		if (dir == null) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "No directory");
			return;
		}

		data = dir.getBytes("UTF-8");
		response.setContentType("text/html; charset=UTF-8");
		response.setContentLength(data.length);
		response.getOutputStream().write(data);
	}

	/* ------------------------------------------------------------ */
	protected void sendData(HttpServletRequest request,
			HttpServletResponse response, boolean include, Resource resource,
			HttpContent content, Enumeration<String> reqRanges)
			throws IOException {
final long content_length = (content==null)?resource.length():content.getContentLengthValue();
        
        // Get the output stream (or writer)
        OutputStream out =null;
        boolean written;
        try
        {
            out = response.getOutputStream();

            // has a filter already written to the response?
            written = out instanceof HttpOutput
                ? ((HttpOutput)out).isWritten()
                : true;
        }
        catch(IllegalStateException e)
        {
            out = new WriterOutputStream(response.getWriter());
            written=true; // there may be data in writer buffer, so assume written
        }
        
        if (logger.isDebugEnabled())
            logger.debug(String.format("sendData content=%s out=%s async=%b",content,out,request.isAsyncSupported()));

        if ( reqRanges == null || !reqRanges.hasMoreElements() || content_length<0)
        {
            //  if there were no ranges, send entire entity
            if (include)
            {
                // write without headers
                resource.writeTo(out,0,content_length);
            }
            // else if we can't do a bypass write because of wrapping
            else if (content==null || written || !(out instanceof HttpOutput))
            {
                // write normally
                putHeaders(response,content,written?-1:0);
                ByteBuffer buffer = (content==null)?null:content.getIndirectBuffer();
                if (buffer!=null)
                    BufferUtil.writeTo(buffer,out);
                else
                    resource.writeTo(out,0,content_length);
            }
            // else do a bypass write
            else
            {
                // write the headers
                putHeaders(response,content,0);

                // write the content asynchronously if supported
                if (request.isAsyncSupported())
                {
                    final AsyncContext context = request.startAsync();
                    context.setTimeout(0);

                    ((HttpOutput)out).sendContent(content,new Callback()
                    {
                        @Override
                        public void succeeded()
                        {   
                            context.complete();
                        }

                        @Override
                        public void failed(Throwable x)
                        {
                            if (x instanceof IOException)
                                logger.debug("Failed to write Header", x);
                            else
                                logger.warn("Failed to write Header", x);
                            context.complete();
                        }
                        
                        @Override
                        public String toString() 
                        {
                            return String.format("DefaultServlet@%x$CB", DocumentServlet.this.hashCode());
                        }
                    });
                }
                // otherwise write content blocking
                else
                {
                    ((HttpOutput)out).sendContent(content);
                }
            }
        }
        else
        {
            // Parse the satisfiable ranges
            List<InclusiveByteRange> ranges =InclusiveByteRange.satisfiableRanges(reqRanges,content_length);

            //  if there are no satisfiable ranges, send 416 response
            if (ranges==null || ranges.size()==0)
            {
                putHeaders(response,content,0);
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader(HttpHeader.CONTENT_RANGE.asString(),
                        InclusiveByteRange.to416HeaderRangeString(content_length));
                resource.writeTo(out,0,content_length);
                return;
            }

            //  if there is only a single valid range (must be satisfiable
            //  since were here now), send that range with a 216 response
            if ( ranges.size()== 1)
            {
                InclusiveByteRange singleSatisfiableRange = ranges.get(0);
                long singleLength = singleSatisfiableRange.getSize(content_length);
                putHeaders(response,content,singleLength);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                if (!response.containsHeader(HttpHeader.DATE.asString()))
                    response.addDateHeader(HttpHeader.DATE.asString(),System.currentTimeMillis());
                response.setHeader(HttpHeader.CONTENT_RANGE.asString(),
                        singleSatisfiableRange.toHeaderRangeString(content_length));
                resource.writeTo(out,singleSatisfiableRange.getFirst(content_length),singleLength);
                return;
            }

            //  multiple non-overlapping valid ranges cause a multipart
            //  216 response which does not require an overall
            //  content-length header
            //
            putHeaders(response,content,-1);
            String mimetype=(content==null?null:content.getContentTypeValue());
            if (mimetype==null)
                logger.warn("Unknown mimetype for "+request.getRequestURI());
            MultiPartOutputStream multi = new MultiPartOutputStream(out);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            if (!response.containsHeader(HttpHeader.DATE.asString()))
                response.addDateHeader(HttpHeader.DATE.asString(),System.currentTimeMillis());

            // If the request has a "Request-Range" header then we need to
            // send an old style multipart/x-byteranges Content-Type. This
            // keeps Netscape and acrobat happy. This is what Apache does.
            String ctp;
            if (request.getHeader(HttpHeader.REQUEST_RANGE.asString())!=null)
                ctp = "multipart/x-byteranges; boundary=";
            else
                ctp = "multipart/byteranges; boundary=";
            response.setContentType(ctp+multi.getBoundary());

            InputStream in=resource.getInputStream();
            long pos=0;

            // calculate the content-length
            int length=0;
            String[] header = new String[ranges.size()];
            for (int i=0;i<ranges.size();i++)
            {
                InclusiveByteRange ibr = ranges.get(i);
                header[i]=ibr.toHeaderRangeString(content_length);
                length+=
                    ((i>0)?2:0)+
                    2+multi.getBoundary().length()+2+
                    (mimetype==null?0:HttpHeader.CONTENT_TYPE.asString().length()+2+mimetype.length())+2+
                    HttpHeader.CONTENT_RANGE.asString().length()+2+header[i].length()+2+
                    2+
                    (ibr.getLast(content_length)-ibr.getFirst(content_length))+1;
            }
            length+=2+2+multi.getBoundary().length()+2+2;
            response.setContentLength(length);

            for (int i=0;i<ranges.size();i++)
            {
                InclusiveByteRange ibr =  ranges.get(i);
                multi.startPart(mimetype,new String[]{HttpHeader.CONTENT_RANGE+": "+header[i]});

                long start=ibr.getFirst(content_length);
                long size=ibr.getSize(content_length);
                if (in!=null)
                {
                    // Handle non cached resource
                    if (start<pos)
                    {
                        in.close();
                        in=resource.getInputStream();
                        pos=0;
                    }
                    if (pos<start)
                    {
                        in.skip(start-pos);
                        pos=start;
                    }
                    
                    IO.copy(in,multi,size);
                    pos+=size;
                }
                else
                    // Handle cached resource
                    (resource).writeTo(multi,start,size);
            }
            if (in!=null)
                in.close();
            multi.close();
        }
        return;
	}
	
	/* ------------------------------------------------------------ */
    protected void putHeaders(HttpServletResponse response,HttpContent content, long contentLength)
    {
        if (response instanceof Response)
        {
            Response r = (Response)response;
            r.putHeaders(content,contentLength,etags);
            HttpFields f = r.getHttpFields();
            if (acceptRanges)
                f.put(ACCEPT_RANGES);

            if (cacheControl!=null)
                f.put(cacheControl);
        }
        else
        {
            Response.putHeaders(response,content,contentLength,etags);
            if (acceptRanges)
                response.setHeader(ACCEPT_RANGES.getName(),ACCEPT_RANGES.getValue());

            if (cacheControl!=null)
                response.setHeader(cacheControl.getName(),cacheControl.getValue());
        }
    }

	/* ------------------------------------------------------------ */
	protected void writeOptionHeaders(HttpFields fields) {
		if (acceptRanges) {
			fields.put(HttpHeader.ACCEPT_RANGES, "bytes");
		}

		if (cacheControl != null) {
			fields.add(cacheControl);
		}
	}

	/* ------------------------------------------------------------ */
	protected void writeOptionHeaders(HttpServletResponse response) {
		if (acceptRanges) {
			response.setHeader(HttpHeader.ACCEPT_RANGES.asString(), "bytes");
		}

		if (cacheControl != null) {
			response.setHeader(HttpHeader.CACHE_CONTROL.asString(),
					cacheControl.getValue());
		}
	}

	/* ------------------------------------------------------------ */
	/*
	 * @see javax.servlet.Servlet#destroy()
	 */
	@Override
	public void destroy() {
		if (cache != null) {
			cache.flushCache();
		}
		super.destroy();
	}

}
