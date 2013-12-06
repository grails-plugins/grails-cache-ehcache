<html>
<head>
<meta name="layout" content="main">
<title>Cache '${name}'</title>
</head>

<body>

Cache '${name}'<br/>
Key '${key}'<br/>

<table border='1'>
<thead>
<tr>
	<th>Value</th>
	<th>HTML</th>
</tr>
</thead>
<tbody>

<tr>
	<td>${value}</td>
	<td>${html?.encodeAsHTML() ?: ''}</td>
</tr>

</tbody>
</table>

</body>

</html>
