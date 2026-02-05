<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<head>
<title>Login Page</title>
<style>
body {
	font-family: Arial, sans-serif;
	background-color: #f4f4f4;
	display: flex;
	justify-content: center;
	align-items: flex-start;
	padding-top: 50px;
}

.container {
	display: flex;
	gap: 40px;
}

form {
	background-color: #ffffff;
	padding: 25px 30px;
	border-radius: 10px;
	box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
	width: 300px;
}

h3 {
	text-align: center;
	margin-bottom: 20px;
}

input[type="text"], input[type="password"] {
	width: 100%;
	padding: 10px;
	margin: 8px 0 16px 0;
	border: 1px solid #ccc;
	border-radius: 5px;
	box-sizing: border-box;
}

input[type="submit"] {
	width: 100%;
	padding: 10px;
	background-color: #4CAF50;
	border: none;
	color: white;
	border-radius: 5px;
	cursor: pointer;
	font-size: 16px;
}

input[type="submit"]:hover {
	background-color: #45a049;
}
</style>
</head>

<body>

	<form action="AdminLogin" method="post">

		<h3>ADMIN PORTAL</h3>

		<label for="adminId">Admin Id:</label><br /> <input type="text"
			id="adminId" name="adminId" /><br /> <br /> <label
			for="adminPassword">Admin Password:</label><br /> <input
			type="password" id="adminPassword" name="adminPassword" /><br /> <br />
		<input type="submit" value="Login" />

	</form>

	<form action="StudentLogin" method="post">

		<h3>STUDENT PORTAL</h3>

		<label for="studentId">Student Id:</label><br /> <input type="text"
			id="studentId" name="studentId" /><br /> <br /> <label
			for="studentPassword">Student Password:</label><br /> <input
			type="password" id="studentPassword" name="studentPassword" /><br />

		<br /> <input type="submit" value="Login" />

	</form>
</body>
</html>