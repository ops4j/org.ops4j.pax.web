package org.ops4j.pax.web.jsf.resourcehandler.extender.internal;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.faces.application.Resource;
import javax.faces.application.ViewResource;

import org.apache.commons.lang3.StringUtils;
import org.ops4j.pax.web.jsf.resourcehandler.extender.OsgiResource;
import org.ops4j.pax.web.jsf.resourcehandler.extender.OsgiResourceLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Default implementation of {@link OsgiResourceLocator}.
 * </p>
 * <p>
 * This implementation will lookup all resources under
 * {@code /META-INF/resources/} and put the URL of that files in a map with the
 * key being the same as the resource would be looked up.
 * </p>
 * <p>
 * Furthermore it provides the capability to overwrite resources: if a key in
 * the map is already used, the old value will be moved to a separated
 * collection, until the overriding bundle is stopped.
 * </p>
 * 
 * @author Marc Schlegel
 */
public class IndexedOsgiResourceLocator implements OsgiResourceLocator {
	/** Following Servlet 3.0 Specification for JAR-Resources */
	private static final String JSF_DEFAULT_RESOURCE_FOLDER = "/META-INF/resources/";

	private BundleContext context;
	private ResourceBundleIndex index;
	private transient Logger logger;
	
	private transient ReadWriteLock readWriteLock;

	private List<ResourceBundleIndexEntry> shadowedMap = new ArrayList<>();

	public IndexedOsgiResourceLocator(BundleContext context) {
		this.logger = LoggerFactory.getLogger(getClass());
		readWriteLock = new ReentrantReadWriteLock();
		this.context = context;
		index = new ResourceBundleIndex();
	}

	@Override
	public void register(final Bundle bundle) {
		Collection<URL> urls;
		try {
			urls = Collections.list(bundle.findEntries(JSF_DEFAULT_RESOURCE_FOLDER, "*.*", true));
		} catch (Exception e) {
			logger.error("Error retrieving bundle-resources from bundle '{}'", bundle.getSymbolicName(), e);
			urls = Collections.emptyList();
		}

		readWriteLock.writeLock().lock();
		try{
			urls.forEach(url -> index.addResourceToIndex(
					url.getPath(), 
					new ResourceInfo(url, LocalDateTime.ofInstant(
							Instant.ofEpochMilli(
									bundle.getLastModified()), 
							ZoneId.systemDefault()),
							bundle.getBundleId()), 
					bundle));
		}finally{
			readWriteLock.writeLock().unlock();
		}

		logger.info("Bundle '{}' scanned for resources in '{}': {} entries added to index.",
				new Object[] {bundle.getSymbolicName(),
				JSF_DEFAULT_RESOURCE_FOLDER, urls.size()});
	}

	@Override
	public void unregister(Bundle bundle) {
		readWriteLock.writeLock().lock();
		try{
			index.cleanBundleFromIndex(bundle);
		}finally{
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public Resource createResource(String resourceName) {
		return createResource(resourceName, null);
	}

	@Override
	public Resource createResource(String resourceName, String libraryName) {
		if (resourceName == null) {
			throw new IllegalArgumentException("createResource must be called with non-null resourceName!");
		}

		String lookupString;
		if (libraryName != null)
			lookupString = JSF_DEFAULT_RESOURCE_FOLDER + cleanLeadingSlashFromPath(libraryName) + '/' + cleanLeadingSlashFromPath(resourceName);
		else
			lookupString = JSF_DEFAULT_RESOURCE_FOLDER + cleanLeadingSlashFromPath(resourceName);

		readWriteLock.readLock().lock();
		try{
			ResourceInfo resourceInfo = index.getResourceInfo(lookupString);
	
			if (resourceInfo != null) {
				return new OsgiResource(
						resourceInfo.getUrl(), 
						resourceName, 
						libraryName, 
						resourceInfo.getLastModified());
			}
		}finally {
			readWriteLock.readLock().unlock();
		}
		return null;
	}

	@Override
	public ViewResource createViewResource(String resourceName) {
		if (resourceName == null) {
			throw new IllegalArgumentException("createViewResource must be called with non-null resourceName!");
		}

		
		final String lookupString = JSF_DEFAULT_RESOURCE_FOLDER + cleanLeadingSlashFromPath(resourceName);
		
		readWriteLock.readLock().lock();
		try{	
			ResourceInfo resourceInfo = index.getResourceInfo(lookupString);
			
			if (resourceInfo != null) {
				return new OsgiResource(
						resourceInfo.getUrl(), 
						resourceName, 
						null, 
						resourceInfo.getLastModified());
			}
		}finally {
			readWriteLock.readLock().unlock();
		}
		
		return null;
	}

	/**
	 * Removes the leading '/' because it does not have any meaning
	 * (with and without should point to the same resource)
	 * @param path the resource-path to clean
	 * @return resource-path without leading '/'
     */
	private String cleanLeadingSlashFromPath(String path){
		if (path != null && path.charAt(0) == '/') {
			return path.substring(1);
		}
		return path;
	}

	private class ResourceBundleIndex {
		
		private Map<String, ResourceBundleIndexEntry> indexMap = new HashMap<>(50);

		private void addResourceToIndex(String lookupPath, ResourceInfo resourceInfo, Bundle bundleWithResource) {
			if (StringUtils.isBlank(lookupPath) || resourceInfo == null || bundleWithResource == null) {
				return;
			}
			if (indexMap.containsKey(lookupPath)) {
				ResourceBundleIndexEntry entry = indexMap.get(lookupPath);
				Bundle currentlyProvidingBundle = context.getBundle(entry.getResourceInfo().getBundleId());
				logger.warn(
						"Resource with path '{}' is already provided by bundle '{}'! Will be overriden by bundle '{}'",
						new Object[] {
								lookupPath,
								currentlyProvidingBundle.getSymbolicName(),
								bundleWithResource.getSymbolicName()});
				shadowedMap.add(indexMap.get(lookupPath));
			}
			indexMap.put(lookupPath,
					new ResourceBundleIndexEntry(lookupPath, resourceInfo));
		}

		private ResourceInfo getResourceInfo(String lookupPath) {
			ResourceBundleIndexEntry entry = indexMap.get(lookupPath);
			return entry != null ? entry.getResourceInfo() : null;
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
			logger.info("Removed all resources from bundle '{}'", removedBundleId);
			// revoke the matching shadowed-entries back to the indexMap
			entriesToBeRevoked.forEach(entry -> {
				indexMap.put(entry.getLookupPath(), entry);
				logger.info("Revoking shadowed resource '{}' from bundle '{}'", entry.getLookupPath(),
						context.getBundle(entry.getResourceInfo().getBundleId()).getSymbolicName());
				shadowedMap.remove(entry);
			});
		}
	}

	private class ResourceBundleIndexEntry {

		private String lookupPath;
		private ResourceInfo resourceInfo;

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
	
	private class ResourceInfo {
		private URL url;
		private LocalDateTime lastModified;
		private long bundleId;
		
		private ResourceInfo(URL url, LocalDateTime lastModified, long bundleId) {
			this.url = url;
			this.lastModified = lastModified;
			this.bundleId = bundleId;
		}

		public URL getUrl() {
			return url;
		}

		public LocalDateTime getLastModified() {
			return lastModified;
		}
		
		private long getBundleId() {
			return bundleId;
		}
	}

}
