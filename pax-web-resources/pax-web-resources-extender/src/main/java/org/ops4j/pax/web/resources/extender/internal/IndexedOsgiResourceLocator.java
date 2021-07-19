/* Copyright 2016 Marc Schlegel
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
package org.ops4j.pax.web.resources.extender.internal;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ops4j.pax.web.resources.api.OsgiResourceLocator;
import org.ops4j.pax.web.resources.api.ResourceInfo;
import org.ops4j.pax.web.resources.api.query.ResourceQueryMatcher;
import org.ops4j.pax.web.resources.api.query.ResourceQueryResult;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Default implementation of {@link OsgiResourceLocator}.</p>
 *
 * <p>This implementation will lookup all resources under {@code /META-INF/resources/} and put the URL of that files
 * in a map with the key being the same as the resource would be looked up.</p>
 *
 * <p>Furthermore it provides the capability to overwrite resources: if a key in the map is already used, the old
 * value will be moved to a separated collection, until the overriding bundle is stopped.</p>
 *
 * @author Marc Schlegel
 */
public class IndexedOsgiResourceLocator implements OsgiResourceLocator {

	/** Following Servlet 3.0 Specification for JAR-Resources */
	private static final String RESOURCE_ROOT = "/META-INF/resources/";

	private final BundleContext context;
	private final ResourceBundleIndex index;
	private final transient Logger logger;

	private final List<ResourceBundleIndexEntry> shadowedMap = new CopyOnWriteArrayList<>();

	public IndexedOsgiResourceLocator(BundleContext context) {
		this.logger = LoggerFactory.getLogger(getClass());
		this.context = context;
		index = new ResourceBundleIndex();
	}

	@Override
	public void register(final Bundle bundle) {
		Collection<URL> urls;
		try {
			urls = Collections.list(bundle.findEntries(RESOURCE_ROOT, "*.*", true));
		} catch (IllegalStateException e) {
			logger.error("Error retrieving bundle-resources from bundle '{}'", bundle.getSymbolicName(), e);
			urls = Collections.emptyList();
		}

		urls.forEach(url -> index.addResourceToIndex(
				url.getPath(),
				new ResourceInfo(url, LocalDateTime.ofInstant(
						Instant.ofEpochMilli(
								bundle.getLastModified()),
						ZoneId.systemDefault()),
						bundle.getBundleId()),
				bundle));

		logger.info("Bundle '{}' scanned for resources in '{}': {} entries added to index.",
				bundle.getSymbolicName(), RESOURCE_ROOT, urls.size());
	}

	@Override
	public void unregister(Bundle bundle) {
		index.cleanBundleFromIndex(bundle);
	}

	@Override
	public ResourceInfo locateResource(String resourceName) {
		final String lookupString = RESOURCE_ROOT + cleanSlashesFromPath(resourceName);

		return index.getResourceInfo(lookupString);
	}

	@Override
	public <R extends ResourceQueryResult, Q extends ResourceQueryMatcher> Collection<R> findResources(Q queryMatcher) {
		if (queryMatcher == null) {
			throw new IllegalArgumentException("findResources must be called with non-null queryMatcher!");
		}
		return index.findResources(queryMatcher);
	}

	/**
	 * Removes the leading '/' because it does not have any meaning
	 * (with and without should point to the same resource)
	 *
	 * @param path the resource-path to clean
	 * @return resource-path without leading '/'
	 */
	private String cleanSlashesFromPath(final String path) {
		if (path == null) {
			throw new IllegalArgumentException("createResource must be called with non-null resourceName!");
		}
		String workPath = path;
		if (workPath.charAt(0) == '/') {
			workPath = path.substring(1);
		}
		if (workPath.charAt(path.length() - 1) == '/') {
			workPath = path.substring(0, path.length() - 1);
		}
		return workPath;
	}

	private class ResourceBundleIndex {

		private final Map<String, ResourceBundleIndexEntry> indexMap = new ConcurrentHashMap<>(100);

		private void addResourceToIndex(String lookupPath, ResourceInfo resourceInfo, Bundle bundleWithResource) {
			if (StringUtils.isBlank(lookupPath) || resourceInfo == null || bundleWithResource == null) {
				return;
			}
			if (indexMap.containsKey(lookupPath)) {
				ResourceBundleIndexEntry entry = indexMap.get(lookupPath);
				Bundle currentlyProvidingBundle = context.getBundle(entry.getResourceInfo().getBundleId());
				if (currentlyProvidingBundle != null) {
					logger.warn(
							"Resource with path '{}' is already provided by bundle '{}'! Will be overridden by bundle '{}'",
									lookupPath,
									currentlyProvidingBundle.getSymbolicName(),
									bundleWithResource.getSymbolicName());
					shadowedMap.add(indexMap.get(lookupPath));
				}
			}
			indexMap.put(lookupPath,
					new ResourceBundleIndexEntry(lookupPath, resourceInfo));
		}

		private ResourceInfo getResourceInfo(String lookupPath) {
			ResourceBundleIndexEntry entry = indexMap.get(lookupPath);
			return entry != null ? entry.getResourceInfo() : null;
		}

		private <R extends ResourceQueryResult, Q extends ResourceQueryMatcher> Collection<R> findResources(Q query) {
			List<R> resultList = new ArrayList<>();
			for (Entry<String, ResourceBundleIndexEntry> entry : indexMap.entrySet()) {
				Optional<R> isQueryResult = query.matches(entry.getKey());
				if (isQueryResult.isPresent()) {
					R queryResult = isQueryResult.get();
					queryResult.addMatchedResourceInfo(entry.getValue().getResourceInfo());
					resultList.add(queryResult);
				}
			}
			return Collections.unmodifiableCollection(resultList);
		}

		private void cleanBundleFromIndex(final Bundle bundle) {
			final long removedBundleId = bundle.getBundleId();
			// We first have to collect all necessary entries from the
			// indexMap as well as the shadowedMap into a separated list in
			// order to avoid concurrent-modifications.
			// This is especially important when using streams, because the
			// underlying backing-collection must not be modified while the
			// stream is open (same applies to iterators)
			List<ResourceBundleIndexEntry> entriesToBeRemoved = indexMap.values().stream()
					.filter(indexEntry -> indexEntry.getResourceInfo().getBundleId() == removedBundleId).collect(Collectors.toList());
			List<ResourceBundleIndexEntry> entriesToBeRevoked = shadowedMap.stream()
					.filter(shadowedEntry -> indexMap.containsKey(shadowedEntry.getLookupPath()))
					.collect(Collectors.toList());
			// remove the entries from the bundle which got stopped
			entriesToBeRemoved.forEach(entry -> indexMap.remove(entry.getLookupPath()));
			logger.info("Removed all resources from bundle '{}'", bundle.getSymbolicName());
			// revoke the matching shadowed-entries back to the indexMap
			entriesToBeRevoked.forEach(entry -> {
				indexMap.put(entry.getLookupPath(), entry);
				logger.info("Revoking shadowed resource '{}' from bundle '{}'", entry.getLookupPath(),
						context.getBundle(entry.getResourceInfo().getBundleId()).getSymbolicName());
				shadowedMap.remove(entry);
			});
		}
	}

	private static class ResourceBundleIndexEntry {

		private final String lookupPath;
		private final ResourceInfo resourceInfo;

		private ResourceBundleIndexEntry(final String lookupPath, final ResourceInfo resourceInfo) {
			this.lookupPath = lookupPath;
			this.resourceInfo = resourceInfo;
		}

		private String getLookupPath() {
			return lookupPath;
		}

		private ResourceInfo getResourceInfo() {
			return resourceInfo;
		}
	}

}
