import grails.converters.JSON

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable

class TestController {

	def grailsCacheManager

	@Cacheable('message')
	def index() {
		new LogEntry(message: 'Called index() action').save(failOnError: true, flush: true)
		render 'index'
	}

	@CacheEvict(value='message', allEntries=true)
	def evict() {
		new LogEntry(message: 'Called evict() action').save(failOnError: true, flush: true)
		render 'evict'
	}

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

	def clearCache() {
		grailsCacheManager.getCache('message').evict()
		render 'cleared cache'
	}
}
