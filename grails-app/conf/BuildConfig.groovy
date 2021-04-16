grails.project.work.dir = 'target'

grails.project.dependency.resolver = "maven"

grails.project.repos.grailsCentral.username = System.getenv("GRAILS_CENTRAL_USERNAME")
grails.project.repos.grailsCentral.password = System.getenv("GRAILS_CENTRAL_PASSWORD")

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
        // Grails Central
        mavenRepo 'https://repo.grails.org/grails/core/'
		//grailsCentral()

        // Grails Plugins
        mavenRepo 'https://repo.grails.org/grails/plugins/'

		mavenLocal()

        // Maven Central
        mavenRepo 'https://repo1.maven.org/maven2/'
		//mavenCentral()
	}

	dependencies {
		compile 'net.sf.ehcache:ehcache:2.9.0', {
			excludes 'bsh', 'btm', 'commons-logging', 'derby', 'dom4j', 'hamcrest-core',
			         'hamcrest-library', 'hibernate', 'hibernate-core', 'hibernate-ehcache',
			         'javassist', 'jta', 'junit', 'mockito-core', 'servlet-api', 'sizeof-agent',
			         'slf4j-api', 'slf4j-jdk14', 'xsom'
		}
		test 'org.codehaus.jsr166-mirror:jsr166y:1.7.0', {
			export = false
		}
	}

	plugins {
		build ':release:3.0.1', ':rest-client-builder:2.0.3', {
			export = false
		}

		compile(":hibernate4:4.3.5.4"){
			excludes "net.sf.ehcache:ehcache-core"  // remove this when http://jira.grails.org/browse/GPHIB-18 is resolved
			export = false
		}

		compile ':cache:1.1.8'
	}
}
