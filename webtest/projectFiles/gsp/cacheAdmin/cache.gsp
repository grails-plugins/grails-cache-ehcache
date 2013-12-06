<html>
<head>
<meta name="layout" content="main">
<title>Cache '${cache.name}'</title>
</head>

<body>

Cache '${cache.name}'<br/>

<table border='1'>
<caption>Cached Items</caption>
<thead>
<tr>
	<th>Key</th>
	<th>Value</th>
	<th>HTML</th>
</tr>
</thead>
<tbody>

<g:each in="${data}" var="row">
<tr>
	<td><g:link action='cacheItem' params="[name: cache.name, key: row.key]">${row.key}</g:link></td>
	<td>${row.value}</td>
	<td>${row.html?.encodeAsHTML() ?: '&nbsp;'}</td>
</tr>
</g:each>

</tbody>
</table>

</body>

</html>
