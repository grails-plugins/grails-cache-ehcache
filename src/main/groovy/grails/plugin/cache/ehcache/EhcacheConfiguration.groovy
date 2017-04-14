package grails.plugin.cache.ehcache

import org.ehcache.config.Configuration

/**
 * Created by jameskleeh on 3/28/17.
 */
interface EhcacheConfiguration {

    Configuration getConfiguration()
}
