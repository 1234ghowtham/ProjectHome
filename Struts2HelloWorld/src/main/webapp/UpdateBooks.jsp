<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<!DOCTYPE html>
<head>
<title>Update Book Details</title>
<style>
body {
	font-family: Arial, sans-serif;
	background-color: #f2f2f2;
	margin: 0;
	padding: 50px;
	display: flex;
	justify-content: center;
}

form {
	background-color: #fff;
	padding: 30px 40px;
	border-radius: 10px;
	box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
	width: 400px;
}

h3 {
	text-align: center;
	color: #333;
	margin-bottom: 25px;
}

label {
	display: block;
	margin: 12px 0 5px;
	color: #555;
}

input[type="text"] {
	width: 100%;
	padding: 10px;
	border: 1px solid #ccc;
	border-radius: 5px;
	box-sizing: border-box;
}

input[type="submit"] {
	width: 100%;
	padding: 12px;
	margin-top: 20px;
	background-color: #4CAF50;
	border: none;
	color: white;
	border-radius: 5px;
	font-size: 16px;
	cursor: pointer;
}

input[type="submit"]:hover {
	background-color: #45a049;
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
	<form action="UpdateBooks" method="post">
		<h3>Update Books</h3>

		<label for="bookId">Book ID:</label><br /> 
		<input type="number" id="bookId" name="bookId" /><br />
		<br /> <label for="bookTitle">Book Title:</label><br /> 
		<input type="text" id="bookTitle" name="bookTitle" /><br />
		<br /> <label for="bookTotalCopies">Total Copies:</label><br /> 
		<input type="number" id="bookTotalCopies" name="bookTotalCopies" /><br />
		<br /> <input type="submit" value="Submit" /> <input type="button"
			value="Back" onclick="window.location.href='Success.jsp'" /> <input
			type="button" value="Home" onclick="window.location.href='index.jsp'" />

	</form>
</body>
</html>










