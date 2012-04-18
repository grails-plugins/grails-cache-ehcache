/* Copyright 2012 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import grails.plugin.cache.ehcache.EhcacheConfigBuilder
import grails.plugin.cache.ehcache.GrailsEhcacheCacheManager
import grails.plugin.cache.web.filter.ehcache.EhcachePageFragmentCachingFilter

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean
import org.springframework.core.io.ByteArrayResource

class CacheEhcacheGrailsPlugin {

	private final Logger log = LoggerFactory.getLogger('grails.plugin.cache.CacheEhcacheGrailsPlugin')

	String version = '0.1'
	String grailsVersion = '2.0 > *'
	def loadAfter = ['cache']

	String title = 'Ehcache Cache Plugin'
	String author = 'Burt Beckwith'
	String authorEmail = 'beckwithb@vmware.com'
	String description = 'An Ehcache-based implementation of the Cache plugin'
	String documentation = 'http://grails.org/plugin/cache-ehcache'

	String license = 'APACHE'
	def organization = [name: 'SpringSource', url: 'http://www.springsource.org/']
	def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPCACHEEHCACHE']
	def scm = [url: 'https://github.com/grails-plugins/grails-cache-ehcache']

	def doWithSpring = {
		if (!isEnabled(application)) {
			log.warn 'Ehcache Cache plugin is disabled'
			return
		}

		def cacheConfig = application.config.grails.cache
		def ehcacheConfig = cacheConfig.ehcache
		def ehcacheConfigLocation
		if (cacheConfig.config instanceof Closure) {
			// parse the config into XML and let Ehcache configure itself from that
			EhcacheConfigBuilder builder = new EhcacheConfigBuilder()
			builder.parse cacheConfig.config
			String xml = builder.toXml()
			log.debug "Ehcache generated XML:\n$xml"
			ehcacheConfigLocation = new ByteArrayResource(xml.bytes)
		}
		else if (ehcacheConfig.ehcacheXmlLocation instanceof CharSequence) {
			// use the specified location
			ehcacheConfigLocation = ehcacheConfig.ehcacheXmlLocation
			log.info "Using Ehcache configuration file $ehcacheConfigLocation"
		}
		else {
			// no config and no specified location, so look for ehcache.xml in the classpath,
			// and fall back to ehcache-failsafe.xml in the Ehcache jar as a last resort
			def ctx = springConfig.unrefreshedApplicationContext
			def defaults = ['classpath:ehcache.xml', 'classpath:ehcache-failsafe.xml']
			ehcacheConfigLocation = defaults.find { ctx.getResource(it).exists() }
			if (ehcacheConfigLocation) {
				log.info "No Ehcache configuration file specified, using $ehcacheConfigLocation"
			}
			else {
				log.error "No Ehcache configuration file specified and default file not found"
			}
		}

		ehcacheCacheManager(EhCacheManagerFactoryBean) {
			configLocation = ehcacheConfigLocation
		}

		cacheManager(GrailsEhcacheCacheManager) {
			cacheManager = ref('ehcacheCacheManager')
			additionalCacheNames = cacheConfig.additionalCacheNames ?: []
		}

		grailsCacheFilter(EhcachePageFragmentCachingFilter) {
			cacheManager = ref('cacheManager')
			nativeCacheManager = ref('ehcacheCacheManager')
			// TODO this name might be brittle - perhaps do by type?
			cacheOperationSource = ref('org.springframework.cache.annotation.AnnotationCacheOperationSource#0')
			keyGenerator = ref('webCacheKeyGenerator')
			expressionEvaluator = ref('webExpressionEvaluator')
		}
	}

	private boolean isEnabled(GrailsApplication application) {
		// TODO
		true
	}
}
