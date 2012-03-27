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

import grails.util.Environment

import org.slf4j.Logger
import org.slf4j.LoggerFactory

// note defaults doesn't apply to hibernateQuery/hibernateTimestamps

/**
 * @author Burt Beckwith
 */
class EhcacheConfigBuilder extends BuilderSupport {

	private static final String INDENT = '\t'
	private static final String LF = System.getProperty('line.separator')

	private static final Map<String, Integer> TTL = [
		host: 0,
		subnet: 1,
		site: 32,
		region: 64,
		continent: 128,
		unrestricted: 255]

	private static final Map<String, String> CACHE_MANAGER_PEER_PROVIDERS =
		[rmi: 'net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory',
		 jgroups: 'net.sf.ehcache.distribution.jgroups.JGroupsCacheManagerPeerProviderFactory',
		 jms: 'net.sf.ehcache.distribution.jms.JMSCacheManagerPeerProviderFactory']

	private static final Map<String, String> CACHE_EVENT_LISTENER_FACTORIES =
		[rmi: 'net.sf.ehcache.distribution.RMICacheReplicatorFactory',
		 jgroups: 'net.sf.ehcache.distribution.jgroups.JGroupsCacheReplicatorFactory',
		 jms: 'net.sf.ehcache.distribution.jms.JMSCacheReplicatorFactory']

	private static final Map<String, String> BOOTSTRAP_CACHE_LOADER_FACTORIES =
		[rmi: 'net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory',
		 jgroups: 'net.sf.ehcache.distribution.jgroups.JGroupsBootstrapCacheLoaderFactory'] // no JMS

	private List<String> _stack = []
	private List<Map<String, Object>> _caches = []
	private Map<String, Object> _defaultCache = [:]
	private Map<String, Object> _defaults = [:]
	private Map<String, Object> _hibernateQuery = [
		name: 'org.hibernate.cache.StandardQueryCache', maxElementsInMemory: 50,
		timeToLiveSeconds: 120, eternal: false, overflowToDisk: true, maxElementsOnDisk: 0]
	private Map<String, Object> _hibernateTimestamps = [
		name: 'org.hibernate.cache.UpdateTimestampsCache', maxElementsInMemory: 5000,
		eternal: true, overflowToDisk: false, maxElementsOnDisk: 0]
	private Map<String, Object> _cacheManagerPeerListenerFactory // can be empty, so exists == not null
	private Map<String, Object> _cacheManagerEventListenerFactory = [:]
	private Map<String, Object> _provider = [:]
	private List<Map<String, Object>> _cacheEventListenerFactories = []
	private List<Map<String, Object>> _bootstrapCacheLoaderFactories = []
	private List<Map<String, Object>> _cacheExceptionHandlerFactories = []
	private List<Map<String, Object>> _cacheLoaderFactories = []
	private List<Map<String, Object>> _cacheExtensionFactories = []
	private Map<String, Object> _current
	private String _diskStore = TEMP_DIR
	private List<Map<String, Object>> _cacheManagerPeerProviderFactories = []

	private final Logger _log = LoggerFactory.getLogger(getClass())

	private static final List DEFAULT_CACHE_PARAM_NAMES = [
		'cacheLoaderTimeoutMillis', 'clearOnFlush', 'copyOnRead', 'copyOnWrite',
		'diskAccessStripes', 'diskExpiryThreadIntervalSeconds', 'diskSpoolBufferSizeMB',
		'diskPersistent', 'eternal', 'maxElementsInMemory', 'maxElementsOnDisk',
		'maxEntriesLocalDisk', 'maxEntriesLocalHeap', 'maxMemoryOffHeap',
		'memoryStoreEvictionPolicy', 'overflowToDisk', 'overflowToOffHeap',
		'statistics', 'timeToIdleSeconds', 'timeToLiveSeconds', 'transactionalMode']

	private static final List CACHE_PARAM_NAMES = DEFAULT_CACHE_PARAM_NAMES + [
		'env', 'logging', 'maxBytesLocalDisk', 'maxBytesLocalHeap', 'maxBytesLocalOffHeap', 'name']

	private static final List RMI_CACHE_MANAGER_PARAM_NAMES = [
		'env', 'factoryType', 'multicastGroupAddress', 'multicastGroupPort',
		'timeToLive', 'className', 'rmiUrl', 'hostName']

	private static final List JGROUPS_CACHE_MANAGER_PARAM_NAMES = ['env', 'connect']

	private static final List JMS_CACHE_MANAGER_PARAM_NAMES = [
		'env', 'initialContextFactoryName', 'providerURL', 'topicConnectionFactoryBindingName',
		'topicBindingName', 'getQueueBindingName', 'securityPrincipalName', 'securityCredentials',
		'urlPkgPrefixes', 'userName', 'password', 'acknowledgementMode']

	private static final List FACTORY_REF_NAMES = [
		'cacheEventListenerFactoryName', 'bootstrapCacheLoaderFactoryName', 'cacheExceptionHandlerFactoryName',
		'cacheLoaderFactoryName', 'cacheExtensionFactoryName']

	private static final List PROVIDER_NAMES = [
		'updateCheck', 'monitoring', 'dynamicConfig', 'name', 'defaultTransactionTimeoutInSeconds',
		'maxBytesLocalHeap', 'maxBytesLocalOffHeap', 'maxBytesLocalDisk']

	private static final String TEMP_DIR = 'java.io.tmpdir'

	@Override
	protected createNode(name) {
		_log.trace "createNode $name"

		switch (name) {
			case 'provider':
			case 'diskStore':
			case 'defaultCache':
			case 'defaults':
			case 'cacheManagerEventListenerFactory':
				_stack.push name
				return name

			case 'cacheManagerPeerListenerFactory':
				_cacheManagerPeerListenerFactory = [:]
				_stack.push name
				return name

			case 'hibernateQuery':
				_stack.push name
				_caches << _hibernateQuery
				return name

			case 'hibernateTimestamps':
				_stack.push name
				_caches << _hibernateTimestamps
				return name

			case 'domainCollection':
			case 'collection':
			case 'domain':
				_current = [:]
				_caches << _current
				_stack.push name
				return name

			case 'cacheManagerPeerProviderFactory':
				_current = [:]
				_cacheManagerPeerProviderFactories << _current
				_stack.push name
				return name

			case 'cacheEventListenerFactory':
				_current = [:]
				_cacheEventListenerFactories << _current
				_stack.push name
				return name

			case 'bootstrapCacheLoaderFactory':
				_current = [:]
				_bootstrapCacheLoaderFactories << _current
				_stack.push name
				return name

			case 'cacheExceptionHandlerFactory':
				_current = [:]
				_cacheExceptionHandlerFactories << _current
				_stack.push name
				return name

			case 'cacheLoaderFactory':
				_current = [:]
				_cacheLoaderFactories << _current
				_stack.push name
				return name

			case 'cacheExtensionFactory':
				_current = [:]
				_cacheExtensionFactories << _current
				_stack.push name
				return name
		}

		throw new IllegalArgumentException("Cannot create empty node with name '$name'")
	}

	@Override
	protected createNode(name, value) {
		_log.trace "createNode $name, value: $value"

		String level = _stack[-1]
		_stack.push name

		switch (level) {
			case 'diskStore':
				switch (name) {
					case 'path':
						_diskStore = value.toString()
						return name

					case 'temp':
						_diskStore = TEMP_DIR
						return name

					case 'home':
						_diskStore = 'user.home'
						return name

					case 'current':
						_diskStore = 'user.dir'
						return name
				}
				break

			case 'defaultCache':
				if (name in DEFAULT_CACHE_PARAM_NAMES) {
					_defaultCache[name] = value
					return name
				}
				break

			case 'defaults':
				if (name in FACTORY_REF_NAMES) {
					addToList _defaults, name, value
					return name
				}

				if (name in CACHE_PARAM_NAMES) {
					_defaults[name] = value
					return name
				}

				break

			case 'hibernateQuery':
				if (name in FACTORY_REF_NAMES) {
					addToList _hibernateQuery, name, value
					return name
				}

				if (name in CACHE_PARAM_NAMES) {
					_hibernateQuery[name] = value
					return name
				}

				break

			case 'hibernateTimestamps':
				if (name in FACTORY_REF_NAMES) {
					addToList _hibernateTimestamps, name, value
					return name
				}

				if (name in CACHE_PARAM_NAMES) {
					_hibernateTimestamps[name] = value
					return name
				}
				break

			case 'domain':
			case 'domainCollection':
				if (('name' == name || 'domain' == name) && value instanceof Class) {
					value = value.name
				}

				if ('name' == name || 'domain' == name || name in CACHE_PARAM_NAMES) {
					_current[name] = value
					return name
				}

				if (name in FACTORY_REF_NAMES) {
					addToList _current, name, value
					return name
				}

				break

			case 'collection':
				if ('name' == name) {
					if (value instanceof Class) {
						value = value.name
					}

					_current.domain = _caches[-2].name
				}

				if ('name' == name || name in CACHE_PARAM_NAMES) {
					_current[name] = value
					return name
				}

				break

			case 'cacheManagerPeerProviderFactory':
				if ('rmiUrl' == name) {
					addToList _current, 'rmiUrls', value
					return name
				}

				// allow all properties for forward compatability
				_current[name] = value
				return name

			case 'cacheManagerPeerListenerFactory':
				// allow all properties for forward compatability
				_cacheManagerPeerListenerFactory[name] = value
				return name

			case 'cacheManagerEventListenerFactory':
				// allow all properties for forward compatability
				_cacheManagerEventListenerFactory[name] = value
				return name

			case 'provider':
				if (name in PROVIDER_NAMES) {
					_provider[name] = value
					return name
				}
				break

			case 'cacheEventListenerFactory':
			case 'bootstrapCacheLoaderFactory':
			case 'cacheExceptionHandlerFactory':
			case 'cacheLoaderFactory':
			case 'cacheExtensionFactory':
				// allow all properties for forward compatability
				_current[name] = value
				return name
		}

		throw new IllegalArgumentException("Cannot create node with name '$name' and value '$value' for parent '$level'")
	}

	@Override
	protected createNode(name, Map attributes) {
		_log.trace "createNode $name + attributes: $attributes"
	}

	@Override
	protected createNode(name, Map attributes, value) {
		_log.trace "createNode $name + value: $value attributes: $attributes"
		throw new UnsupportedOperationException()
	}

	@Override
	protected void setParent(parent, child) {
		_log.trace "setParent $parent, child: $child"
		// do nothing
	}

	@Override
	protected void nodeCompleted(parent, node) {
		_log.trace "nodeCompleted $parent $node"
		_stack.pop()
	}

	String toXml() {

		String env = Environment.current.name

		Map cacheEventListenerFactoriesXml = generateChildElementXmlMap(_cacheEventListenerFactories,
				env, CACHE_EVENT_LISTENER_FACTORIES, 'cacheEventListenerFactory')

		Map bootstrapCacheLoaderFactoriesXml = generateChildElementXmlMap(_bootstrapCacheLoaderFactories,
				env, BOOTSTRAP_CACHE_LOADER_FACTORIES, 'bootstrapCacheLoaderFactory')

		Map cacheExceptionHandlerFactoriesXml = generateChildElementXmlMap(_cacheExceptionHandlerFactories,
						env, [:], 'cacheExceptionHandlerFactory')

		Map cacheLoaderFactoriesXml = generateChildElementXmlMap(_cacheLoaderFactories,
				env, [:], 'cacheLoaderFactory')

		Map cacheExtensionFactoriesXml = generateChildElementXmlMap(_cacheExtensionFactories,
				env, [:], 'cacheExtensionFactory')

		StringBuilder xml = new StringBuilder()
		xml.append '<ehcache xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="ehcache.xsd"'

		appendProperty xml, 'defaultTransactionTimeoutInSeconds', getValue(_provider, 'defaultTransactionTimeoutInSeconds', 15), ' '
		appendProperty xml, 'dynamicConfig', getValue(_provider, 'dynamicConfig', true), ' '
		appendProperty xml, 'maxBytesLocalDisk', getValue(_provider, 'maxBytesLocalDisk', 0), ' '
		appendProperty xml, 'maxBytesLocalHeap', getValue(_provider, 'maxBytesLocalHeap', 0), ' '
		appendProperty xml, 'maxBytesLocalOffHeap', getValue(_provider, 'maxBytesLocalOffHeap', 0), ' '
		appendProperty xml, 'monitoring', getValue(_provider, 'monitoring', 'autodetect'), ' '
		appendProperty xml, 'updateCheck', getValue(_provider, 'updateCheck', false), ' '
		if (_provider.name) {
			appendProperty xml, 'name', _provider.name
		}

		xml.append('>').append LF

		if (_diskStore) {
			xml.append """$LF$INDENT<diskStore path="$_diskStore" />$LF"""
		}

		if (_defaultCache) {
			appendCache xml, 'defaultCache', _defaultCache, env, cacheEventListenerFactoriesXml,
				bootstrapCacheLoaderFactoriesXml, cacheExceptionHandlerFactoriesXml,
				cacheLoaderFactoriesXml, cacheExtensionFactoriesXml
		}

		for (data in _cacheManagerPeerProviderFactories) {
			appendCacheManagerPeerProviderFactory xml, data, env
		}

		appendCacheManagerPeerListenerFactory xml, env

		appendCacheManagerEventListenerFactory xml, env

		for (data in _caches) {
			Map<String, Object> cache = [:]
			cache.putAll _defaults
			cache.putAll data
			appendCache xml, 'cache', cache, env, cacheEventListenerFactoriesXml,
				bootstrapCacheLoaderFactoriesXml, cacheExceptionHandlerFactoriesXml,
				cacheLoaderFactoriesXml, cacheExtensionFactoriesXml
		}

		xml.append(LF).append('</ehcache>').append LF

		xml.toString()
	}

	private void appendCache(StringBuilder xml, String type, Map data, String env,
			Map cacheEventListenerFactoriesXml, Map bootstrapCacheLoaderFactoriesXml,
			Map cacheExceptionHandlerFactoriesXml, Map cacheLoaderFactoriesXml,
			Map cacheExtensionFactoriesXml) {

		if (data.domain) {
			// collection
			data.name = "${data.domain}.$data.name"
			data.remove 'domain'
		}

		if (!isValidInEnv(data, env)) {
			_log.debug "skipping cache $data.name since it's not valid in env '$env'"
			return
		}

		xml.append "$LF$INDENT<$type "

		String name = data.remove('name')
		if (name) {
			xml.append('name="').append(name).append('"')
		}

		List cacheEventListenerFactoryNames = data.remove('cacheEventListenerFactoryName')
		List bootstrapCacheLoaderFactoryNames = data.remove('bootstrapCacheLoaderFactoryName')
		List cacheExceptionHandlerFactoryNames = data.remove('cacheExceptionHandlerFactoryName')
		List cacheLoaderFactoryNames = data.remove('cacheLoaderFactoryName')
		List cacheExtensionFactoryNames = data.remove('cacheExtensionFactoryName')

		data.each { key, value ->
			xml.append LF
			xml.append INDENT
			appendProperty xml, key, value, '       ', true
		}

		StringBuilder children = new StringBuilder()
		appendFactoryXmls children, cacheEventListenerFactoryNames, cacheEventListenerFactoriesXml
		appendFactoryXmls children, bootstrapCacheLoaderFactoryNames, bootstrapCacheLoaderFactoriesXml
		appendFactoryXmls children, cacheExceptionHandlerFactoryNames, cacheExceptionHandlerFactoriesXml
		appendFactoryXmls children, cacheLoaderFactoryNames, cacheLoaderFactoriesXml
		appendFactoryXmls children, cacheExtensionFactoryNames, cacheExtensionFactoriesXml

		if (children) {
			xml.append '>'
			xml.append LF
			xml.append children
			xml.append(INDENT).append '</cache>'
		}
		else {
			xml.append(LF).append(INDENT).append '/>'
		}

		xml.append LF
	}

	private void appendCacheManagerPeerProviderFactory(StringBuilder xml, Map data, String env) {

		String type = data.remove('factoryType')
		String className = data.remove('className')
		if (!className) {
			className = CACHE_MANAGER_PEER_PROVIDERS[type]
		}

		if (!isValidInEnv(data, env)) {
			_log.debug "skipping cacheManagerPeerProviderFactory $className since it's not valid in env '$env'"
			return
		}

		String timeToLive = data.remove('timeToLive')
		if (timeToLive) {
			data.timeToLive = TTL[timeToLive] // replace string with number
		}

		switch (type) {
			case 'rmi':
				def rmiUrls = data.remove('rmiUrls')
				if (rmiUrls) {
					data.peerDiscovery = 'manual'
					data.rmiUrls = rmiUrls.join('|')
				}
				else {
					data.peerDiscovery = 'automatic'
				}

				appendCacheManagerPeerProviderFactoryNode xml, data, ',', className

				break

			case 'jgroups':
				appendCacheManagerPeerProviderFactoryNode xml, data, '::', className
				break

			case 'jms':
				appendCacheManagerPeerProviderFactoryNode xml, data, ',', className
				break

			default: throw new RuntimeException("Unknown cache manager type $type")
		}
	}

	private void appendCacheManagerPeerProviderFactoryNode(StringBuilder xml, Map data,
			String delimiter, String className) {

		String properties = joinProperties(data, delimiter)
		appendSimpleNodeWithProperties xml, 'cacheManagerPeerProviderFactory', className, properties, delimiter
	}

	private void appendCacheManagerPeerListenerFactory(StringBuilder xml, String env) {
		if (_cacheManagerPeerListenerFactory == null) {
			return
		}

		if (!isValidInEnv(_cacheManagerPeerListenerFactory, env)) {
			_log.debug "skipping cacheManagerPeerListenerFactory since it's not valid in env '$env'"
			return
		}

		String className = _cacheManagerPeerListenerFactory.remove('className') ?:
			'net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory'
		String properties = joinProperties(_cacheManagerPeerListenerFactory, ',')

		appendSimpleNodeWithProperties xml, 'cacheManagerPeerListenerFactory', className, properties, ','
	}

	private void appendCacheManagerEventListenerFactory(StringBuilder xml, String env) {
		if (!_cacheManagerEventListenerFactory) {
			return
		}

		if (!isValidInEnv(_cacheManagerEventListenerFactory, env)) {
			_log.debug "skipping cacheManagerEventListenerFactory since it's not valid in env '$env'"
			return
		}

		String className = _cacheManagerEventListenerFactory.remove('className')
		String properties = joinProperties(_cacheManagerEventListenerFactory, ',')

		appendSimpleNodeWithProperties xml, 'cacheManagerEventListenerFactory', className, properties, ','
	}

	private String generateChildElementXml(Map data, Map classNames, String nodeName) {

		def xml = new StringBuilder()

		String type = data.remove('factoryType')
		String className = data.remove('className')
		if (!className) {
			className = classNames[type]
		}

		String properties = joinProperties(data, ',')

		appendSimpleNodeWithProperties xml, nodeName, className, properties, ',', 2

		xml.toString()
	}

	private String joinProperties(Map data, String delimiter) {

		StringBuilder properties = new StringBuilder()
		String delim = ''
		data.each { key, value ->
			appendProperty properties, key, value, delim, false
			delim = delimiter
		}

		properties.toString()
	}

	private void appendProperty(StringBuilder sb, String name, value, String prefix, boolean quote = true) {
		sb.append(prefix).append(name).append('=')
		if (quote) sb.append('"')
		sb.append value
		if (quote) sb.append('"')
	}

	private boolean isValidInEnv(Map data, String env) {
		def environments = data.remove('env') ?: []
		if (!(environments instanceof List)) {
			environments = [environments]
		}

		environments.isEmpty() || environments.contains(env)
	}

	private void appendSimpleNodeWithProperties(StringBuilder xml, String nodeName, String className,
				String properties, String delimiter, int indentCount = 1) {

		String indent = INDENT.multiply(indentCount)
		xml.append "$LF$indent<$nodeName class='$className'$LF"
		if (properties) {
			xml.append """$indent${INDENT}properties="$properties"$LF"""
			xml.append "$indent${INDENT}propertySeparator='$delimiter'$LF"
		}
		xml.append "$indent/>$LF"
	}

	private void addToList(Map data, String listName, value) {
		List list = data[listName]
		if (!list) {
			list = []
			data[listName] = list
		}

		list << value
	}

	private Map generateChildElementXmlMap(List maps, String env, Map classNames, String nodeName) {
		Map xmls = [:]
		for (data in maps) {
			if (isValidInEnv(data, env)) {
				String name = data.remove('name')
				xmls[name] = generateChildElementXml(data, classNames, nodeName)
			}
		}
		xmls
	}

	private void appendFactoryXmls(StringBuilder xml, List names, Map xmls) {
		for (name in names) {
			String factoryXml = xmls[name]
			if (factoryXml) {
				xml.append factoryXml
			}
		}
	}

	private String getValue(Map m, String name, defaultIfNotSpecified) {
		def value = m[name]
		if (value == null) {
			value = defaultIfNotSpecified
		}
		value
	}
}
