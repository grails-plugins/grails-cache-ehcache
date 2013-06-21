package grails.plugin.cache.ehcache

import grails.test.GrailsUnitTestCase
import grails.test.mixin.TestFor
import groovyx.gpars.GParsPool
import net.sf.ehcache.CacheManager
import org.junit.Before

/**
 * @author Andrew Walters
 */
class EhcacheCacheManagerTests extends GrailsUnitTestCase {

    protected GrailsEhcacheCacheManager manager

    @Before
    void before() {
        manager = new GrailsEhcacheCacheManager()
        manager.cacheManager = CacheManager.create()
    }

    void "test cache creation serial access"() {
        (0..10).each {
            manager.getCache("testCache")
        }
    }

    /**
     * As a parallel access isn't guaranteed to result in traversing the code at the same time, loop a number
     * of times to try and cause the simultaneous access to getCache()
     */
    void "test cache creation parallel access"() {
        assertFalse(manager.cacheExists("testCache"))

        10.times {
            println "Trying to get a failure x ${it + 1}"

            GParsPool.withPool {
                (0..4).everyParallel {
                    manager.getCache("testCache") != null
                }

                manager.destroyCache("testCache")
            }
        }
    }

    void "test cache get parallel access"() {
        manager.getCache("testCache")

        assertTrue(manager.cacheExists("testCache"))

        10.times {
            println "Trying to get a failure x ${it + 1}"

                GParsPool.withPool {
                (0..10).eachParallel {
                    assertNotNull(manager.getCache("testCache"))
                }
            }
        }
    }
}
