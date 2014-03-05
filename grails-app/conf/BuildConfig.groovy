grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for gh-pages branch

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	dependencies {
		compile 'net.sf.ehcache:ehcache:2.8.1', {
			excludes 'bsh', 'btm', 'commons-logging', 'derby', 'dom4j', 'hamcrest-core',
			         'hamcrest-library', 'hibernate', 'hibernate-core', 'hibernate-ehcache',
			         'javassist', 'jta', 'junit', 'mockito-core', 'servlet-api', 'sizeof-agent',
			         'slf4j-api', 'slf4j-jdk14', 'xsom'
		}

		test 'org.codehaus.gpars:gpars:1.0.0', {
			export = false
		}
		test 'org.codehaus.jsr166-mirror:jsr166y:1.7.0', {
			export = false
		}
	}

	plugins {
		build ':release:2.2.1', ':rest-client-builder:1.0.3', {
			export = false
		}

		compile ":hibernate:$grailsVersion", {
			export = false
		}

		compile ':cache:1.1.1'
	}
}
