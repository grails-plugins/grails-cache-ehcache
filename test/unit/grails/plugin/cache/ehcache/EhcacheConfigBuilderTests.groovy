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

import grails.test.GrailsUnitTestCase
import grails.util.Environment

/**
 * @author Burt Beckwith
 */
class EhcacheConfigBuilderTests extends GrailsUnitTestCase {

	private EhcacheConfigBuilder builder
	private String xml
	private root

	void testTopLevelAttributes() {

		parse {}

		assertAttribute 'false', 'updateCheck'
		assertAttribute 'autodetect', 'monitoring'
		assertAttribute 'true', 'dynamicConfig'
		assertAttribute '15', 'defaultTransactionTimeoutInSeconds'
		assertAttribute '0', 'maxBytesLocalHeap'
		assertAttribute '0', 'maxBytesLocalOffHeap'
		assertAttribute '0', 'maxBytesLocalDisk'

		parse {
			provider {
				updateCheck true
				monitoring 'on'
				dynamicConfig false
				defaultTransactionTimeoutInSeconds 30
				maxBytesLocalHeap 10
				maxBytesLocalOffHeap 20
				maxBytesLocalDisk 30
			}
		}

		assertAttribute 'true', 'updateCheck'
		assertAttribute 'on', 'monitoring'
		assertAttribute 'false', 'dynamicConfig'
		assertAttribute '30', 'defaultTransactionTimeoutInSeconds'
		assertAttribute '10', 'maxBytesLocalHeap'
		assertAttribute '20', 'maxBytesLocalOffHeap'
		assertAttribute '30', 'maxBytesLocalDisk'
	}

	void testDiskStore() {

		parse {
			diskStore {
				temp true
			}
		}

		def diskStores = root.diskStore
		assertEquals 1, diskStores.size()
		assertAttribute 'java.io.tmpdir', 'path', diskStores[0]

		parse {
			diskStore {
				path '/tmp/ehcache'
			}
		}

		diskStores = root.diskStore
		assertEquals 1, diskStores.size()
		assertAttribute '/tmp/ehcache', 'path', diskStores[0]

		parse {
			diskStore {
				home true
			}
		}

		diskStores = root.diskStore
		assertEquals 1, diskStores.size()
		assertAttribute 'user.home', 'path', diskStores[0]

		parse {
			diskStore {
				current true
			}
		}

		diskStores = root.diskStore
		assertEquals 1, diskStores.size()
		assertAttribute 'user.dir', 'path', diskStores[0]
	}

	void testDefaultCache() {

		parse {
			defaultCache {
				maxElementsInMemory 10000
				eternal false
				timeToIdleSeconds 120
				timeToLiveSeconds 120
				overflowToDisk true
				maxElementsOnDisk 10000000
				diskPersistent false
				diskExpiryThreadIntervalSeconds 120
				memoryStoreEvictionPolicy 'LRU'
			}
		}

		def defaultCaches = root.defaultCache
		assertEquals 1, defaultCaches.size()
		def defaultCache = defaultCaches[0]
		assertAttribute '10000', 'maxElementsInMemory', defaultCache
		assertAttribute 'false', 'eternal', defaultCache
		assertAttribute '120', 'timeToIdleSeconds', defaultCache
		assertAttribute '120', 'timeToLiveSeconds', defaultCache
		assertAttribute 'true', 'overflowToDisk', defaultCache
		assertAttribute '10000000', 'maxElementsOnDisk', defaultCache
		assertAttribute 'false', 'diskPersistent', defaultCache
		assertAttribute '120', 'diskExpiryThreadIntervalSeconds', defaultCache
		assertAttribute 'LRU', 'memoryStoreEvictionPolicy', defaultCache
	}

	void testDomain() {

		parse {
			domain {
				name 'com.foo.Thing'
				eternal false
				overflowToDisk true
				maxElementsInMemory 10000
				maxElementsOnDisk 10000000
			}
		}

		def caches = root.cache
		assertEquals 1, caches.size()
		def cache = caches[0]
		assertAttribute 'com.foo.Thing', 'name', cache
		assertAttribute 'false', 'eternal', cache
		assertAttribute 'true', 'overflowToDisk', cache
		assertAttribute '10000', 'maxElementsInMemory', cache
		assertAttribute '10000000', 'maxElementsOnDisk', cache
	}

	void testDomainWithDefaults() {

		parse {
			defaults {
				maxElementsInMemory 1000
				eternal false
				overflowToDisk false
				maxElementsOnDisk 0
			}

			domain {
				name 'com.foo.Thing'
				maxElementsOnDisk 10000000
			}
		}

		def caches = root.cache
		assertEquals 1, caches.size()
		def cache = caches[0]
		assertAttribute 'com.foo.Thing', 'name', cache
		assertAttribute 'false', 'eternal', cache
		assertAttribute 'false', 'overflowToDisk', cache
		assertAttribute '1000', 'maxElementsInMemory', cache
		assertAttribute '10000000', 'maxElementsOnDisk', cache
	}

	void testCollection() {

		parse {
			domain {
				name 'com.foo.Author'
				eternal false
				overflowToDisk true
				maxElementsInMemory 10000
				maxElementsOnDisk 10000000
			}
			domainCollection {
				name 'books'
				domain 'com.foo.Author'
				eternal true
				overflowToDisk true
				maxElementsInMemory 100
				maxElementsOnDisk 10000
			}
		}

		def caches = root.cache
		assertEquals 2, caches.size()

		def domain = caches[0]
		assertAttribute 'com.foo.Author', 'name', domain
		assertAttribute 'false', 'eternal', domain
		assertAttribute 'true', 'overflowToDisk', domain
		assertAttribute '10000', 'maxElementsInMemory', domain
		assertAttribute '10000000', 'maxElementsOnDisk', domain

		def collection = caches[1]
		assertAttribute 'com.foo.Author.books', 'name', collection
		assertAttribute 'true', 'eternal', collection
		assertAttribute 'true', 'overflowToDisk', collection
		assertAttribute '100', 'maxElementsInMemory', collection
		assertAttribute '10000', 'maxElementsOnDisk', collection
	}

	void testInnerCollection() {

		parse {
			defaults {
				maxElementsInMemory 1000
				eternal false
				overflowToDisk false
				maxElementsOnDisk 0
			}

			domain {
				name 'com.foo.Author'
				maxElementsInMemory 10000

				collection {
					name 'books'
					eternal true
					maxElementsInMemory 100
				}
			}
		}

		def caches = root.cache
		assertEquals 2, caches.size()

		def domain = caches[0]
		assertAttribute 'com.foo.Author', 'name', domain
		assertAttribute 'false', 'eternal', domain
		assertAttribute 'false', 'overflowToDisk', domain
		assertAttribute '10000', 'maxElementsInMemory', domain
		assertAttribute '0', 'maxElementsOnDisk', domain

		def collection = caches[1]
		assertAttribute 'com.foo.Author.books', 'name', collection
		assertAttribute 'true', 'eternal', collection
		assertAttribute 'false', 'overflowToDisk', collection
		assertAttribute '100', 'maxElementsInMemory', collection
		assertAttribute '0', 'maxElementsOnDisk', collection
	}

	void testHibernate() {

		parse {
			hibernateQuery()
			hibernateTimestamps()
		}

		def caches = root.cache
		assertEquals 2, caches.size()

		def queryCache = caches[0]
		assertAttribute 'org.hibernate.cache.StandardQueryCache', 'name', queryCache
		assertAttribute 'false', 'eternal', queryCache
		assertAttribute 'true', 'overflowToDisk', queryCache
		assertAttribute '50', 'maxElementsInMemory', queryCache
		assertAttribute '0', 'maxElementsOnDisk', queryCache
		assertAttribute '120', 'timeToLiveSeconds', queryCache

		def updateTimestamps = caches[1]
		assertAttribute 'org.hibernate.cache.UpdateTimestampsCache', 'name', updateTimestamps
		assertAttribute 'true', 'eternal', updateTimestamps
		assertAttribute 'false', 'overflowToDisk', updateTimestamps
		assertAttribute '5000', 'maxElementsInMemory', updateTimestamps
		assertAttribute '0', 'maxElementsOnDisk', updateTimestamps
	}

	void testEnvironment() {

		def config = {
			defaults {
				maxElementsInMemory 1000
				eternal false
				overflowToDisk false
				maxElementsOnDisk 0
			}

			domain {
				name 'com.foo.Thing'
			}

			domain {
				name 'com.foo.Other'
				env 'staging'
			}

			domain {
				name 'com.foo.Book'
				env(['staging', 'production'])
			}
		}

		parse config

		def caches = root.cache
		assertEquals 1, caches.size()
		assertAttribute 'com.foo.Thing', 'name', caches[0]

		Environment.metaClass.getName = { -> 'staging' }

		parse config

		caches = root.cache
		assertEquals 3, caches.size()
		assertAttribute 'com.foo.Thing', 'name', caches[0]
		assertAttribute 'com.foo.Other', 'name', caches[1]
		assertAttribute 'com.foo.Book', 'name', caches[2]

		Environment.metaClass.getName = { -> 'production' }

		parse config

		caches = root.cache
		assertEquals 2, caches.size()
		assertAttribute 'com.foo.Thing', 'name', caches[0]
		assertAttribute 'com.foo.Book', 'name', caches[1]
	}

	void testDistributed() {

		Environment.metaClass.getName = { -> 'production' }

		parse {

			defaults {
				maxElementsInMemory 1000
				eternal false
				overflowToDisk false
				maxElementsOnDisk 0
				cacheEventListenerFactoryName 'cacheEventListenerFactory'
			}

			domain {
				name 'com.foo.Book'
			}

			cacheManagerPeerProviderFactory {
				env 'production'
				factoryType 'rmi'
				multicastGroupAddress '${ehcacheMulticastGroupAddress}'// '237.0.0.2'
				multicastGroupPort '${ehcacheMulticastGroupPort}'// 5557
				timeToLive 'subnet'
			}

			cacheManagerPeerListenerFactory {
				env 'production'
			}

			cacheEventListenerFactory {
				env 'production'
				name 'cacheEventListenerFactory'
				factoryType 'rmi'
				replicateAsynchronously false
			}
		}

		def cacheManagerPeerProviderFactoryNodes = root.cacheManagerPeerProviderFactory
		assertEquals 1, cacheManagerPeerProviderFactoryNodes.size()
		def factory = cacheManagerPeerProviderFactoryNodes[0]

		assertAttribute 'net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory', 'class', factory
		assertAttribute 'multicastGroupAddress=${ehcacheMulticastGroupAddress},multicastGroupPort=${ehcacheMulticastGroupPort},timeToLive=1,peerDiscovery=automatic', 'properties', factory
		assertAttribute ',', 'propertySeparator', factory

		def cacheManagerPeerListenerFactoryNodes = root.cacheManagerPeerListenerFactory
		assertEquals 1, cacheManagerPeerListenerFactoryNodes.size()
		factory = cacheManagerPeerListenerFactoryNodes[0]

		assertAttribute 'net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory', 'class', factory

		def caches = root.cache
		assertEquals 1, caches.size()
		assertAttribute 'com.foo.Book', 'name', caches[0]
		assertAttribute '1000', 'maxElementsInMemory', caches[0]
		assertAttribute 'false', 'eternal', caches[0]
		assertAttribute 'false', 'overflowToDisk', caches[0]
		assertAttribute '0', 'maxElementsOnDisk', caches[0]

		def cacheEventListenerFactoryNodes = caches[0].cacheEventListenerFactory
		assertEquals 1, cacheEventListenerFactoryNodes.size()
		factory = cacheEventListenerFactoryNodes[0]

		assertAttribute 'net.sf.ehcache.distribution.RMICacheReplicatorFactory', 'class', factory
		assertAttribute 'replicateAsynchronously=false', 'properties', factory
		assertAttribute ',', 'propertySeparator', factory
	}

	private void assertAttribute(String expected, String name, node = root) {
		assertEquals expected, node."@$name".text()
	}

	private void parse(Closure config) {
		builder = new EhcacheConfigBuilder()
		config.delegate = builder
		config()
		xml = builder.toXml()
		root = new XmlSlurper().parseText(xml)
	}
}
