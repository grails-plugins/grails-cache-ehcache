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
package grails.plugin.cache.ehcache

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.core.io.ByteArrayResource

import grails.plugin.cache.ConfigLoader
import grails.plugin.cache.ehcache.GrailsEhCacheManagerFactoryBean.ReloadableCacheManager

/**
 * @author Burt Beckwith
 */
class EhcacheConfigLoader extends ConfigLoader {

	protected Logger log = LoggerFactory.getLogger(getClass())

	void reload(List<ConfigObject> configs, ApplicationContext ctx) {

		// process in reverse order so lower order values have higher priority
		// and can override previous settings
		EhcacheConfigBuilder builder = new EhcacheConfigBuilder()
		for (ListIterator<ConfigObject> iter = configs.listIterator(configs.size()); iter.hasPrevious(); ) {
			ConfigObject co = iter.previous()
			def config = co.config
			if (config instanceof Closure) {
				builder.parse config
			}
		}

		String xml = builder.toXml()
		log.debug "Ehcache generated XML:\n$xml"

		GrailsEhcacheCacheManager cacheManager = ctx.grailsCacheManager
		ReloadableCacheManager nativeCacheManager = cacheManager.cacheManager
		nativeCacheManager.rebuild new ByteArrayResource(xml.bytes)
	}
}
