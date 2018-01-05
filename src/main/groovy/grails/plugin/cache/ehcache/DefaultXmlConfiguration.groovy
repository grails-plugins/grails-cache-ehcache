package grails.plugin.cache.ehcache

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.ehcache.config.Configuration
import org.ehcache.xml.XmlConfiguration
import org.grails.io.support.PathMatchingResourcePatternResolver
import org.grails.io.support.Resource

@CompileStatic
@Slf4j
class DefaultXmlConfiguration implements EhcacheConfiguration {

    Configuration configuration

    DefaultXmlConfiguration(String location) {
        def resolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader())
        Resource resource = resolver.getResource(location)
        log.debug("Attempting to search for ehcache xml configuration at $location")
        if (resource && resource.exists()) {
            log.debug("Configuration found at $location")
            configuration = new XmlConfiguration(resource.getURL())
        } else {
            log.debug("Configuration not found")
        }
    }

}
