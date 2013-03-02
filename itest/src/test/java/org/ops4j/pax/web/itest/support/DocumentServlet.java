package org.ops4j.pax.web.itest.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeaderValues;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.io.WriterOutputStream;
import org.eclipse.jetty.server.AbstractHttpConnection;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Dispatcher;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.InclusiveByteRange;
import org.eclipse.jetty.server.ResourceCache;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.nio.NIOConnector;
import org.eclipse.jetty.server.ssl.SslConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.MultiPartOutputStream;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.resource.ResourceFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;

@Component(immediate = true, provide = Servlet.class, properties = "alias=/document")
public class DocumentServlet extends HttpServlet implements 
		ResourceFactory {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private String resourcePath;
	
	@Activate
	public void activate() {
		logger.info("Document servlet started");
		resourcePath = "./target";
	}

	private static final long serialVersionUID = 4930458713846881193L;
	private ServletContext _servletContext;
	private ContextHandler _contextHandler;

	private boolean _acceptRanges = true;
	private boolean _dirAllowed = true;
	private boolean _welcomeServlets = false;
	private boolean _welcomeExactServlets = false;
	private boolean _redirectWelcome = false;
	private boolean _gzip = true;
	private boolean _pathInfoOnly = false;

	private Resource _resourceBase;
	private ResourceCache _cache;

	private MimeTypes _mimeTypes;
	private String[] _welcomes;
	private Resource _stylesheet;
	private boolean _useFileMappedBuffer = false;
	private ByteArrayBuffer _cacheControl;
	private String _relativeResourceBase;
    private ServletHandler _servletHandler;
    private ServletHolder _defaultHolder;

	/* ------------------------------------------------------------ */
	@Override
	public void init() throws UnavailableException {
		_servletContext = getServletContext();
		_contextHandler = initContextHandler(_servletContext);

		_mimeTypes = _contextHandler.getMimeTypes();

		_welcomes = _contextHandler.getWelcomeFiles();
		if (_welcomes == null)
			_welcomes = new String[] { "index.html", "index.jsp" };

		_acceptRanges = getInitBoolean("acceptRanges", _acceptRanges);
		_dirAllowed = getInitBoolean("dirAllowed", _dirAllowed);
		_redirectWelcome = getInitBoolean("redirectWelcome", _redirectWelcome);
		_gzip = getInitBoolean("gzip", _gzip);
		_pathInfoOnly = getInitBoolean("pathInfoOnly", _pathInfoOnly);

		if ("exact".equals(getInitParameter("welcomeServlets"))) {
			_welcomeExactServlets = true;
			_welcomeServlets = false;
		} else
			_welcomeServlets = getInitBoolean("welcomeServlets",
					_welcomeServlets);

		if (getInitParameter("aliases") != null)
			_contextHandler.setAliases(getInitBoolean("aliases", false));

		boolean aliases = _contextHandler.isAliases();
		if (!aliases && !FileResource.getCheckAliases())
			throw new IllegalStateException("Alias checking disabled");
		if (aliases)
			_servletContext.log("Aliases are enabled");

		_useFileMappedBuffer = getInitBoolean("useFileMappedBuffer",
				_useFileMappedBuffer);

		_relativeResourceBase = getInitParameter("relativeResourceBase");

		String rb = resourcePath;
		if (rb != null) {
			if (_relativeResourceBase != null)
				throw new UnavailableException(
						"resourceBase & relativeResourceBase");
			try {
				_resourceBase = _contextHandler.newResource(rb);
			} catch (Exception e) {
				logger.warn(Log.EXCEPTION, e);
				throw new UnavailableException(e.toString());
			}
		}

		String css = getInitParameter("stylesheet");
		try {
			if (css != null) {
				_stylesheet = Resource.newResource(css);
				if (!_stylesheet.exists()) {
					logger.warn("!" + css);
					_stylesheet = null;
				}
			}
			if (_stylesheet == null) {
				_stylesheet = Resource.newResource(this.getClass().getResource(
						"/jetty-dir.css"));
			}
		} catch (Exception e) {
			logger.warn(e.toString(), e);
		}

		String t = getInitParameter("cacheControl");
		if (t != null)
			_cacheControl = new ByteArrayBuffer(t);

		String resourceCache = getInitParameter("resourceCache");
		int max_cache_size = getInitInt("maxCacheSize", -2);
		int max_cached_file_size = getInitInt("maxCachedFileSize", -2);
		int max_cached_files = getInitInt("maxCachedFiles", -2);
		if (resourceCache != null) {
			if (max_cache_size != -1 || max_cached_file_size != -2
					|| max_cached_files != -2)
				logger.debug("ignoring resource cache configuration, using resourceCache attribute");
			if (_relativeResourceBase != null || _resourceBase != null)
				throw new UnavailableException(
						"resourceCache specified with resource bases");
			_cache = (ResourceCache) _servletContext
					.getAttribute(resourceCache);

			logger.debug("Cache {}={}", resourceCache, _cache);
		}

		try {
			if (_cache == null && max_cached_files > 0) {
				_cache = new ResourceCache(null, this, _mimeTypes,
						_useFileMappedBuffer, true);

				if (max_cache_size > 0)
					_cache.setMaxCacheSize(max_cache_size);
				if (max_cached_file_size >= -1)
					_cache.setMaxCachedFileSize(max_cached_file_size);
				if (max_cached_files >= -1)
					_cache.setMaxCachedFiles(max_cached_files);
			}
		} catch (Exception e) {
			logger.warn(Log.EXCEPTION, e);
			throw new UnavailableException(e.toString());
		}

		_servletHandler = _contextHandler
				.getChildHandlerByClass(ServletHandler.class);
		for (ServletHolder h : _servletHandler.getServlets())
			if (h.getServletInstance() == this)
				_defaultHolder = h;

		if (logger.isDebugEnabled())
			logger.debug("resource base = " + _resourceBase);
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
			if (servletContext instanceof ContextHandler.Context)
				return ((ContextHandler.Context) servletContext)
						.getContextHandler();
			else {
				throw new IllegalArgumentException("The servletContext "
						+ servletContext + " "
						+ servletContext.getClass().getName() + " is not "
						+ ContextHandler.Context.class.getName());
			}
		} else
			return ContextHandler.getCurrentContext().getContextHandler();
	}

	/* ------------------------------------------------------------ */
	@Override
	public String getInitParameter(String name) {
		String value = getServletContext().getInitParameter(
				"org.eclipse.jetty.servlet.Default." + name);
		if (value == null)
			value = super.getInitParameter(name);
		return value;
	}

	/* ------------------------------------------------------------ */
	private boolean getInitBoolean(String name, boolean dft) {
		String value = getInitParameter(name);
		if (value == null || value.length() == 0)
			return dft;
		return (value.startsWith("t") || value.startsWith("T")
				|| value.startsWith("y") || value.startsWith("Y") || value
					.startsWith("1"));
	}

	/* ------------------------------------------------------------ */
	private int getInitInt(String name, int dft) {
		String value = getInitParameter(name);
		if (value == null)
			value = getInitParameter(name);
		if (value != null && value.length() > 0)
			return Integer.parseInt(value);
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
		if (_relativeResourceBase != null)
			pathInContext = URIUtil.addPaths(_relativeResourceBase,
					pathInContext);

		try {
			if (_resourceBase != null) {
				r = _resourceBase.addPath(pathInContext);
			} else {
				URL u = _servletContext.getResource(pathInContext);
				r = _contextHandler.newResource(u);
			}

			if (logger.isDebugEnabled())
				logger.debug("Resource " + pathInContext + "=" + r);
		} catch (IOException e) {
			// do nothing
		}

		if ((r == null || !r.exists())
				&& pathInContext.endsWith("/jetty-dir.css"))
			r = _stylesheet;

		return r;
	}

	/* ------------------------------------------------------------ */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String servletPath = null;
		String pathInfo = null;
		Enumeration<String> reqRanges = null;
		boolean included = request.getAttribute(Dispatcher.INCLUDE_REQUEST_URI) != null;
		if (included) {
			servletPath = (String) request
					.getAttribute(Dispatcher.INCLUDE_SERVLET_PATH);
			pathInfo = (String) request
					.getAttribute(Dispatcher.INCLUDE_PATH_INFO);
			if (servletPath == null) {
				servletPath = request.getServletPath();
				pathInfo = request.getPathInfo();
			}
		} else {
			servletPath = _pathInfoOnly ? "/" : request.getServletPath();
			pathInfo = request.getPathInfo();

			// Is this a Range request?
			reqRanges = request.getHeaders(HttpHeaders.RANGE);
			if (!hasDefinedRange(reqRanges))
				reqRanges = null;
		}

		String pathInContext = URIUtil.addPaths(servletPath, pathInfo);
		boolean endsWithSlash = (pathInfo == null ? request.getServletPath()
				: pathInfo).endsWith(URIUtil.SLASH);

		// Can we gzip this request?
		String pathInContextGz = null;
		boolean gzip = false;
		if (!included && _gzip && reqRanges == null && !endsWithSlash) {
			String accept = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
			if (accept != null && accept.indexOf("gzip") >= 0)
				gzip = true;
		}

		// Find the resource and content
		Resource resource = null;
		HttpContent content = null;

		try {
			// Try gzipped content first
			if (gzip) {
				pathInContextGz = pathInContext + ".gz";

				if (_cache == null) {
					resource = getResource(pathInContextGz);
				} else {
					content = _cache.lookup(pathInContextGz);
					resource = (content == null) ? null : content.getResource();
				}

				if (resource == null || !resource.exists()
						|| resource.isDirectory()) {
					gzip = false;
					pathInContextGz = null;
				}
			}

			// find resource
			if (!gzip) {
				if (_cache == null)
					resource = getResource(pathInContext);
				else {
					content = _cache.lookup(pathInContext);
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
				if (endsWithSlash && _contextHandler.isAliases()
						&& pathInContext.length() > 1) {
					String q = request.getQueryString();
					pathInContext = pathInContext.substring(0,
							pathInContext.length() - 1);
					if (q != null && q.length() != 0)
						pathInContext += "?" + q;
					response.sendRedirect(response.encodeRedirectURL(URIUtil
							.addPaths(_servletContext.getContextPath(),
									pathInContext)));
				} else {
					// ensure we have content
					if (content == null)
						content = new HttpContent.ResourceAsHttpContent(
								resource,
								_mimeTypes.getMimeByExtension(resource
										.toString()), response.getBufferSize());

					if (included
							|| passConditionalHeaders(request, response,
									resource, content)) {
						if (gzip) {
							response.setHeader(HttpHeaders.CONTENT_ENCODING,
									"gzip");
							String mt = _servletContext
									.getMimeType(pathInContext);
							if (mt != null)
								response.setContentType(mt);
						}
						sendData(request, response, included, resource,
								content, reqRanges);
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
					if (_redirectWelcome) {
						// Redirect to the index
						response.setContentLength(0);
						String q = request.getQueryString();
						if (q != null && q.length() != 0)
							response.sendRedirect(response
									.encodeRedirectURL(URIUtil.addPaths(
											_servletContext.getContextPath(),
											welcome)
											+ "?" + q));
						else
							response.sendRedirect(response
									.encodeRedirectURL(URIUtil.addPaths(
											_servletContext.getContextPath(),
											welcome)));
					} else {
						// Forward to the index
						RequestDispatcher dispatcher = request
								.getRequestDispatcher(welcome);
						if (dispatcher != null) {
							if (included)
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
					content = new HttpContent.ResourceAsHttpContent(resource,
							_mimeTypes.getMimeByExtension(resource.toString()));
					if (included
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
	 * will be the first entry in the list of configured {@link #_welcomes
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
	private String getWelcomeFile(String pathInContext)
			throws MalformedURLException, IOException {
		if (_welcomes == null)
			return null;

		String welcome_servlet = null;
		for (int i = 0; i < _welcomes.length; i++) {
			String welcome_in_context = URIUtil.addPaths(pathInContext,
					_welcomes[i]);
			Resource welcome = getResource(welcome_in_context);
			if (welcome != null && welcome.exists())
				return _welcomes[i];

			if ((_welcomeServlets || _welcomeExactServlets)
					&& welcome_servlet == null) {
				Map.Entry<?, ?> entry = _servletHandler
						.getHolderEntry(welcome_in_context);
				if (entry != null
						&& entry.getValue() != _defaultHolder
						&& (_welcomeServlets || (_welcomeExactServlets && entry
								.getKey().equals(welcome_in_context))))
					welcome_servlet = welcome_in_context;

			}
		}
		return welcome_servlet;
	}

	/* ------------------------------------------------------------ */
	/*
	 * Check modification date headers.
	 */
	protected boolean passConditionalHeaders(HttpServletRequest request,
			HttpServletResponse response, Resource resource, HttpContent content)
			throws IOException {
		try {
			if (!request.getMethod().equals(HttpMethods.HEAD)) {
				String ifms = request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
				if (ifms != null) {
					if (content != null) {
						Buffer mdlm = content.getLastModified();
						if (mdlm != null) {
							if (ifms.equals(mdlm.toString())) {
								response.reset();
								response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
								response.flushBuffer();
								return false;
							}
						}
					}

					long ifmsl = request
							.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
					if (ifmsl != -1) {
						if (resource.lastModified() / 1000 <= ifmsl / 1000) {
							response.reset();
							response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
							response.flushBuffer();
							return false;
						}
					}
				}

				// Parse the if[un]modified dates and compare to resource
				long date = request
						.getDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE);

				if (date != -1) {
					if (resource.lastModified() / 1000 > date / 1000) {
						response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
						return false;
					}
				}

			}
		} catch (IllegalArgumentException iae) {
			if (!response.isCommitted())
				response.sendError(400, iae.getMessage());
			throw iae;
		}
		return true;
	}

	/* ------------------------------------------------------------------- */
	protected void sendDirectory(HttpServletRequest request,
			HttpServletResponse response, Resource resource,
			String pathInContext) throws IOException {
		if (!_dirAllowed) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		byte[] data = null;
		String base = URIUtil.addPaths(request.getRequestURI(), URIUtil.SLASH);

		// handle ResourceCollection
		if (_resourceBase instanceof ResourceCollection)
			resource = _resourceBase.addPath(pathInContext);
		else if (_contextHandler.getBaseResource() instanceof ResourceCollection)
			resource = _contextHandler.getBaseResource().addPath(pathInContext);

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
		boolean direct;
		long content_length;
		if (content == null) {
			direct = false;
			content_length = resource.length();
		} else {
			Connector connector = AbstractHttpConnection.getCurrentConnection().getConnector();
            direct=connector instanceof NIOConnector && ((NIOConnector)connector).getUseDirectBuffers() && !(connector instanceof SslConnector);
            content_length=content.getContentLength();
		}

		// Get the output stream (or writer)
		OutputStream out = null;
		boolean written;
		try {
			out = response.getOutputStream();

			// has a filter already written to the response?
            written = out instanceof HttpOutput 
                ? ((HttpOutput)out).isWritten() 
                : AbstractHttpConnection.getCurrentConnection().getGenerator().isWritten();
		} catch (IllegalStateException e) {
			out = new WriterOutputStream(response.getWriter());
			written = true; // there may be data in writer buffer, so assume
							// written
		}

		if (reqRanges == null || !reqRanges.hasMoreElements()
				|| content_length < 0) {
			// if there were no ranges, send entire entity
			if (include) {
				resource.writeTo(out, 0, content_length);
			} else {
				// See if a direct methods can be used?
				// See if a direct methods can be used?
                if (content!=null && !written && out instanceof HttpOutput)
                {
                    if (response instanceof Response)
                    {
                        writeOptionHeaders(((Response)response).getHttpFields());
                        ((AbstractHttpConnection.Output)out).sendContent(content);
                    }
                    else 
                    {
                        Buffer buffer = direct?content.getDirectBuffer():content.getIndirectBuffer();
                        if (buffer!=null)
                        {
                            writeHeaders(response,content,content_length);
                            ((AbstractHttpConnection.Output)out).sendContent(buffer);
                        }
                        else
                        {
                            writeHeaders(response,content,content_length);
                            resource.writeTo(out,0,content_length);
                        }
                    }
                } else {
					// Write headers normally
					writeHeaders(response, content, written ? -1
							: content_length);

					// Write content normally
					Buffer buffer = (content == null) ? null : content
							.getIndirectBuffer();
					if (buffer != null)
						buffer.writeTo(out);
					else
						resource.writeTo(out, 0, content_length);
				}
			}
		} else {
			// Parse the satisfiable ranges
			List<?> ranges = InclusiveByteRange.satisfiableRanges(reqRanges,
					content_length);

			// if there are no satisfiable ranges, send 416 response
			if (ranges == null || ranges.size() == 0) {
				writeHeaders(response, content, content_length);
				response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				response.setHeader(HttpHeaders.CONTENT_RANGE,
						InclusiveByteRange
								.to416HeaderRangeString(content_length));
				resource.writeTo(out, 0, content_length);
				return;
			}

			// if there is only a single valid range (must be satisfiable
			// since were here now), send that range with a 216 response
			if (ranges.size() == 1) {
				InclusiveByteRange singleSatisfiableRange = (InclusiveByteRange) ranges
						.get(0);
				long singleLength = singleSatisfiableRange
						.getSize(content_length);
				writeHeaders(response, content, singleLength);
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
				response.setHeader(HttpHeaders.CONTENT_RANGE,
						singleSatisfiableRange
								.toHeaderRangeString(content_length));
				resource.writeTo(out,
						singleSatisfiableRange.getFirst(content_length),
						singleLength);
				return;
			}

			// multiple non-overlapping valid ranges cause a multipart
			// 216 response which does not require an overall
			// content-length header
			//
			writeHeaders(response, content, -1);
			String mimetype = content.getContentType().toString();
			MultiPartOutputStream multi = new MultiPartOutputStream(out);
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

			// If the request has a "Request-Range" header then we need to
			// send an old style multipart/x-byteranges Content-Type. This
			// keeps Netscape and acrobat happy. This is what Apache does.
			String ctp;
			if (request.getHeader(HttpHeaders.REQUEST_RANGE) != null)
				ctp = "multipart/x-byteranges; boundary=";
			else
				ctp = "multipart/byteranges; boundary=";
			response.setContentType(ctp + multi.getBoundary());

			InputStream in = resource.getInputStream();
			long pos = 0;

			// calculate the content-length
			int length = 0;
			String[] header = new String[ranges.size()];
			for (int i = 0; i < ranges.size(); i++) {
				InclusiveByteRange ibr = (InclusiveByteRange) ranges.get(i);
				header[i] = ibr.toHeaderRangeString(content_length);
				length += ((i > 0) ? 2 : 0)
						+ 2
						+ multi.getBoundary().length()
						+ 2
						+ HttpHeaders.CONTENT_TYPE.length()
						+ 2
						+ mimetype.length()
						+ 2
						+ HttpHeaders.CONTENT_RANGE.length()
						+ 2
						+ header[i].length()
						+ 2
						+ 2
						+ (ibr.getLast(content_length) - ibr
								.getFirst(content_length)) + 1;
			}
			length += 2 + 2 + multi.getBoundary().length() + 2 + 2;
			response.setContentLength(length);

			for (int i = 0; i < ranges.size(); i++) {
				InclusiveByteRange ibr = (InclusiveByteRange) ranges.get(i);
				multi.startPart(mimetype,
						new String[] { HttpHeaders.CONTENT_RANGE + ": "
								+ header[i] });

				long start = ibr.getFirst(content_length);
				long size = ibr.getSize(content_length);
				if (in != null) {
					// Handle non cached resource
					if (start < pos) {
						in.close();
						in = resource.getInputStream();
						pos = 0;
					}
					if (pos < start) {
						in.skip(start - pos);
						pos = start;
					}
					IO.copy(in, multi, size);
					pos += size;
				} else
					// Handle cached resource
					(resource).writeTo(multi, start, size);

			}
			if (in != null)
				in.close();
			multi.close();
		}
		return;
	}

	/* ------------------------------------------------------------ */
	protected void writeHeaders(HttpServletResponse response,
			HttpContent content, long count) throws IOException {
		if (content.getContentType() != null
				&& response.getContentType() == null)
			response.setContentType(content.getContentType().toString());

		if (response instanceof Response) {
			Response r = (Response) response;
			HttpFields fields = r.getHttpFields();

			if (content.getLastModified() != null)
				fields.put(HttpHeaders.LAST_MODIFIED_BUFFER,
						content.getLastModified());
			else if (content.getResource() != null) {
				long lml = content.getResource().lastModified();
				if (lml != -1)
					fields.putDateField(HttpHeaders.LAST_MODIFIED_BUFFER, lml);
			}

			if (count != -1)
				r.setLongContentLength(count);

			writeOptionHeaders(fields);
		} else {
			long lml = content.getResource().lastModified();
			if (lml >= 0)
				response.setDateHeader(HttpHeaders.LAST_MODIFIED, lml);

			if (count != -1) {
				if (count < Integer.MAX_VALUE)
					response.setContentLength((int) count);
				else
					response.setHeader(HttpHeaders.CONTENT_LENGTH,
							Long.toString(count));
			}

			writeOptionHeaders(response);
		}
	}

	/* ------------------------------------------------------------ */
	protected void writeOptionHeaders(HttpFields fields) throws IOException {
		if (_acceptRanges)
			fields.put(HttpHeaders.ACCEPT_RANGES_BUFFER,
					HttpHeaderValues.BYTES_BUFFER);

		if (_cacheControl != null)
			fields.put(HttpHeaders.CACHE_CONTROL_BUFFER, _cacheControl);
	}

	/* ------------------------------------------------------------ */
	protected void writeOptionHeaders(HttpServletResponse response)
			throws IOException {
		if (_acceptRanges)
			response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");

		if (_cacheControl != null)
			response.setHeader(HttpHeaders.CACHE_CONTROL,
					_cacheControl.toString());
	}

	/* ------------------------------------------------------------ */
	/*
	 * @see javax.servlet.Servlet#destroy()
	 */
	@Override
	public void destroy() {
		if (_cache != null)
			_cache.flushCache();
		super.destroy();
	}

}
