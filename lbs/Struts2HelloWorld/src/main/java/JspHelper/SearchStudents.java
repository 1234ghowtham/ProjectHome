package JspHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redisconnection.RedisService;

public class SearchStudents {

	public Map<String, Object> searchStudentsById(int studentId) {
		
		Map<String, Object> studentDetailsMap = new HashMap<>();
		
		boolean fromCache = false;
		
		String fetchStudentDetailsQuery = """
				SELECT s.Student_id, s.Student_name, b.Book_id, b.Copies_borrowed, b.borrow_date, b.due_date
				FROM Students s
				LEFT JOIN Borrowbooks b ON s.Student_id = b.Student_id
				LEFT JOIN Books bk ON bk.Book_id = b.Book_id
				WHERE s.Student_id = ?
				""";

		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient();
				PreparedStatement fetchStudentDetailsStatement = connection
						.prepareStatement(fetchStudentDetailsQuery);) {

			fetchStudentDetailsStatement.setInt(1, studentId);

			try(ResultSet fetchStudentDetailsResultSet = fetchStudentDetailsStatement.executeQuery())
			{
				
				while (fetchStudentDetailsResultSet.next()) {

					int dbStudentId = fetchStudentDetailsResultSet.getInt("Student_id");
					int dbBookId = fetchStudentDetailsResultSet.getInt("Book_id");

					String studentDetailsKey = "studentId:" + dbStudentId;
					String bookDetailsKey = "bookId:" + dbBookId;
					String borrowedDetailsKey = "studentId:" + dbStudentId + ":bookId:" + dbBookId;

					Map<String, String> studentDataMap = null;
					Map<String, String> bookDataMap = null;
					Map<String, String> borrowDataMap = null;

					if (redisClient != null) {
						try {
							studentDataMap = redisClient.hgetAll(studentDetailsKey);
							bookDataMap = redisClient.hgetAll(bookDetailsKey);
							borrowDataMap = redisClient.hgetAll(borrowedDetailsKey);

							if (studentDataMap != null && bookDataMap != null && borrowDataMap != null
									&& studentDataMap.containsKey("studentId") && bookDataMap.containsKey("bookId")
									&& borrowDataMap.containsKey("borrowedCopies")) {
								fromCache = true;
							}
							
						} catch (Exception e) {
							System.err.println("Redis read failed for studentId " + dbStudentId);
						}
					}

					if (fromCache) {
						studentDetailsMap.put("studentId", safeParseInt(studentDataMap.get("studentId"), 0));
						studentDetailsMap.put("studentName", studentDataMap.get("studentName"));
						studentDetailsMap.put("bookId", safeParseInt(borrowDataMap.get("bookId"), 0));
						studentDetailsMap.put("borrowedCopies", safeParseInt(borrowDataMap.get("borrowedCopies"), 0));
						studentDetailsMap.put("borrowDate", borrowDataMap.get("borrowDate"));
						studentDetailsMap.put("dueDate", borrowDataMap.get("dueDate"));
						studentDetailsMap.put("fine", safeParseInt(borrowDataMap.get("fine"), 0));
					} else {
						studentDetailsMap.put("studentId", dbStudentId);
						studentDetailsMap.put("studentName", fetchStudentDetailsResultSet.getString("Student_name"));
						studentDetailsMap.put("bookId", dbBookId);
						studentDetailsMap.put("borrowedCopies", fetchStudentDetailsResultSet.getInt("Copies_borrowed"));
						studentDetailsMap.put("borrowDate", fetchStudentDetailsResultSet.getDate("borrow_date"));
						studentDetailsMap.put("dueDate", fetchStudentDetailsResultSet.getDate("due_date"));

						// Cache this record for future
						
						if (redisClient != null) {
							try {
								storingInCache(redisClient, studentDetailsMap);
							} catch (Exception e) {
								System.err.println("Redis write failed, continuing without cache: " + e.getMessage());
							}
						}
						
					}

					studentDetailsMap.put("fromCache", fromCache);

					System.out.println(studentDetailsMap);
					System.out.println(fromCache ? "Cache" : "Database");
				}
				
			}} catch (SQLException e) {
				System.err.println("None Student Registered for that entered studentId");
			} catch (Exception e) {
			e.printStackTrace();
		} 

		return studentDetailsMap;
	}

	private static int safeParseInt(Object value, int defaultValue) {
		if (value == null)
			return defaultValue;
		try {
			String str = value.toString().trim();
			if (str.isEmpty() || str.equalsIgnoreCase("null"))
				return defaultValue;
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private void storingInCache(Jedis redisClient, Map<String, Object> studentDetailsMap) {
		
		try {
			
		String studentDetailsKey = "studentId:" + studentDetailsMap.get("studentId");
		String bookDetailsKey = "bookId:" + studentDetailsMap.get("bookId");
		String borrowedDetailsKey = "studentId:" + studentDetailsMap.get("studentId") + ":bookId:" + studentDetailsMap.get("bookId");

		Map<String, String> studentData = new HashMap<>();
		studentData.put("studentId", String.valueOf(studentDetailsMap.get("studentId")));
		studentData.put("studentName", String.valueOf(studentDetailsMap.get("studentName")));
		redisClient.hmset(studentDetailsKey, studentData);

		Map<String, String> bookData = new HashMap<>();
		bookData.put("bookId", String.valueOf(studentDetailsMap.get("bookId")));
		redisClient.hmset(bookDetailsKey, bookData);

		Map<String, String> borrowData = new HashMap<>();
		borrowData.put("borrowedCopies", String.valueOf(studentDetailsMap.get("borrowedCopies")));
		borrowData.put("borrowDate", String.valueOf(studentDetailsMap.get("borrowDate")));
		borrowData.put("dueDate", String.valueOf(studentDetailsMap.get("dueDate")));
		borrowData.put("fine", String.valueOf(studentDetailsMap.getOrDefault("fine", 0)));
		borrowData.put("bookId", String.valueOf(studentDetailsMap.get("bookId")));
		redisClient.hmset(borrowedDetailsKey, borrowData);
		
		} catch (JedisConnectionException e) {
			System.err.println(e.getMessage());
		}
	}
}
