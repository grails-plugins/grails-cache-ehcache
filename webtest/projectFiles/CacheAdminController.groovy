import grails.plugin.cache.web.PageInfo

import org.springframework.cache.Cache

class CacheAdminController {

	def grailsCacheManager

	def index() {

		def caches = [:]
		for (String name in grailsCacheManager.cacheNames) {
			caches[name] = grailsCacheManager.getCache(name)
		}

		[caches: caches]
	}

	def cache(String name) {

		def cache
		def data = []
		if (grailsCacheManager.cacheExists(name)) {
			cache = grailsCacheManager.getCache(name)
			for (key in cache.allKeys) {
				def value = cache.get(key)?.get()
				String html
				if (value instanceof PageInfo) {
					html = new String(value.ungzippedBody, response.characterEncoding)
				}
				data << [key: key, value: value, html: html]
			}
		}

		[cache: cache, data: data]
	}

	def cacheItem(String name, String key) {
		def value = grailsCacheManager.getCache(name).get(key)?.get()
		String html
		if (value instanceof PageInfo) {
			html = new String(value.ungzippedBody, response.characterEncoding)
		}

		[name: name, key: key, value: value, html: html]
	}

	def clearCache(String cacheName) {
		Cache cache = grailsCacheManager.getCache(cacheName)
		cache.clear()
		flash.message = "cleared cache '$cacheName'"
		redirect action: 'index'
	}
}
