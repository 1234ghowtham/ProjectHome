<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Update Borrowed Details</title>
<style>
body {
	font-family: Arial, sans-serif;
	background-color: #eef2f3;
	margin: 0;
	padding: 50px;
	display: flex;
	justify-content: center;
	align-items: center;
}

form {
	background-color: #ffffff;
	padding: 30px 40px;
	border-radius: 10px;
	box-shadow: 0 0 12px rgba(0, 0, 0, 0.1);
	width: 400px;
}

h3 {
	text-align: center;
	color: #333;
	margin-bottom: 25px;
}

label {
	display: block;
	margin-top: 15px;
	font-weight: bold;
	color: #444;
}

input[type="text"] {
	width: 100%;
	padding: 10px;
	margin-top: 6px;
	border: 1px solid #ccc;
	border-radius: 5px;
	box-sizing: border-box;
	font-size: 14px;
}

input[type="submit"] {
	width: 100%;
	margin-top: 25px;
	padding: 12px;
	background-color: #28a745;
	color: white;
	border: none;
	border-radius: 5px;
	font-size: 16px;
	cursor: pointer;
}

input[type="submit"]:hover {
	background-color: #218838;
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
	<form action="UpdateBorrowBooks" method="post">

		<h3>Borrow Books</h3>

		BookId:<br /> <input type="text" name="bookId" /><br /> 
		BookCopies:<br /><input type="text" name="borrowBookCopies" value="1" readonly="readonly" /><br /> 
			<input type="submit" value="Submit" />
			<input type="button" value="Back" onclick="window.location.href='Success1.jsp'" /> 
			<input type="button" value="Home" onclick="window.location.href='index.jsp'" />
	</form>
</body>
</html>