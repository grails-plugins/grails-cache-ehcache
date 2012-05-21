class TaglibController {

	def grailsCacheAdminService

	def clearBlocksCache() {
		grailsCacheAdminService.clearBlocksCache()
		render 'cleared blocks cache'
	}

	def clearTemplatesCache() {
		grailsCacheAdminService.clearTemplatesCache()
		render 'cleared templates cache'
	}

	def blockCache(int counter) {
		[counter: counter]
	}

	def renderTag(int counter) {
		[counter: counter]
	}
}
