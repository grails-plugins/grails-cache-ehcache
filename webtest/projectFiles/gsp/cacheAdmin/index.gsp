<html>
<head>
<meta name="layout" content="main">
<title>Cache Admin</title>
</head>

<body>

<g:if test="${flash.message}">
<div class="message" role="status">${flash.message}</div>
</g:if>

<table border='1'>
<caption>Caches</caption>
<thead>
<tr>
	<th>Name</th>
	<th>Impl Class</th>
	<th>Size</th>
	<th>Clear</th>
</tr>
</thead>

<tbody>
<g:each in="${caches}" var="entry">
<tr>
	<td><g:link action='cache' params="[name: entry.key]">${entry.key}</g:link></td>
	<td>${entry.value.getClass().getName()}</td>
	<td>${entry.value.allKeys.size()}</td>
	<td><g:link action='clearCache' params="[cacheName: entry.key]">Clear</g:link></td>
</tr>
</g:each>
</tbody>

</table>

</body>

</html>
