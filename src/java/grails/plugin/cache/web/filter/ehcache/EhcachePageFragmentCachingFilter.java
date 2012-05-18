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

//	@Override
//	protected void replaceCacheWithDecoratedCache(Cache cache, BlockingCache blocking) {
//		getNativeCacheManager().replaceCacheWithDecoratedCache(
//				(Ehcache)cache.getNativeCache(), (Ehcache)blocking.getNativeCache());
//	}

//	@Override
//	protected BlockingCache createBlockingCache(Cache cache) {
//		return new EhcacheBlockingCache((Ehcache)cache.getNativeCache());
//	}

	@Override
	protected int getTimeToLive(ValueWrapper wrapper) {
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
	protected void put(Cache cache, String key, PageInfo pageInfo, Integer timeToLiveSeconds) {
		Element element = new Element(key, pageInfo);
		if (timeToLiveSeconds != null) {
			element.setTimeToLive(timeToLiveSeconds);
		}
		((EhCacheCache)cache).getNativeCache().put(element);
	}
}
