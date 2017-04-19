package cacheehcache

import grails.plugin.cache.ehcache.DefaultXmlConfiguration
import grails.plugin.cache.ehcache.GrailsEhcacheCacheManager
import grails.plugins.Plugin

class CacheEhcacheGrailsPlugin extends Plugin {

   // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.0 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def version = '3.0.0.BUILD-SNAPSHOT'

    def title = "Cache Ehcache" // Headline display name of the plugin
    def author = "Jeff Brown"
    def authorEmail = "jeff@jeffandbetsy.net"
    def description = '''\
An Ehcache-based implementation of the Cache plugin.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/cache-ehcache"

    def license = "APACHE"

    def developers = [ [ name: "Burt Beckwith", email: "burt@burtbeckwith.com" ]]

    def scm = [ url: "https://github.com/grails-plugins/grails-cache-ehcache" ]

    def loadAfter = ['cache']

    Closure doWithSpring() {
        { ->
            String ehcacheXmlLocation = config.getProperty('grails.cache.ehcache.ehcacheXmlLocation', String, 'classpath:ehcache.xml')
            Long timeout = config.getProperty('grails.cache.ehcache.lockTimeout', Long, 200)

            log.info "Attempting to use Ehcache configuration file $ehcacheXmlLocation"

            ehcacheConfiguration(DefaultXmlConfiguration, ehcacheXmlLocation)

            grailsCacheManager(GrailsEhcacheCacheManager) {
                configuration = ref('ehcacheConfiguration')
                lockTimeout = timeout
            }

        }
    }
}
