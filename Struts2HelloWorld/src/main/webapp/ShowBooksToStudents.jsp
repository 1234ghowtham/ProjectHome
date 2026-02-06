<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" errorPage="error.jsp"%>

<%@ page import="JspHelper.ShowBooksToStudents"%>
<%@ page import="java.util.*"%>

<!DOCTYPE html>
<head>
<meta charset="UTF-8">
<title>Show Books For Student</title>
<style>
body {
	font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
	background-color: #f0f2f5;
	padding: 30px;
	margin: 0;
}

.search-form {
	display: flex;
	justify-content: center;
	margin-bottom: 30px;
}

.search-form input[type="text"] {
	padding: 10px 15px;
	border: 1px solid #ccc;
	border-radius: 4px 0 0 4px;
	width: 300px;
	font-size: 16px;
	outline: none;
}

.search-form button {
	padding: 10px 15px;
	border: none;
	background-color: #007bff;
	color: white;
	border-radius: 0 4px 4px 0;
	font-size: 18px;
	cursor: pointer;
	transition: background-color 0.3s ease;
}

.search-form button:hover {
	background-color: #0056b3;
}

table {
	width: 100%;
	border-collapse: collapse;
	margin-top: 20px;
	background-color: #ffffff;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

th, td {
	padding: 14px 16px;
	border: 1px solid #dee2e6;
	text-align: center;
}

th {
	background-color: #343a40;
	color: white;
	text-transform: uppercase;
	font-weight: bold;
	letter-spacing: 0.5px;
}

tr:nth-child(even) {
	background-color: #f8f9fa;
}

tr:hover {
	background-color: #f1f3f5;
}

td[colspan="5"] {
	font-style: italic;
	color: #888;
	text-align: center;
	padding: 20px;
}

input[type="button"] {
	background-color: #28a745;
	color: white;
	border: none;
	padding: 10px 20px;
	margin-top: 25px;
	margin-right: 10px;
	font-size: 14px;
	border-radius: 5px;
	cursor: pointer;
	transition: background-color 0.3s;
}

input[type="button"]:hover {
	background-color: #218838;
}
</style>
</head>

<body>
	<form action="SearchBooksToStudents.jsp" method="post"
		class="search-form">
		<input type="text" name="bookTitle" placeholder="Search book title..." />
		<button type="submit" title="Search">🔍</button>
	</form>
	
	<%
	ShowBooksToStudents show = new ShowBooksToStudents();
	List<Map<String, Object>> recordsList = show.getAllBooks();
	%>
	
	<table>

		<tr>
			<th>Book ID</th>
			<th>Book Title</th>
			<th>Available Copies</th>
			<th>Total Copies</th>
		</tr>

		<%
		if (recordsList.isEmpty()) {
		%>

		<tr>
			<td colspan="6">No book records found.</td>
		</tr>

		<%
		} else {
		for (Map<String, Object> record : recordsList) {
		%>
		
		<tr>
			<td><%=record.get("bookId")%></td>
			<td><%=record.get("bookTitle")%></td>
			<td><%=record.get("availableCopies")%></td>
			<td><%=record.get("totalCopies")%></td>
		</tr>
		
		<%
		}
		}
		%>

	</table>
	<input type="button" value="Back"
		onclick="window.location.href='Success1.jsp'" />
	<input type="button" value="Home"
		onclick="window.location.href='index.jsp'" />
</body>
</html>