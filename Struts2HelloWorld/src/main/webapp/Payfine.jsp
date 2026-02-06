<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" errorPage="error.jsp"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Pay Fine Amount</title>
<style>
body {
    font-family: Arial, sans-serif;
    background-color: #f4f4f4;
    margin: 0;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
}

form {
    background-color: #ffffff;
    padding: 30px 40px;
    border-radius: 8px;
    box-shadow: 0 4px 8px rgba(0,0,0,0.1);
    width: 300px;
}

h3 {
    margin-top: 0;
    margin-bottom: 20px;
    color: #333333;
    text-align: center;
}

label {
    display: block;
    margin-bottom: 5px;
    color: #555555;
    font-weight: bold;
}

input[type="text"] {
    width: 100%;
    padding: 8px 10px;
    margin-bottom: 15px;
    border: 1px solid #cccccc;
    border-radius: 4px;
    box-sizing: border-box;
}

input[type="submit"],
input[type="button"] {
    width: 100%;
    padding: 10px;
    margin-top: 10px;
    border: none;
    border-radius: 4px;
    background-color: #4285f4;
    color: white;
    font-size: 14px;
    cursor: pointer;
    transition: background-color 0.3s ease;
}

input[type="button"]:nth-of-type(2) {
    background-color: #6c757d; /* Back button */
}

input[type="button"]:nth-of-type(3) {
    background-color: #28a745; /* Home button */
}

input[type="submit"]:hover,
input[type="button"]:hover {
    opacity: 0.9;
}
</style>
</head>
<body>
	<form action="FineAmount" method="post">

		<h3>Pay Fine</h3>

		<label for="bookId">Book Id:</label><br /> <input type="text"
			id="bookId" name="bookId" /><br />
		<br /> <label for="fine">Fine Amount:</label><br /> <input
			type="text" id="fine" name="fine" /><br />
		<br /> <input type="submit" value="Submit" /> <input type="button"
			value="Back"
			onclick="window.location.href='ShowStudentBorrowedBooks.jsp'" /> <input
			type="button" value="Home" onclick="window.location.href='index.jsp'" />
	</form>
</body>
</html>