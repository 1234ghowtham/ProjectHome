<%@page import="java.util.concurrent.TimeUnit"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" errorPage="error.jsp"%>
<%@ page import="java.util.*"%>
<%@page import="JspHelper.ShowStudentBorrowedBooks"%>

<!DOCTYPE html>
<head>
<meta charset="UTF-8">
<title>Student Borrowed Details</title>
<style>
body {
	font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
	background-color: #f8f9fa;
	margin: 0;
	padding: 30px;
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
	color: #ffffff;
	font-weight: bold;
	text-transform: uppercase;
	letter-spacing: 0.5px;
}

tr:nth-child(even) {
	background-color: #f1f3f5;
}

tr:hover {
	background-color: #e9ecef;
}

input[type="button"] {
	background-color: #007bff;
	color: #fff;
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
	background-color: #0056b3;
}

td[colspan='5'] {
	font-style: italic;
	color: #888;
	text-align: center;
	padding: 20px;
}
</style>
</head>

<body>

	<%
	ShowStudentBorrowedBooks show = new ShowStudentBorrowedBooks();

	Integer studentId = (Integer) session.getAttribute("studentId");

	List<Map<String, Object>> records = show.getBorrowedBooks(studentId);
	System.out.println(records);
	%>

	<table>

		<tr>
			<th>Book ID</th>
			<th>Book Copies</th>
			<th>Borrow Date</th>
			<th>Due Date</th>
			<th>Fine</th>
		</tr>

		<%if(records.isEmpty()){ %>

		<tr>
			<td colspan="6">No book records found.</td>
		</tr>

		<%
		} else {
			for (Map<String, Object> record : records) {
		%>
		
		<tr>
			<td><%=record.get("bookId")%></td>
			<td><%=record.get("borrowedCopies")%></td>
			<td><%=record.get("borrowDate")%></td>
			<td><%=record.get("dueDate")%></td>
			<td><%=record.get("fine")%></td>
		</tr>

		<% }} %>
		
	</table>
	<input type="button" value="Back"
		onclick="window.location.href='Success1.jsp'" />
	<input type="button" value="Home"
		onclick="window.location.href='index.jsp'" />
	<input type="button" value="PayFine"
		onclick="window.location.href='Payfine.jsp'" />
</body>
</html>