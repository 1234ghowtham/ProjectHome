<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" errorPage="error.jsp"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<%@page import="JspHelper.ShowBooks"%>

<!DOCTYPE html>
<head>
<meta charset="UTF-8">
<title>Show Books</title>
<style>
body {
	font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
	background-color: #f4f6f8;
	margin: 0;
	padding: 20px;
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
}

.search-form button {
	padding: 10px 15px;
	border: none;
	background-color: #007BFF;
	color: white;
	border-radius: 0 4px 4px 0;
	font-size: 18px;
	cursor: pointer;
	transition: background-color 0.3s;
}

.search-form button:hover {
	background-color: #0056b3;
}

table {
	width: 100%;
	border-collapse: collapse;
	margin-top: 10px;
	background-color: #ffffff;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

th, td {
	padding: 12px 15px;
	border: 1px solid #e1e1e1;
	text-align: center;
}

th {
	background-color: #007BFF;
	color: white;
	font-weight: bold;
}

tr:nth-child(even) {
	background-color: #f9f9f9;
}

tr:hover {
	background-color: #f1f1f1;
}

input[type="submit"], input[type="button"] {
	background-color: #28a745;
	color: white;
	border: none;
	padding: 8px 16px;
	border-radius: 4px;
	font-size: 14px;
	cursor: pointer;
	transition: background-color 0.3s ease;
}

input[type="submit"]:hover, input[type="button"]:hover {
	background-color: #218838;
}

input[type="button"]:last-of-type {
	margin-left: 10px;
	background-color: #6c757d;
}

input[type="button"]:last-of-type:hover {
	background-color: #5a6268;
}

form[onsubmit] {
	margin: 0;
}
</style>
</head>

<body>

	<form action="SearchBooks.jsp" method="post" class="search-form">
		<input type="number" name="bookId" placeholder="Search book id..." min="1" required/>
		<button type="submit" title="Search">🔍</button>
	</form>
	
	<%
	ShowBooks show = new ShowBooks();
	List<Map<String, Object>> records = show.getAllBooks();
	%>
	
	<table>
		<tr>
			<th>Book ID</th>
			<th>Book Title</th>
			<th>Available Copies</th>
			<th>Total Copies</th>
		</tr>
		
		<%
		if (records.isEmpty()) {
		%>
		
		<tr>
			<td colspan="6">No book records found.</td>
		</tr>

		<%
		} else {
		for (Map<String, Object> record : records) {
		%>

		<tr>
			<td><%=record.get("bookId")%></td>
			<td><%=record.get("bookTitle")%></td>
			<td><%=record.get("bookAvailableCopies")%></td>
			<td><%=record.get("bookTotalCopies")%></td>
			<td>
				<form action="DeleteBooks" method="post"
					onsubmit="return confirm('Are you sure you want to delete this book?');">
					<input type="hidden" name="bookId" value=<%=record.get("bookId")%> />
					<input type="submit" value="Delete" />
				</form>
			</td>
			
			<%
			}
			}
			%>
			
		</tr>

	</table>
	<input type="button" value="Back"
		onclick="window.location.href='Success.jsp'" />
	<input type="button" value="Home"
		onclick="window.location.href='index.jsp'" />

</body>
</html>