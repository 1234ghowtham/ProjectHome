<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<title>Update Returned Details</title>
<style>
body {
	font-family: Arial, sans-serif;
	background-color: #f2f2f2;
	display: flex;
	justify-content: center;
	align-items: center;
	height: 100vh;
}

form {
	background-color: #fff;
	padding: 30px;
	border-radius: 10px;
	box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
	width: 300px;
}

h3 {
	text-align: center;
	color: #333;
}

input[type="text"] {
	width: 100%;
	padding: 10px;
	margin: 8px 0 16px 0;
	border: 1px solid #ccc;
	border-radius: 4px;
}

input[type="submit"] {
	width: 100%;
	background-color: #4CAF50;
	color: white;
	padding: 10px;
	border: none;
	border-radius: 4px;
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

	<form action="UpdateReturnBooks" method="post">
	
		<h3>Return Books</h3>
		
		BookId:<br /><input type="text" name="bookId" /><br />
		ReturnBookCopies:<br /> <input type="text" name="returnBookCopies"
			value="1" readonly="readonly" /><br /> 
			<input type="submit" value="Submit" /> 
			<input type="button" value="Back" onclick="window.location.href='Success1.jsp'" /> 
			<input type="button" value="Home" onclick="window.location.href='index.jsp'" />
	</form>
	
</body>
</html>