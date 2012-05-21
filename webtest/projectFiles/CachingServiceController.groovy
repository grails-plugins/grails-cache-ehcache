class CachingServiceController {

	def cachingService

	def cachingServiceInvocationCount() {
		render 'Basic Caching Service Invocation Count Is ' + cachingService.invocationCounter
	}

	def cachingService() {
		render "Value From Service Is '$cachingService.data'"
	}

	def cachePut(String key, String value) {
		render 'Result: ' + cachingService.getData(key, value)
	}

	def cacheGet(String key) {
		render 'Result: ' + cachingService.getData(key)
	}
}
