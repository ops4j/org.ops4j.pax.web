/* Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.spi.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import org.ops4j.pax.web.service.WebContainerContext;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds web elements in a global context accross all services (all bundles
 * using the Http Service).
 *
 * @author Alin Dreghiciu
 */
public class ServerModel {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ServerModel.class);

	/**
	 * Map between aliases used for registering a servlet and the registered
	 * servlet model. Used to block registration of an alias more then one time.
	 */
	private final Map<String, ServletModel> aliasMapping;
	/**
	 * Set of all registered servlets. Used to block registration of the same
	 * servlet more times.
	 */
	private final Set<Servlet> servlets;
	/**
	 * Mapping between full registration url patterns and servlet model. Full
	 * url pattern mean that it has the context name prepended (if context name
	 * is set) to the actual url pattern. Used to globally find (against all
	 * registered patterns) the right servlet context for the pattern.
	 */
	private final Map<String, UrlPattern> servletUrlPatterns;
	/**
	 * Mapping between full registration url patterns and filter model. Full url
	 * pattern mean that it has the context name prepended (if context name is
	 * set) to the actual url pattern. Used to globally find (against all
	 * registered patterns) the right filter context for the pattern.
	 */
	private final ConcurrentMap<String, Set<UrlPattern>> filterUrlPatterns;
	/**
	 * Map between http contexts and the bundle that registred a web element
	 * using that http context. Used to block more bundles registering web
	 * elements using the same http context.
	 */
	private final ConcurrentMap<WebContainerContext, Bundle> httpContexts;
	/**
	 * Servlet lock. Used to sychonchornize on servlet
	 * registration/unregistrationhat that works agains 3 maps (m_servlets,
	 * m_aliasMaping, m_servletToUrlPattern).
	 */
	private final ReentrantReadWriteLock servletLock;

	private final ReentrantReadWriteLock filterLock;

	private final ConcurrentMap<ServletContainerInitializer, ContainerInitializerModel> containerInitializers;

	/**
	 * Constructor.
	 */
	public ServerModel() {
		aliasMapping = new HashMap<>();
		servlets = new HashSet<>();
		servletUrlPatterns = new HashMap<>();
		filterUrlPatterns = new ConcurrentHashMap<>();
		httpContexts = new ConcurrentHashMap<>();
		containerInitializers = new ConcurrentHashMap<>();
		servletLock = new ReentrantReadWriteLock(true);
		filterLock = new ReentrantReadWriteLock(true);
	}

	/**
	 * Registers a servlet model.
	 *
	 * @param model servlet model to register
	 * @throws ServletException   - If servlet is already registered
	 * @throws NamespaceException - If servlet alias is already registered
	 */
	public void addServletModel(final ServletModel model) throws NamespaceException, ServletException {
		servletLock.writeLock().lock();
		try {
			if (model.getServlet() != null && servlets.contains(model.getServlet())) {
				throw new ServletException("servlet already registered with a different alias");
			}
			if (model.getAlias() != null) {
				final String alias = getFullPath(model.getContextModel(), model.getAlias());
				if (aliasMapping.containsKey(alias)) {
					throw new NamespaceException("alias: '" + alias + "' is already in use in this or another context");
				}
				aliasMapping.put(alias, model);
			}
			if (model.getServlet() != null) {
				servlets.add(model.getServlet());
			}
			for (String urlPattern : model.getUrlPatterns()) {
				servletUrlPatterns.put(getFullPath(model.getContextModel(), urlPattern),
						new UrlPattern(getFullPath(model.getContextModel(), urlPattern), model));
			}
		} finally {
			servletLock.writeLock().unlock();
		}
	}

	/**
	 * Unregisters a servlet model.
	 *
	 * @param model servlet model to unregister
	 */
	public void removeServletModel(final ServletModel model) {
		servletLock.writeLock().lock();
		try {
			if (model.getAlias() != null) {
				aliasMapping.remove(getFullPath(model.getContextModel(), model.getAlias()));
			}
			if (model.getServlet() != null) {
				servlets.remove(model.getServlet());
			}
			if (model.getUrlPatterns() != null) {
				for (String urlPattern : model.getUrlPatterns()) {
					servletUrlPatterns.remove(getFullPath(model.getContextModel(), urlPattern));
				}
			}
		} finally {
			servletLock.writeLock().unlock();
		}
	}

	/**
	 * Registers a filter model.
	 *
	 * @param model filter model to register
	 */
	public void addFilterModel(final FilterModel model) {
		if (model.getUrlPatterns() != null) {
			try {
				filterLock.writeLock().lock();
				for (String urlPattern : model.getUrlPatterns()) {
					final UrlPattern newUrlPattern = new UrlPattern(getFullPath(model.getContextModel(), urlPattern),
							model);
					String fullPath = getFullPath(model.getContextModel(), urlPattern);
					Set<UrlPattern> urlSet = filterUrlPatterns.get(fullPath);
					if (urlSet == null) {
						//initialize first
						urlSet = new HashSet<>();
					}
					urlSet.add(newUrlPattern);
					filterUrlPatterns.put(fullPath, urlSet);
//					final UrlPattern existingPattern = filterUrlPatterns.putIfAbsent(
//							getFullPath(model.getContextModel(), urlPattern), newUrlPattern);
//					if (existingPattern != null) {
//						// this should never happen but is a good assertion
//						LOG.error("Internal error (please report): Cannot associate url mapping "
//								+ getFullPath(model.getContextModel(), urlPattern) + " to " + newUrlPattern
//								+ " because is already associated to " + existingPattern);
//					}
				}
			} finally {
				filterLock.writeLock().unlock();
			}
		}
	}

	/**
	 * Unregister a filter model.
	 *
	 * @param model filter model to unregister
	 */
	public void removeFilterModel(final FilterModel model) {
		if (model.getUrlPatterns() != null) {
			try {
				filterLock.writeLock().lock();
				for (String urlPattern : model.getUrlPatterns()) {
					String fullPath = getFullPath(model.getContextModel(), urlPattern);
					Set<UrlPattern> urlSet = filterUrlPatterns.get(fullPath);
					UrlPattern toDelete = null;
					for (UrlPattern pattern : urlSet) {
						FilterModel filterModel = (FilterModel) pattern.getModel();
						Class<?> filter = filterModel.getFilterClass();
						Class<?> matchFilter = model.getFilterClass();
						if (filter != null && filter.equals(matchFilter)) {
							toDelete = pattern;
							break;
						}
						Object filterInstance = filterModel.getFilter();
						if (filterInstance != null && filterInstance == model.getFilter()) {
							toDelete = pattern;
							break;
						}
					}
					urlSet.remove(toDelete);
				}
			} finally {
				filterLock.writeLock().unlock();
			}
		}
	}

	public void addContainerInitializerModel(ContainerInitializerModel model) {
		if (containerInitializers.containsKey(model.getContainerInitializer())) {
			throw new IllegalArgumentException(
					"ContainerInitializer Model already contains a container initializer of this: "
							+ model.getContainerInitializer());
		}
		containerInitializers.put(model.getContainerInitializer(), model);
	}

	public void removeContainerInitializerModel(ContainerInitializerModel model) {
		containerInitializers.remove(model.getContainerInitializer());
	}

	/**
	 * Associates a http context with a bundle if the http service is not
	 * already associated to another bundle. This is done in order to prevent
	 * sharing http context between bundles. The implementation is not 100%
	 * correct as it can be that at a certain moment in time when this method is
	 * called,another thread is processing a release of the http service,
	 * process that will deassociate the bundle that released the http service,
	 * and that bundle could actually be related to the http context that this
	 * method is trying to associate. But this is less likely to happen as it
	 * should have as precondition that this is happening concurrent and that
	 * the two bundles are sharing the http context. But this solution has the
	 * benefits of not needing synchronization.
	 *
	 * @param httpContext         http context to be assicated to the bundle
	 * @param bundle              bundle to be assiciated with the htp service
	 * @param allowReAsssociation if it should allow a context to be reassiciated to a bundle
	 * @throws IllegalStateException - If htp context is already associated to another bundle.
	 */
	public void associateHttpContext(final WebContainerContext httpContext, final Bundle bundle,
									 final boolean allowReAsssociation) {
		final Bundle currentBundle = httpContexts.putIfAbsent(httpContext, bundle);
		if ((!allowReAsssociation) && currentBundle != null && currentBundle != bundle) {
			throw new IllegalStateException("Http context " + httpContext + " is already associated to bundle "
					+ currentBundle);
		}
	}

	public HttpContext findDefaultHttpContextForBundle(Bundle bundle) {
		HttpContext httpContext = null;
		for (Entry<WebContainerContext, Bundle> entry : httpContexts.entrySet()) {
			if (entry.getValue() == bundle) {
				httpContext = entry.getKey();
				break;
			}
		}
		return httpContext;
	}

	/**
	 * Deassociate all http context assiciated to the provided bundle. The
	 * bellow code is only correct in the context that there is no other thread
	 * is calling the association method in the mean time. This should not
	 * happen as once a bundle is releasing the HttpService the service is first
	 * entering a stopped state ( before the call to this method is made), state
	 * that will not perform the registration calls anymore.
	 *
	 * @param bundle bundle to be deassociated from http contexts
	 */
	public void deassociateHttpContexts(final Bundle bundle) {
		httpContexts.entrySet().stream()
				.filter(entry -> entry.getValue() == bundle)
				.forEach(entry -> httpContexts.remove(entry.getKey()));
	}

	public ContextModel matchPathToContext(final String path) {
		final boolean debug = LOG.isDebugEnabled();
		if (debug) {
			LOG.debug("Matching [" + path + "]...");
		}
		UrlPattern urlPattern = null;
		// first match servlets
		servletLock.readLock().lock();
		try {
			urlPattern = matchPathToContext(servletUrlPatterns, path);
		} finally {
			servletLock.readLock().unlock();
		}
		// then if there is no matched servlet look for filters
		if (urlPattern == null) {
			urlPattern = matchFilterPathToContext(filterUrlPatterns, path);
		}
		ContextModel matched = null;
		if (urlPattern != null) {
			matched = urlPattern.getModel().getContextModel();
		}
		if (debug) {
			if (matched != null) {
				LOG.debug("Path [" + path + "] matched to " + urlPattern);
			} else {
				LOG.debug("Path [" + path + "] does not match any context");
			}
		}
		return matched;
	}

	private static UrlPattern matchFilterPathToContext(final Map<String, Set<UrlPattern>> urlPatternsMap, final String path) {
		Set<String> keySet = urlPatternsMap.keySet();
		for (String key : keySet) {
			Set<UrlPattern> patternsMap = urlPatternsMap.get(key);

			for (UrlPattern urlPattern : patternsMap) {
				Map<String, UrlPattern> tempMap = new HashMap<>();
				tempMap.put(key, urlPattern);
				UrlPattern pattern = matchPathToContext(tempMap, path);
				if (pattern != null) {
					return pattern;
				}
			}
		}
		return null;
	}

	private static UrlPattern matchPathToContext(final Map<String, UrlPattern> urlPatternsMap, final String path) {
		UrlPattern matched = null;
		String servletPath = path;

		while ((matched == null) && (!"".equals(servletPath))) {
			// Match the asterisks first that comes just after the current
			// servlet path, so that it satisfies the longest path req
			if (servletPath.endsWith("/")) {
				matched = urlPatternsMap.get(servletPath + "*");
			} else {
				matched = urlPatternsMap.get(servletPath + "/*");
			}

			// try to match the exact resource if the above fails
			if (matched == null) {
				matched = urlPatternsMap.get(servletPath);
			}

			// now try to match the url backwards one directory at a time
			if (matched == null) {
				String lastPathSegment = servletPath.substring(servletPath.lastIndexOf("/") + 1);
				servletPath = servletPath.substring(0, servletPath.lastIndexOf("/"));
				// case 1: the servlet path is /
				if (("".equals(servletPath)) && ("".equals(lastPathSegment))) {
					break;
				} else if ("".equals(lastPathSegment)) {
					// case 2 the servlet path ends with /
					matched = urlPatternsMap.get(servletPath + "/*");
					continue;
				} else if (lastPathSegment.contains(".")) {
					// case 3 the last path segment has a extension that needs
					// to be
					// matched
					String extension = lastPathSegment.substring(lastPathSegment.lastIndexOf("."));
					if (extension.length() > 1) {
						// case 3.5 refer to second bulleted point of heading
						// Specification of Mappings
						// in servlet specification
						// PATCH - do not go global too early. Next 3 lines
						// modified.
						// matched = urlPatternsMap.get("*" + extension);
						if (matched == null) {
							matched = urlPatternsMap.get(("".equals(servletPath) ? "*" : servletPath + "/*")
									+ extension);
						}
					}
				} else {
					// case 4 search for the wild cards at the end of servlet
					// path
					// of the next iteration
					if (servletPath.endsWith("/")) {
						matched = urlPatternsMap.get(servletPath + "*");
					} else {
						matched = urlPatternsMap.get(servletPath + "/*");
					}
				}

				// case 5 if all the above fails look for the actual mapping
				if (matched == null) {
					matched = urlPatternsMap.get(servletPath);
				}

				// case 6 the servlet path has / followed by context name, this
				// case is
				// selected at the end of the directory, when none of the them
				// matches.
				// So we try to match to root.
				if ((matched == null) && ("".equals(servletPath)) && (!"".equals(lastPathSegment))) {
					matched = urlPatternsMap.get("/");
				}
			}
		}
		return matched;
	}

	/**
	 * Returns the full path (including the context name if set)
	 *
	 * @param model a context model
	 * @param path  path to be prepended
	 * @return full path
	 */
	private static String getFullPath(final ContextModel model, final String path) {
		String fullPath = path.trim();
		if (model.getContextName().length() > 0) {
			fullPath = "/" + model.getContextName();
			if (!"/".equals(path.trim())) {
				if ((!(fullPath.endsWith("/"))) && (!(path.startsWith("/")))) {
					fullPath += "/";
				}
				fullPath = fullPath + path;
			}
		}
		return fullPath;
	}

	/**
	 * Touple of full url pattern and registered model (servlet/filter) for the
	 * model.
	 */
	private static class UrlPattern {

		private final Pattern pattern;
		private final Model model;

		UrlPattern(final String pattern, final Model model) {
			this.model = model;
			String patternToUse = pattern;
			if (!patternToUse.contains("*")) {
				patternToUse = patternToUse + (pattern.endsWith("/") ? "*" : "/*");
			}
			patternToUse = patternToUse.replace(".", "\\.");
			patternToUse = patternToUse.replace("*", ".*");
			this.pattern = Pattern.compile(patternToUse);
		}

		Model getModel() {
			return model;
		}

		@Override
		public String toString() {
			return new StringBuilder().append("{").append("pattern=").append(pattern.pattern()).append(",model=")
					.append(model).append("}").toString();
		}
	}

}
