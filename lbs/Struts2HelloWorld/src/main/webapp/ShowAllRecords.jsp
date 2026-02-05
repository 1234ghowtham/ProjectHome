<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" errorPage="error.jsp"%>
<%@page import="JspHelper.ShowAllBorrowRecords"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>All Records</title>
<style>
body {
	font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
	background-color: #f4f6f9;
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
	margin-top: 20px;
	background-color: #ffffff;
	box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

th, td {
	padding: 14px 16px;
	border: 1px solid #ddd;
	text-align: center;
}

th {
	background-color: #343a40;
	color: white;
	text-transform: uppercase;
	letter-spacing: 0.5px;
}

tr:nth-child(even) {
	background-color: #f9f9f9;
}

tr:hover {
	background-color: #f1f1f1;
}

input[type="submit"] {
	background-color: #dc3545;
	color: white;
	border: none;
	padding: 8px 14px;
	border-radius: 4px;
	font-size: 14px;
	cursor: pointer;
	transition: background-color 0.3s;
}

input[type="submit"]:hover {
	background-color: #c82333;
}

input[type="button"] {
	background-color: #28a745;
	color: white;
	border: none;
	padding: 10px 20px;
	margin-top: 30px;
	margin-right: 10px;
	font-size: 14px;
	border-radius: 5px;
	cursor: pointer;
	transition: background-color 0.3s;
}

input[type="button"]:hover {
	background-color: #218838;
}

td[colspan="5"] {
	font-style: italic;
	color: #666;
}
</style>
</head>



<body>

	<form action="SearchAllRecords.jsp" method="post" class="search-form">

		<input type="number" name="Year" placeholder="Year..." min="2000" max="2100" required />
		<input type="number" name="Month" placeholder="Month..." min="1" max="12"required />
		<button type="submit" title="Search">🔍</button>

	</form>

	<%
		ShowAllBorrowRecords show = new ShowAllBorrowRecords();
		List<Map<String, Object>> recordsList = show.getBorrowedRecords();
	%>
	
	<table>
		<tr>
			<th>Student Id</th>
			<th>Book ID</th>
			<th>Borrow Date</th>
			<th>Due Date</th>
			<th>Return Date</th>
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
			<td><%=record.get("studentId")%></td>
			<td><%=record.get("bookId")%></td>
			<td><%=record.get("borrowDate")%></td>
			<td><%=record.get("dueDate")%></td>
			<td><%=record.get("returnDate")%></td>
		</tr>
		
		<%
		}
		}
		%>
		
	</table>

	<input type="button" value="Back"
		onclick="window.location.href='Success.jsp'" />
	<input type="button" value="Home"
		onclick="window.location.href='index.jsp'" />

</body>
</html>