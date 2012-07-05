import org.springframework.cache.Cache

import grails.converters.JSON

abstract class AbstractCacheController {

	def grailsCacheManager

	def clearLogEntries() {
		LogEntry.list()*.delete()
		render 'deleted all LogEntry instances'
	}

	def logEntryCount() {
		render LogEntry.count() as String
	}

	def mostRecentLogEntry() {
		def entry = LogEntry.listOrderById(order: 'desc', max: 1)[0]
		if (entry) {
			def map = [id: entry.id, message: entry.message, dateCreated: entry.dateCreated.time]
			render map as JSON
			return
		}
		render 'none'
	}

	def clearCache(String cacheName) {
		Cache cache = grailsCacheManager.getCache(cacheName)
		cache.clear()
		render "cleared cache '$cacheName'"
	}
}
