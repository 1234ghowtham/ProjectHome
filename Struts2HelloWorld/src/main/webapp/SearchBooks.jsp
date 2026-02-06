<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" errorPage="error.jsp"%>
<%@page import="JspHelper.SearchBook"%>
<%@page import="java.util.*"%>

<!DOCTYPE html>
<head>
<meta charset="UTF-8">
<title>S</title>
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

	<%
	Map<String, Object> book = new HashMap<>();
	SearchBook service = new SearchBook();
	
	String bookIdParam = request.getParameter("bookId");

	if (bookIdParam != null && !bookIdParam.trim().isEmpty()) {
		
		int bookId = Integer.parseInt(bookIdParam.trim()); // Convert to int
		book = service.searchBook(bookId);
	}
	%>
	
	<table>
		
		<%
		if (!book.isEmpty()) {
		%>
		
		<tr>
			<th>Book ID</th>
			<th>Book Title</th>
			<th>Available Copies</th>
			<th>Total Copies</th>
		</tr>
		<tr>
			<td><%=book.get("bookId")%></td>
			<td><%=book.get("bookTitle")%></td>
			<td><%=book.get("bookAvailableCopies")%></td>
			<td><%=book.get("bookTotalCopies")%></td>
			<td>
				<form action="DeleteBooks" method="post"
					onsubmit="return confirm('Are you sure you want to delete this book?');">
					<input type="hidden" name="bookId" value="<%=book.get("bookId")%>" />
					<input type="submit" value="Delete" />
				</form>
			</td>
		</tr>
		
		<%
		} else {
		%>
		
		<tr>
			<td colspan="5">No book records found.</td>
		</tr>
		
		<%
		}
		%>
		
	</table>
	<input type="button" value="Back"
		onclick="window.location.href='ShowBooks.jsp'" />
	<input type="button" value="Home"
		onclick="window.location.href='index.jsp'" />
</body>
</html>