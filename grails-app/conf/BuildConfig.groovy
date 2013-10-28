grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for gh-pages branch
grails.project.source.level = 1.6

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
	}

	dependencies {
		compile 'net.sf.ehcache:ehcache:2.7.4'
		compile('org.hibernate:hibernate-ehcache:3.6.10.Final'){
			// if the application that uses this plugin happens to not use Hibernate, don't pull in Hibernate
			export = false
		}

        test 'org.codehaus.gpars:gpars:1.0.0'
	}

	plugins {
		build(':release:2.0.4', ':rest-client-builder:1.0.2') {
			export = false
		}
		compile ':cache:1.1.1'
	}
}
