/* Copyright 2012-2013 SpringSource.
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
package grails.plugin.cache.ehcache

import grails.plugin.cache.CacheException
import grails.plugin.cache.GrailsCacheManager
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.ehcache.CacheManager
import org.ehcache.Status
import org.ehcache.config.CacheConfiguration
import org.ehcache.config.Configuration
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.springframework.beans.factory.DisposableBean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import org.springframework.beans.factory.InitializingBean
import org.springframework.cache.Cache
import org.springframework.util.Assert

/**
 * Based on org.springframework.cache.ehcache.EhCacheCacheManager; reworked
 * to return GrailsEhcacheCache instances.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Burt Beckwith
 * @author Andrew Walters
 */
@Slf4j
@CompileStatic
class GrailsEhcacheCacheManager implements GrailsCacheManager, InitializingBean, DisposableBean {

	protected CacheManager cacheManager
	protected final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>()
	protected Set<String> cacheNames = new LinkedHashSet<String>()
	protected Configuration configuration

	private final Lock lock = new ReentrantLock()

    Long lockTimeout = 200

    @Override
	Cache getCache(final String name) {
		Cache cache = cacheMap.get(name)
		if (cache == null) {
			try {
				cache = getOrCreateCache(name)
			}
			catch (InterruptedException e) {
				throw new CacheException("Failed to get lock for " + name + " cache creation");
			}
		}
		return cache
	}

	protected Cache getOrCreateCache(String name) throws InterruptedException {
		// Ensure we don't have parallel access to cache creation which can lead to 'cache already exists' exceptions
		if (!lock.tryLock(lockTimeout, TimeUnit.MILLISECONDS)) {
			throw new CacheException("Failed to get lock for " + name + " cache creation")
		}

		try {
            Map<String, CacheConfiguration<?, ?>> configurations = cacheManager.runtimeConfiguration.cacheConfigurations
            org.ehcache.Cache cache
            if (configurations.containsKey(name)) {
                CacheConfiguration configuration = configurations.get(name)
                cache = cacheManager.getCache(name, configuration.keyType, configuration.valueType)
            } else {
                cache = createDefaultCache(name)
            }
			Cache grailsCache = new GrailsEhcacheCache(name, cache)
			addCache(grailsCache)
            grailsCache
		}
		finally {
			lock.unlock()
		}
	}

    protected org.ehcache.Cache createDefaultCache(String name) {
        cacheManager.createCache(name, CacheConfigurationBuilder.newCacheConfigurationBuilder(Object, Object, ResourcePoolsBuilder.heap(10)))
    }

    protected CacheManager createDefaultManager() {
        CacheManagerBuilder.newCacheManagerBuilder().build()
    }

	void setConfiguration(EhcacheConfiguration configuration) {
        this.configuration = configuration.getConfiguration()
	}

    @Override
	boolean cacheExists(String name) {
		getCacheNames().contains(name)
	}

    @Override
	boolean destroyCache(String name) {
		cacheManager.removeCache(name)
		cacheMap.remove(name)
		cacheNames.remove(name)
		true
	}

    @Override
	Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(cacheNames)
	}

    protected GrailsEhcacheCache buildCache(String name, CacheConfiguration configuration) {
        new GrailsEhcacheCache(name, cacheManager.getCache(name, configuration.keyType, configuration.valueType)
        )
    }

	protected Collection<Cache> loadCaches() {
		Assert.notNull(cacheManager, "A backing EhCache CacheManager is required")
		Status status = cacheManager.getStatus()
		Assert.isTrue(Status.AVAILABLE == status,
				"An 'available' EhCache CacheManager is required - current cache is " + status)

        Map<String, CacheConfiguration<?, ?>> configurations = cacheManager.runtimeConfiguration.cacheConfigurations
		Set<String> names = configurations.keySet()
		Collection<Cache> caches = new LinkedHashSet<Cache>(names.size())
		for (String name : names) {
            CacheConfiguration configuration = configurations.get(name)
			caches.add(buildCache(name, configuration))
		}
		return caches
	}

	protected void addCache(Cache cache) {
		cacheMap.put(cache.getName(), cache)
		cacheNames.add(cache.getName())
	}

    CacheManager getUnderlyingCacheManager() {
        cacheManager
    }

	@Override
	 void afterPropertiesSet() {
        if (configuration != null) {
            log.debug("Using provided configuration")
            cacheManager = CacheManagerBuilder.newCacheManager(configuration)
        } else {
            log.debug("Using default configuration")
            cacheManager = createDefaultManager()
        }
        cacheManager.init()
		if (Status.AVAILABLE != cacheManager.getStatus()) {
			// loadCaches() will assert on status, so no need to do anything here
			return
		}

		Collection<? extends Cache> caches = loadCaches()
		// preserve the initial order of the cache names
		for (Cache cache : caches) {
			addCache(cache)
		}

		log.debug("Cache names: {}", getCacheNames())
	}

    @Override
    void destroy() throws Exception {
        cacheManager.close()
    }
}
