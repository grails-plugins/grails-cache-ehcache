package grails.plugin.cache.ehcache.hibernate;

import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cfg.Settings;

/**
 * Use the existing ehCache CacheManager instance instead of creating a new instance. For use with Hibernate 4.0+.
 * Configure in DataSource.groovy: hibernate.cache.region.factory_class = 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory4
 *
 * Note that most of this code is copied from {@link org.hibernate.cache.DelegatingRegionFactory}, which cannot be extended in this case as its constructor is package-scoped (and this class is not in the same package).
 * 
 * @author Craig Andrews
 */
public class BeanEhCacheRegionFactory4 implements RegionFactory {

    private final RegionFactory regionFactory;

    public BeanEhCacheRegionFactory4(final Properties properties) {
        this.regionFactory = new BeanEhcacheRegionFactory(properties);
    }

    public final void start(final Settings settings, final Properties properties) throws CacheException {
        regionFactory.start(settings, properties);
    }

    public final void stop() {
        regionFactory.stop();
    }

    public final boolean isMinimalPutsEnabledByDefault() {
        return regionFactory.isMinimalPutsEnabledByDefault();
    }

    public final AccessType getDefaultAccessType() {
        return regionFactory.getDefaultAccessType();
    }

    public final long nextTimestamp() {
        return regionFactory.nextTimestamp();
    }

    public final EntityRegion buildEntityRegion(final String regionName, final Properties properties,
                                                final CacheDataDescription metadata) throws CacheException {
        return regionFactory.buildEntityRegion(regionName, properties, metadata);
    }

    public final CollectionRegion buildCollectionRegion(final String regionName, final Properties properties,
                                                        final CacheDataDescription metadata) throws CacheException {
        return regionFactory.buildCollectionRegion(regionName, properties, metadata);
    }

    public final QueryResultsRegion buildQueryResultsRegion(final String regionName, final Properties properties) throws CacheException {
        return regionFactory.buildQueryResultsRegion(regionName, properties);
    }

    public final TimestampsRegion buildTimestampsRegion(final String regionName, final Properties properties) throws CacheException {
        return regionFactory.buildTimestampsRegion(regionName, properties);
    }

}
