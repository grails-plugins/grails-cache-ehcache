grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
	}

	dependencies {
		compile 'net.sf.ehcache:ehcache-core:2.4.6'
	}

	plugins {
		build(':release:1.0.1', ':svn:1.0.2') {
			export = false
		}
		compile ':cache:0.5.BUILD-SNAPSHOT'
	}
}
