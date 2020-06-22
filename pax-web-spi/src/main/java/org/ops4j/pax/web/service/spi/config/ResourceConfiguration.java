/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi.config;

/**
 * Configuration related to "default servlets"
 */
public interface ResourceConfiguration {

	/**
	 * <p>Boolean property to specify whether default servlet should reply with {@code Accept-Ranges: bytes} header.</p>
	 */
	boolean acceptRanges();

	/**
	 * <p>Boolean property to specify whether <em>welcome file</em> should be served immediately, or by redirect.</p>
	 */
	boolean redirectWelcome();

	/**
	 * <p>Boolean property to specify whether <em>dir index</em> should be present when accessing <em>dir
	 * resource</em>.</p>
	 */
	boolean dirListing();

	/**
	 * <p>Integer property to specify maximum number of cache entries (per single <em>resource manager</em>).</p>
	 */
	int maxCacheEntries();

	/**
	 * <p>Integer property to specify maximum size of single cache entry (file) (per single <em>resource manager</em>).</p>
	 */
	int maxCacheEntrySize();

	/**
	 * <p>Integer property to specify maximum total size of the cache (per single <em>resource manager</em>).</p>
	 */
	int maxTotalCacheSize();

}
