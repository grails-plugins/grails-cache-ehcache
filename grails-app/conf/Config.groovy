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
		maxElementsInMemory 10000
		maxElementsOnDisk 10000000
	}

	defaults {
		timeToLiveSeconds 1234
	}
}
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"
