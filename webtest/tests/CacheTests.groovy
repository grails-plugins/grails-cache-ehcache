import functionaltestplugin.FunctionalTestCase
import grails.converters.JSON

class CacheTests extends FunctionalTestCase {

	@Override
	protected void setUp() {
		super.setUp()

		get '/test/evict'
		assertContent 'evict'

		get '/test/clearCache?cacheName=message'
		assertContent "cleared cache 'message'"

		get '/test/clearLogEntries'
		assertContent 'deleted all LogEntry instances'
	}

	void testCacheAndEvict() {

		// check that there are no log entries
		get '/test/logEntryCount'
		assertContent '0'

		get '/test/mostRecentLogEntry'
		assertContent 'none'

		// get the index action which should trigger caching

		get '/test/index'
		assertContent 'index'

		get '/test/logEntryCount'
		assertContent '1'

		get '/test/mostRecentLogEntry'

		def json = JSON.parse(response.contentAsString)

		assertEquals 'Called index() action', json.message
		long id = json.id
		long dateCreated = json.dateCreated

		// get the index action again, should be cached

		get '/test/index'
		assertContent 'index'

		get '/test/logEntryCount'
		assertContent '1'

		get '/test/mostRecentLogEntry'
		json = JSON.parse(response.contentAsString)

		assertEquals 'Called index() action', json.message
		assertEquals id, json.id
		assertEquals dateCreated, json.dateCreated

		// evict

		get '/test/evict'
		assertContent 'evict'

		get '/test/logEntryCount'
		assertContent '2'

		get '/test/mostRecentLogEntry'
		json = JSON.parse(response.contentAsString)

		assertEquals 'Called evict() action', json.message
		assertEquals id + 1, json.id
		assertTrue dateCreated < json.dateCreated

		// save the values to compare
		id++
		dateCreated = json.dateCreated

		// get the index action again, should not be cached

		get '/test/index'
		assertContent 'index'

		get '/test/logEntryCount'
		assertContent '3'

		get '/test/mostRecentLogEntry'
		json = JSON.parse(response.contentAsString)

		assertEquals 'Called index() action', json.message
		assertEquals id + 1, json.id
		assertTrue dateCreated < json.dateCreated
	}

	void testParams() {
		get '/test/withParams?foo=baz&bar=123'
		assertContent 'withParams baz 123'

		get '/test/withParams?foo=baz2&bar=1234'
		assertContent 'withParams baz2 1234'

		get '/test/withParams?foo=baz&bar=123'
		assertContent 'withParams baz 123'

		// try again with UrlMappings

		get '/withParams/baz/123'
		assertContent 'withParams baz 123'

		get '/withParams/baz2/1234'
		assertContent 'withParams baz2 1234'

		get '/withParams/baz/123'
		assertContent 'withParams baz 123'
	}

	void testBasicCachingService() {
		get '/cachingService/cachingServiceInvocationCount'
		assertStatus 200
		assertContentContains 'Basic Caching Service Invocation Count Is 0'

		get '/cachingService/cachingServiceInvocationCount'
		assertStatus 200
		assertContentContains 'Basic Caching Service Invocation Count Is 0'

		get '/cachingService/cachingService'
		assertStatus 200
		assertContentContains "Value From Service Is 'Hello World!'"
		
		get '/cachingService/cachingServiceInvocationCount'
		assertStatus 200
		assertContentContains 'Basic Caching Service Invocation Count Is 1'

		get '/cachingService/cachingService'
		assertStatus 200
		assertContentContains "Value From Service Is 'Hello World!'"

		get '/cachingService/cachingServiceInvocationCount'
		assertStatus 200
		assertContentContains 'Basic Caching Service Invocation Count Is 1'
	}

	void testBasicCachePutService() {
		get '/cachingService/cacheGet?key=band'
		assertStatus 200
		assertContentContains 'Result: null'

		get '/cachingService/cachePut?key=band&value=Thin+Lizzy'
		assertStatus 200
		assertContentContains 'Result: ** Thin Lizzy **'

		get '/cachingService/cacheGet?key=band'
		assertStatus 200
		assertContentContains 'Result: ** Thin Lizzy'

		get '/cachingService/cacheGet?key=singer'
		assertStatus 200
		assertContentContains 'Result: null'

		get '/cachingService/cachePut?key=singer&value=Phil+Lynott'
		assertStatus 200
		assertContentContains 'Result: ** Phil Lynott **'

		get '/cachingService/cacheGet?key=singer'
		assertStatus 200
		assertContentContains 'Result: ** Phil Lynott'

		get '/cachingService/cachePut?key=singer&value=John+Sykes'
		assertStatus 200
		assertContentContains 'Result: ** John Sykes **'

		get '/cachingService/cacheGet?key=singer'
		assertStatus 200
		assertContentContains 'Result: ** John Sykes'

		get '/cachingService/cacheGet?key=band'
		assertStatus 200
		assertContentContains 'Result: ** Thin Lizzy'
	}

	void testBlockTag() {
		get '/taglib/blockCache?counter=5'
		assertStatus 200
		assertContentContains 'First block counter 6'
		assertContentContains 'Second block counter 7'
		assertContentContains 'Third block counter 8'

		get '/taglib/blockCache?counter=42'
		assertStatus 200
		assertContentContains 'First block counter 6'
		assertContentContains 'Second block counter 7'
		assertContentContains 'Third block counter 8'
	}

	void testClearingBlocksCache() {
		get '/taglib/clearBlocksCache'
		assertStatus 200
		assertContentContains 'cleared blocks cache'

		get '/taglib/blockCache?counter=100'
		assertStatus 200
		assertContentContains 'First block counter 101'
		assertContentContains 'Second block counter 102'
		assertContentContains 'Third block counter 103'

		get '/taglib/blockCache?counter=42'
		assertStatus 200
		assertContentContains 'First block counter 101'
		assertContentContains 'Second block counter 102'
		assertContentContains 'Third block counter 103'

		get '/taglib/clearBlocksCache'
		assertStatus 200
		assertContentContains 'cleared blocks cache'

		get '/taglib/blockCache?counter=50'
		assertStatus 200
		assertContentContains 'First block counter 51'
		assertContentContains 'Second block counter 52'
		assertContentContains 'Third block counter 53'

		get '/taglib/blockCache?counter=150'
		assertStatus 200
		assertContentContains 'First block counter 51'
		assertContentContains 'Second block counter 52'
		assertContentContains 'Third block counter 53'
	}

	void testRenderTag() {
		get '/taglib/clearTemplatesCache'
		assertStatus 200
		assertContentContains 'cleared templates cache'

		get '/taglib/renderTag?counter=1'
		assertStatus 200

		assertContentContains 'First invocation: Counter value: 1'
		assertContentContains 'Second invocation: Counter value: 1'
		assertContentContains 'Third invocation: Counter value: 3'
		assertContentContains 'Fourth invocation: Counter value: 3'
		assertContentContains 'Fifth invocation: Counter value: 1'

		get '/taglib/renderTag?counter=5'
		assertStatus 200

		assertContentContains 'First invocation: Counter value: 1'
		assertContentContains 'Second invocation: Counter value: 1'
		assertContentContains 'Third invocation: Counter value: 3'
		assertContentContains 'Fourth invocation: Counter value: 3'
		assertContentContains 'Fifth invocation: Counter value: 1'

		get '/taglib/clearTemplatesCache'
		assertStatus 200
		assertContentContains 'cleared templates cache'

		get '/taglib/renderTag?counter=5'
		assertStatus 200

		assertContentContains 'First invocation: Counter value: 5'
		assertContentContains 'Second invocation: Counter value: 5'
		assertContentContains 'Third invocation: Counter value: 7'
		assertContentContains 'Fourth invocation: Counter value: 7'
		assertContentContains 'Fifth invocation: Counter value: 5'

		get '/taglib/renderTag?counter=1'
		assertStatus 200

		assertContentContains 'First invocation: Counter value: 5'
		assertContentContains 'Second invocation: Counter value: 5'
		assertContentContains 'Third invocation: Counter value: 7'
		assertContentContains 'Fourth invocation: Counter value: 7'
		assertContentContains 'Fifth invocation: Counter value: 5'
	}
}
