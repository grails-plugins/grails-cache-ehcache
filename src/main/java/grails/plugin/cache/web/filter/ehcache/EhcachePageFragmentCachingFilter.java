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
package grails.plugin.cache.web.filter.ehcache;

import grails.plugin.cache.GrailsValueWrapper;
import grails.plugin.cache.web.PageInfo;
import grails.plugin.cache.web.filter.PageFragmentCachingFilter;
import net.sf.ehcache.Element;

import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.ehcache.EhCacheCache;

/**
 * Ehcache-based implementation of PageFragmentCachingFilter.
 *
 * @author Burt Beckwith
 */
public class EhcachePageFragmentCachingFilter extends PageFragmentCachingFilter {

	protected static final long ONE_YEAR_IN_SECONDS = 60 * 60 * 24 * 365;

	@Override
	protected int getTimeToLive(final ValueWrapper wrapper) {
		if (wrapper instanceof GrailsValueWrapper) {
			Element e = (Element)((GrailsValueWrapper)wrapper).getNativeWrapper();
			return e.getTimeToLive();
		}
		return Integer.MAX_VALUE;
	}

	@Override
	protected net.sf.ehcache.CacheManager getNativeCacheManager() {
		return (net.sf.ehcache.CacheManager)super.getNativeCacheManager();
	}

	@Override
	protected void put(final Cache cache, final String key, final PageInfo pageInfo, final Integer timeToLiveSeconds) {
		Element element = new Element(key, pageInfo);
		if (timeToLiveSeconds == null || timeToLiveSeconds >= ONE_YEAR_IN_SECONDS) {
			element.setTimeToLive((int) ((EhCacheCache)cache).getNativeCache().getCacheConfiguration().getTimeToLiveSeconds());
		}
		else {
			element.setTimeToLive(timeToLiveSeconds);
		}
		((EhCacheCache)cache).getNativeCache().put(element);

		log.debug("Put element into cache [{0}] with ttl [{1}]", new Object[] { cache.getName(), element.getTimeToLive() });
	}
}
