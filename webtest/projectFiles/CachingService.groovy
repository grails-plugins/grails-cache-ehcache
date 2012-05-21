import grails.plugin.cache.CachePut
import grails.plugin.cache.Cacheable

class CachingService {

	static transactional = false

	private int invocationCounter = 0

	int getInvocationCounter() {
		invocationCounter
	}

	@Cacheable('basic')
	String getData() {
		++invocationCounter
		'Hello World!'
	}

	@Cacheable(value='basic', key='#key')
	def getData(String key) {
	}

	@CachePut(value='basic', key='#key')
	String getData(String key, String value) {
		"** ${value} **"
	}
}
