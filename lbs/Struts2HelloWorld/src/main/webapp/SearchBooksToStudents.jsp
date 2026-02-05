<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" errorPage="error.jsp"%>
<%@page import="JspHelper.SearchBooksToStudents"%>
<%@page import="java.util.*"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Search Books For Students</title>
<style>
body {
	text-align: center;
	font-family: Arial, sans-serif;
}

table {
	margin: 20px auto;
	border-collapse: collapse;
	width: 80%;
}

th, td {
	border: 1px solid #000;
	padding: 12px 20px;
	text-align: center;
}

th {
	background-color: #f2f2f2;
}

form {
	margin: 0;
}

input[type="submit"] {
	padding: 5px 10px;
	background-color: #ff4d4d;
	color: white;
	border: none;
	border-radius: 4px;
	cursor: pointer;
}

input[type="submit"]:hover {
	background-color: #cc0000;
}

form {
	display: flex;
	flex-direction: column;
	align-items: center; /* Centers all form elements */
	background-color: #fff;
	padding: 30px 40px;
	border-radius: 10px;
	box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
	width: 400px;
}

input[type="button"] {
	background-color: #2196F3;
	width: 48%; /* Adjust the width if needed */
	padding: 12px;
	margin-top: 20px;
	color: white;
	border: none;
	border-radius: 5px;
	font-size: 16px;
	cursor: pointer;
}

input[type="button"]:hover {
	background-color: #0b7dda;
}

.button-container {
	display: flex;
	justify-content: center; /* Centers the buttons horizontally */
	gap: 10px; /* Space between the buttons */
	width: 100%;
}
</style>
</head>

<body>
	<%
	SearchBooksToStudents bookService = new SearchBooksToStudents();
	
	String bookTitle = request.getParameter("bookTitle");
	
	List<Map<String, Object>> books = bookService.searchBooks(bookTitle);
	%>
	
	<table>
		<tr>
			<th>Book ID</th>
			<th>Book Title</th>
			<th>Available Copies</th>
			<th>Total Copies</th>
		</tr>
		
		<%
		if (books.isEmpty()) {
		%>
		
		<tr>
			<td colspan="5">No book records found.</td>
		</tr>
		
		<%
		} else {
		for (Map<String, Object> book : books) {
		%>
		
		<tr>
			<td><%=book.get("bookId")%></td>
			<td><%=book.get("bookTitle")%></td>
			<td><%=book.get("bookAvailableCopies")%></td>
			<td><%=book.get("bookTotalCopies")%></td>
			<td><%=(Boolean.TRUE.equals(book.get("fromCache")) ? "Cache" : "DB")%></td>
		</tr>
		
		<%
		}
		}
		%>
		
	</table>
	<input type="button" value="Back"
		onclick="window.location.href='ShowBooks.jsp'" />
	<input type="button" value="Home"
		onclick="window.location.href='index.jsp'" />
</body>
</html>