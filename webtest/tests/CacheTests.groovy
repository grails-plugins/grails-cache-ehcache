import functionaltestplugin.FunctionalTestCase
import grails.converters.JSON

class CacheTests extends FunctionalTestCase {

	@Override
	protected void setUp() {
		super.setUp()

		get '/test/evict'
		assertContent 'evict'

		get '/test/clearCache'
		assertContent 'cleared cache'

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
}
