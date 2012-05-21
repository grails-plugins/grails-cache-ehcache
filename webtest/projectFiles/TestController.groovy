import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable

class TestController extends AbstractCacheController {

	@Cacheable('message')
	def index() {
		new LogEntry(message: 'Called index() action').save(failOnError: true, flush: true)
		render 'index'
	}

	@Cacheable('message')
	def withParams(String foo, Integer bar) {
		render "withParams $foo $bar"
	}

	@CacheEvict(value='message', allEntries=true)
	def evict() {
		new LogEntry(message: 'Called evict() action').save(failOnError: true, flush: true)
		render 'evict'
	}
}
