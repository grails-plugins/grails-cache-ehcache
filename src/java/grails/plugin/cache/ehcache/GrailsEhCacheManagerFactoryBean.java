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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

/**
 * Based on org.springframework.cache.ehcache.EhCacheManagerFactoryBean.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Burt Beckwith
 */
public class GrailsEhCacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean, DisposableBean {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected Resource configLocation;
	protected String cacheManagerName;
	protected ReloadableCacheManager cacheManager;

	public CacheManager getObject() {
		return cacheManager;
	}

	public Class<? extends ReloadableCacheManager> getObjectType() {
		return cacheManager == null ? ReloadableCacheManager.class : cacheManager.getClass();
	}

	public boolean isSingleton() {
		return true;
	}

	public void afterPropertiesSet() throws IOException, CacheException {
		logger.info("Initializing EHCache CacheManager");

		InputStream inputStream;
		if (configLocation == null) {
			// use dummy configuration for now, will be done for real via rebuild()
			String dummyXml =
					"<ehcache updateCheck='false'>" +
					"<defaultCache maxElementsInMemory='1' eternal='false' overflowToDisk='false' timeToLiveSeconds='1234' />" +
					"</ehcache>";
			inputStream = new ByteArrayInputStream(dummyXml.getBytes());
		}
		else {
			inputStream = configLocation.getInputStream();
		}

		try {
			cacheManager = new ReloadableCacheManager(inputStream);
		}
		finally {
			inputStream.close();
		}

		if (cacheManagerName != null) {
			cacheManager.setName(cacheManagerName);
		}
	}

	public void destroy() {
		logger.info("Shutting down EHCache CacheManager");
		cacheManager.shutdown();
	}

	/**
	 * Set the location of the EHCache config file. A typical value is "/WEB-INF/ehcache.xml".
	 * <p>Default is "ehcache.xml" in the root of the class path, or if not found,
	 * "ehcache-failsafe.xml" in the EHCache jar (default EHCache initialization).
	 * @see net.sf.ehcache.CacheManager#create(java.io.InputStream)
	 * @see net.sf.ehcache.CacheManager#CacheManager(java.io.InputStream)
	 */
	public void setConfigLocation(Resource location) {
		configLocation = location;
	}

	/**
	 * Set the name of the EHCache CacheManager (if a specific name is desired).
	 * @see net.sf.ehcache.CacheManager#setName(String)
	 */
	public void setCacheManagerName(String name) {
		cacheManagerName = name;
	}

	public static class ReloadableCacheManager extends CacheManager {

		public ReloadableCacheManager(InputStream inputStream) {
			super(inputStream);
		}

		public void rebuild(Resource location) throws IOException {

	      for (String cacheName : getCacheNames()) {
	      	removeCache(cacheName);
	      }

			status = Status.STATUS_UNINITIALISED;

			// TODO ugly hack since the field is private
			Field diskStorePath = ReflectionUtils.findField(getClass(), "diskStorePath", String.class);
			ReflectionUtils.makeAccessible(diskStorePath);
			ReflectionUtils.setField(diskStorePath, this, null);

			// remove since it's going to be re-added
			ALL_CACHE_MANAGERS.remove(this);

	      init(null, null, null, location.getInputStream());
		}
	}
}
