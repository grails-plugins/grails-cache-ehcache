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

import groovy.transform.CompileStatic
import org.springframework.cache.support.SimpleValueWrapper
import grails.plugin.cache.GrailsCache
import org.ehcache.Cache

import java.util.concurrent.Callable

/**
 * Extends the default implementation to return GrailsValueWrapper instances instead of
 * SimpleValueWrapper, to include the Ehcache Element instance.
 *
 * @author Burt Beckwith
 */
@CompileStatic
class GrailsEhcacheCache<K, V> implements GrailsCache {

	protected Cache<K, V> ehcache
	protected String name

	GrailsEhcacheCache(String name, Cache<K, V> ehcache) {
		this.ehcache = ehcache
		this.name = name
	}

	@Override
	String getName() {
		name
	}

	@Override
	Cache<K, V> getNativeCache() {
		ehcache
	}

	@Override
	SimpleValueWrapper get(Object key) {
		V element = getNativeCache().get((K)key)
		element == null ? null : new SimpleValueWrapper(element)
	}

	@Override
	<T> T get(Object key, Class<T> type) {
		V value = getNativeCache().get((K)key)
		if (value && type && !type.isInstance(value)) {
			throw new IllegalStateException("Cached value is not of required type [${type.getName()}]: ${value}")
		}
		(T) value
	}

	@Override
	<T> T get(Object key, Callable<T> valueLoader) {
		throw new UnsupportedOperationException()
	}

	@Override
	void put(Object key, Object value) {
		getNativeCache().put((K)key, (V)value)
	}

	@Override
	org.springframework.cache.Cache.ValueWrapper putIfAbsent(Object key, Object value) {
		new SimpleValueWrapper(getNativeCache().putIfAbsent((K)key, (V)value))
	}

	@Override
	void evict(Object key) {
		getNativeCache().remove((K)key)
	}

	@Override
	void clear() {
		getNativeCache().clear()
	}

	@SuppressWarnings("unchecked")
	Collection<Object> getAllKeys() {
		Set<K> keys = []
		getNativeCache().each {
			keys.add(it.key)
		}
		keys
	}
}
