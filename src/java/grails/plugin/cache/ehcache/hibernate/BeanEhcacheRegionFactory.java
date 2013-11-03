package grails.plugin.cache.ehcache.hibernate;

import grails.util.Holders;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Settings;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.EhCacheRegionFactory;

/** Use the existing ehCache CacheManager instance instead of creating a new instance.
 * Use this in DataSource.groovy like this: hibernate.cache.region.factory_class = 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory
 */
// It would be nicer to extend net.sf.ehcache.hibernate.AbstractEhcacheRegionFactory but it's package scoped, so it's not visible
public class BeanEhcacheRegionFactory extends EhCacheRegionFactory {

	/**
	 * Creates an EhCacheRegionFactory that uses a predefined CacheManager bean
	 */
	public BeanEhcacheRegionFactory(Properties prop) {
		super(prop);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start(Settings settings, Properties properties) throws CacheException {
		this.settings = settings;
		this.manager = Holders.getGrailsApplication().getMainContext().getBean("ehcacheCacheManager", CacheManager.class);
        mbeanRegistrationHelper.registerMBean(manager, properties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
        try {
            if (manager != null) {
                mbeanRegistrationHelper.unregisterMBean();
                manager.shutdown();
                manager = null;
            }
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
	}

}
