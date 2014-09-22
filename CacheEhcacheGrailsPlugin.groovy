/* Copyright 2012-2013 SpringSource.
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
import java.util.Map;

import grails.plugin.cache.ehcache.EhcacheConfigLoader
import grails.plugin.cache.ehcache.GrailsEhCacheManagerFactoryBean
import grails.plugin.cache.ehcache.GrailsEhcacheCacheManager
import grails.plugin.cache.web.filter.ehcache.EhcachePageFragmentCachingFilter
import net.sf.ehcache.constructs.web.ShutdownListener
import net.sf.ehcache.management.ManagementService

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.tools.GroovyClass;
import org.codehaus.groovy.tools.RootLoader;
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.support.MBeanServerFactoryBean

class CacheEhcacheGrailsPlugin {

	private final Logger log = LoggerFactory.getLogger('grails.plugin.cache.CacheEhcacheGrailsPlugin')
	
	private static final String BEAN_EHCACHE_REGION_FACTORY = '''
package grails.plugin.cache.ehcache.hibernate;

import grails.util.Holders;

import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.EhCacheRegionFactory;

import org.hibernate.cfg.Settings;

/**
 * Use the existing ehCache CacheManager instance instead of creating a new instance. For use with Hibernate 3.x (use {@link grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory4} with Hibernate 4.0+).
 * Configure in DataSource.groovy: hibernate.cache.region.factory_class = 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory
 *
 * @author Craig Andrews
 * @author Burt Beckwith
 */
public class BeanEhcacheRegionFactory extends EhCacheRegionFactory {

	/**
	 * Creates an EhCacheRegionFactory that uses a predefined CacheManager bean
	 */
	public BeanEhcacheRegionFactory(final Properties properties) {
		super(properties);
	}

	public void start(final Settings settings, final Properties properties) throws org.hibernate.cache.CacheException {
		this.settings = settings;
		manager = Holders.getGrailsApplication().getMainContext().getBean("ehcacheCacheManager", CacheManager.class);
		mbeanRegistrationHelper.registerMBean(manager, properties);
	}

	public void stop() {
		if (manager == null) {
			return;
		}

		try {
			mbeanRegistrationHelper.unregisterMBean();
			manager.shutdown();
			manager = null;
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new org.hibernate.cache.CacheException(e);
		}
	}
}
	'''
	private static final String BEAN_EHCACHE_REGION_FACTORY4 = '''
package grails.plugin.cache.ehcache.hibernate;

import grails.util.Holders;

import java.util.Properties;

import net.sf.ehcache.CacheManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.EhCacheRegionFactory;
import org.hibernate.cfg.Settings;

/**
 * Use the existing ehCache CacheManager instance instead of creating a new instance. For use with Hibernate 4.0+.
 * Configure in DataSource.groovy: hibernate.cache.region.factory_class = 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory4
 *
 * Note that most of this code is copied from {@link org.hibernate.cache.DelegatingRegionFactory}, which cannot be extended in this case as its constructor is package-scoped (and this class is not in the same package).
 * 
 * @author Craig Andrews
 */
public class BeanEhcacheRegionFactory4 extends EhCacheRegionFactory {
	
	public BeanEhcacheRegionFactory4(){
		super();
	}

    public BeanEhcacheRegionFactory4(final Properties properties) {
        super(properties);
    }

    public final void start(final Settings settings, final Properties properties) throws CacheException {
		this.settings = settings;
		manager = Holders.getGrailsApplication().getMainContext().getBean("ehcacheCacheManager", CacheManager.class);
		mbeanRegistrationHelper.registerMBean(manager, properties);
    }

}
	'''

	String version = '1.0.4-SNAPSHOT'
	String grailsVersion = '2.0 > *'
	def loadAfter = ['cache']
	def pluginExcludes = [
		'grails-app/conf/*CacheConfig.groovy',
		'scripts/CreateCacheEhcacheTestApps.groovy',
		'docs/**',
		'src/docs/**'
	]

	String title = 'Ehcache Cache Plugin'
	String author = 'Burt Beckwith'
	String authorEmail = 'burt@burtbeckwith.com'
	String description = 'An Ehcache-based implementation of the Cache plugin'
	String documentation = 'http://grails-plugins.github.com/grails-cache-ehcache/'

	String license = 'APACHE'
	def organization = [name: 'SpringSource', url: 'http://www.springsource.org/']
	def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPCACHEEHCACHE']
	def scm = [url: 'https://github.com/grails-plugins/grails-cache-ehcache']
	
	public CacheEhcacheGrailsPlugin(){
		// conditionally load the Hibernate 3 or Hibernate 4 classes
		Map<String, String> toLoad = new HashMap<String,String>(2)
		try{
			Class.forName('org.hibernate.cache.TimestampsRegion')
			toLoad.put('grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory', BEAN_EHCACHE_REGION_FACTORY)
		}catch(ClassNotFoundException e){
			//ignore
		}
		try{
			Class.forName('org.hibernate.cache.spi.TimestampsRegion')
			toLoad.put('grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory4', BEAN_EHCACHE_REGION_FACTORY4)
		}catch(ClassNotFoundException e){
			//ignore
		}
		ClassLoader classLoader = GrailsApplication.class.classLoader
		for(Map.Entry<String, String> entry : toLoad){
			boolean alreadyLoaded
			try{
				Class.forName(entry.key)
				alreadyLoaded=true
			}catch(ClassNotFoundException e){
				alreadyLoaded=false
			}
			if(!alreadyLoaded){
				CompilationUnit compilationUnit = new CompilationUnit()
				compilationUnit.addSource(entry.key, entry.value)
				compilationUnit.compile(Phases.CLASS_GENERATION)
				byte[] bytes = ((GroovyClass)compilationUnit.getClasses()[0]).getBytes()
				Class c = classLoader.defineClass(entry.key, bytes, 0, bytes.length)
				classLoader.resolveClass(c)
			}
		}
	}

	def doWithWebDescriptor = { webXml ->
		def filterMapping = webXml.'filter-mapping'

		// If you are using persistent disk stores, or distributed caching, care should be taken to shutdown Ehcache.
		// http://ehcache.org/documentation/operations/shutdown
		filterMapping[filterMapping.size() - 1] + {
			listener {
				'listener-class'(ShutdownListener.name)
			}
		}
	}

	def doWithSpring = {
		if (!isEnabled(application)) {
			log.warn 'Ehcache Cache plugin is disabled'
			return
		}

		def cacheConfig = application.config.grails.cache
		def ehcacheConfig = cacheConfig.ehcache
		def ehcacheConfigLocation
		boolean reloadable

		if (ehcacheConfig.reloadable instanceof Boolean) {
			reloadable = ehcacheConfig.reloadable
		}else{
			reloadable = true
		}
		if (ehcacheConfig.ehcacheXmlLocation instanceof CharSequence) {
			// use the specified location
			ehcacheConfigLocation = ehcacheConfig.ehcacheXmlLocation
			log.info "Using Ehcache configuration file $ehcacheConfigLocation"
		}
		else if (cacheConfig.config instanceof Closure || application.cacheConfigClasses) {
			// leave the location null to indicate that the real configuration will
			// happen in doWithApplicationContext (from the core plugin, using this
			// plugin's grailsCacheConfigLoader)
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
				ehcacheConfigLocation = defaults[1] // won't work but will fail more helpfully
			}
		}

		ehcacheCacheManager(GrailsEhCacheManagerFactoryBean) {
			configLocation = ehcacheConfigLocation
			rebuildable = reloadable
		}

		grailsCacheConfigLoader(EhcacheConfigLoader) {
			rebuildable = reloadable
		}

		grailsCacheManager(GrailsEhcacheCacheManager) {
			cacheManager = ref('ehcacheCacheManager')
		}

		grailsCacheFilter(EhcachePageFragmentCachingFilter) {
			cacheManager = ref('grailsCacheManager')
			nativeCacheManager = ref('ehcacheCacheManager')
			cacheOperationSource = ref('cacheOperationSource')
			keyGenerator = ref('webCacheKeyGenerator')
			expressionEvaluator = ref('webExpressionEvaluator')
		}

		grailsCacheMbeanServer(MBeanServerFactoryBean) {
			locateExistingServerIfPossible = true
		}

		ehCacheManagementService(ManagementService) { bean ->
			bean.initMethod = 'init'
			bean.destroyMethod = 'dispose'
			bean.constructorArgs = [ehcacheCacheManager, grailsCacheMbeanServer, true, true, true, true, true]
		}
	}

	private boolean isEnabled(GrailsApplication application) {
		def enabled = application.config.grails.cache.enabled
		(enabled instanceof Boolean) ? enabled : true
	}
}
