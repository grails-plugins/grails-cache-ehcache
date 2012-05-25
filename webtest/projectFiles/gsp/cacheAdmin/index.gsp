<html>
<head>
<title>Cache Admin</title>
</head>

<body>

<table border='1'>
<caption>Caches</caption>
<thead>
<tr>
	<th>Name</th>
	<th>Impl Class</th>
	<th>Size</th>
</tr>
</thead>

<tbody>
<g:each in="${caches}" var="entry">
<tr>
	<td><g:link action='cache' params="[name: entry.key]">${entry.key}</g:link></td>
	<td>${entry.value.getClass().getName()}</td>
	<td>${entry.value.allKeys.size()}</td>
</tr>
</g:each>
</tbody>

</table>

</body>

</html>
