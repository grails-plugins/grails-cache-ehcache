package grails.plugin.cache.ehcache.hibernate;

import grails.util.Holders;

import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.EhCacheRegionFactory;

import org.hibernate.cfg.Settings;

/**
 * Use the existing ehCache CacheManager instance instead of creating a new instance.
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
