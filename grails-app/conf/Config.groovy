log4j = {
	error 'org.codehaus.groovy.grails',
	      'org.springframework',
	      'org.hibernate',
	      'net.sf.ehcache.hibernate'
	debug 'grails.plugin.cache'
}

// for tests
grails.cache.config = {
	cache {
		name 'mycache'
		eternal false
		overflowToDisk true
		maxBytesLocalHeap "5M"
		maxElementsOnDisk 10000000
	}

	defaults {
		timeToLiveSeconds 1234
	}
}
