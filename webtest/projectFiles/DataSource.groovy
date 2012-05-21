dataSource {
	pooled = true
	driverClassName = 'org.h2.Driver'
	dbCreate = 'create-drop'
	url = 'jdbc:h2:mem:devDb;MVCC=TRUE'
	username = 'sa'
	password = ''
	logSql = true
}
hibernate {
	cache.use_second_level_cache = true
	cache.use_query_cache = false
	cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
	format_sql = true
	use_sql_comments = true
}
