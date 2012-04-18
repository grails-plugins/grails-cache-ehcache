log4j = {
	error 'org.codehaus.groovy.grails',
	      'org.springframework',
	      'org.hibernate',
	      'net.sf.ehcache.hibernate'
	debug 'grails.plugin.cache'
}

// for unit tests
grails.cache.config = {
	cache {
		name 'mycache'
		eternal false
		overflowToDisk true
		maxElementsInMemory 10000
		maxElementsOnDisk 10000000
	}
}
