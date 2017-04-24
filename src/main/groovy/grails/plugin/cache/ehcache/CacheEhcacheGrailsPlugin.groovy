package grails.plugin.cache.ehcache

import grails.plugin.cache.ehcache.DefaultXmlConfiguration
import grails.plugin.cache.ehcache.GrailsEhcacheCacheManager
import grails.plugins.Plugin

class CacheEhcacheGrailsPlugin extends Plugin {

   // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.0 > *"


    def title = "Cache Ehcache" // Headline display name of the plugin
    def description = '''\
An Ehcache-based implementation of the Cache plugin.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/cache-ehcache"

    def license = "APACHE"

    def developers = [ [ name: "James Kleeh", email: "james.kleeh@gmail.com" ]]

    def issueManagement = [system: 'Github', url: 'https://github.com/grails-plugins/grails-cache-ehcache/issues']

    def scm = [ url: "https://github.com/grails-plugins/grails-cache-ehcache/" ]

    def loadAfter = ['cache']

    def dependsOn = [cache: "4.0.0.BUILD-SNAPSHOT > *"]

    Closure doWithSpring() {
        { ->
            String ehcacheXmlLocation = config.getProperty('grails.cache.ehcache.ehcacheXmlLocation', String, 'classpath:ehcache.xml')
            Long timeout = config.getProperty('grails.cache.ehcache.lockTimeout', Long, 200)

            ehcacheConfiguration(DefaultXmlConfiguration, ehcacheXmlLocation)

            grailsCacheManager(GrailsEhcacheCacheManager) {
                configuration = ref('ehcacheConfiguration')
                lockTimeout = timeout
            }

        }
    }
}
