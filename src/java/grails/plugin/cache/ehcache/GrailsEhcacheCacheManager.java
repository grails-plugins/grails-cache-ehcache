/* Copyright 2012 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.cache.ehcache;

import grails.plugin.cache.GrailsCacheManager;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.util.Assert;

/**
 * Based on org.springframework.cache.ehcache.EhCacheCacheManager; reworked
 * to return GrailsEhcacheCache instances.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Burt Beckwith
 */
public class GrailsEhcacheCacheManager extends AbstractCacheManager implements GrailsCacheManager {

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected CacheManager cacheManager;
	protected List<String> additionalCacheNames;

	@Override
	protected Collection<Cache> loadCaches() {
		Assert.notNull(cacheManager, "A backing EhCache CacheManager is required");
		Status status = cacheManager.getStatus();
		Assert.isTrue(Status.STATUS_ALIVE.equals(status),
				"An 'alive' EhCache CacheManager is required - current cache is " + status);

		String[] names = cacheManager.getCacheNames();
		Collection<Cache> caches = new LinkedHashSet<Cache>(names.length);
		for (String name : names) {
			caches.add(new GrailsEhcacheCache(cacheManager.getEhcache(name)));
		}
		return caches;
	}

	@Override
	public Cache getCache(String name) {
		Cache cache = super.getCache(name);
		if (cache == null) {
			// check the EhCache cache again (in case the cache was added at runtime)
			Ehcache ehcache = cacheManager.getEhcache(name);
			if (ehcache == null) {
				// create a new one based on defaults
				cacheManager.addCache(name);
				ehcache = cacheManager.getEhcache(name);
			}

			cache = new GrailsEhcacheCache(ehcache);
			addCache(cache);
		}
		return cache;
	}

	public boolean cacheExists(String name) {
		return getCacheNames().contains(name);
	}

	public boolean destroyCache(String name) {
		cacheManager.removeCache(name);
		return true;
	}

	/**
	 * Set the backing EhCache {@link net.sf.ehcache.CacheManager}.
	 */
	public void setCacheManager(CacheManager manager) {
		cacheManager = manager;
	}

	/**
	 * Dependency injection for optional names of caches to create with default settings.
	 * @param names
	 */
	public void setAdditionalCacheNames(List<String> names) {
		additionalCacheNames = names;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(cacheManager, "A backing EhCache CacheManager is required");
		if (Status.STATUS_ALIVE != cacheManager.getStatus()) {
			// loadCaches() will assert on status, so no need to do anything here
			return;
		}

		String[] registered = cacheManager.getCacheNames();
		int cacheCount = registered == null ? 0 : registered.length;
		cacheCount += additionalCacheNames == null ? 0 : additionalCacheNames.size();
		if (cacheCount == 0) {
			// AbstractCacheManager requires at least one
			cacheManager.addCache("_default");
		}

		if (additionalCacheNames != null) {
			for (String name : additionalCacheNames) {
				// creates a cache with the default settings in ehcache.xml
				cacheManager.addCache(name);
			}
		}

		super.afterPropertiesSet();

		log.debug("Cache names: {}", getCacheNames());
	}
}
