<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>

<head>
<title>Welcome To Admin Login Page</title>
<style>
body {
	font-family: Arial, sans-serif;
	background-color: #f2f2f2;
	margin: 0;
	padding: 50px;
	display: flex;
	flex-direction: column;
	align-items: center;
}

h1 {
	color: #333;
	margin-bottom: 10px;
}

.card {
	background-color: #ffffff;
	width: 300px;
	padding: 25px;
	margin: 15px;
	border-radius: 10px;
	text-align: center;
	box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
}

.card h3 {
	margin-bottom: 20px;
	color: #444;
}

.card button {
	padding: 10px 20px;
	font-size: 16px;
	background-color: #007BFF;
	border: none;
	color: white;
	border-radius: 5px;
	cursor: pointer;
}

.card button:hover {
	background-color: #0056b3;
}

.container {
	display: flex;
	flex-wrap: wrap;
	justify-content: center;
}

body {
	font-family: Arial, sans-serif;
	background-color: #f2f2f2;
	margin: 0;
	padding: 20px;
}

h1 {
	text-align: center;
	color: #333;
}

.section {
	background-color: #fff;
	border-radius: 8px;
	padding: 20px;
	margin: 20px auto;
	width: 60%;
	box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
}

.section h3 {
	margin-bottom: 5px;
	color: #444;
}

.button-group {
	display: flex;
	flex-wrap: wrap;
	gap: 20px;
	justify-content: center;
}

button {
	padding: 10px 20px;
	background-color: #4CAF50;
	color: white;
	border: none;
	border-radius: 5px;
	font-size: 16px;
	cursor: pointer;
	width: 200px; /* Button width */
}

button:hover {
	background-color: #45a049;
}
</style>
</head>

<body>
	<h3>Update Books</h3>
	<button onclick="location.href='UpdateBooks.jsp'">UpdateBooks</button>
</body>

<body>
	<h3>Show Books</h3>
	<button onclick="location.href='ShowBooks.jsp'">ShowBooks</button>
</body>

<body>
	<h3>Update Student</h3>
	<button onclick="location.href='UpdateStudents.jsp'">UpdateStudent</button>
</body>

<body>
	<h3>Show Student</h3>
	<button onclick="location.href='ShowStudents.jsp'">ShowStudent</button>
</body>

<body>
	<h3>All Records</h3>
	<button onclick="location.href='ShowAllRecords.jsp'">All Records</button>
</body>

<body>
	<h3>Home</h3>
	<button onclick="location.href='index.jsp'">🏠 Home</button>
</body>
</html>