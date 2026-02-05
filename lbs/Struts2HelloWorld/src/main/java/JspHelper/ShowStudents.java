package JspHelper;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redisconnection.RedisService;

public class ShowStudents {

	public List<Map<String, Object>> getStudentRecord() {
		
		List<Map<String, Object>> recordList = new ArrayList<>();
		
		Map<String, String> borrowedDataMap = null;
		Map<String, String> studentDataMap = null;
		Date currentDate = null;
		boolean fromCache = false;
		
		String getCurrentDateQuery = "SELECT CURRENT_DATE";
		String getStudentDetailsQuery = """
				SELECT s.Student_id, s.Student_name, b.Book_id, b.Copies_borrowed, b.Borrow_date
				FROM Students s
				LEFT JOIN Borrowbooks b ON s.Student_id = b.Student_id
				LEFT JOIN Books bk ON bk.Book_id = b.Book_id
				ORDER BY s.Student_id
				""";

		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient();
				PreparedStatement currentDateStatement = connection.prepareStatement(getCurrentDateQuery);
				PreparedStatement studentDetailsStatement = connection.prepareStatement(getStudentDetailsQuery);
				ResultSet currentDateResultSet = currentDateStatement.executeQuery();
				ResultSet studentDetailsResultSet = studentDetailsStatement.executeQuery()) {
				
				if (currentDateResultSet.next()) {
					 currentDate = currentDateResultSet.getDate(1);
					 System.out.println("Executing query for all students...");
				}

			while (studentDetailsResultSet.next()) {
				
				Map<String, Object> recordMap = new HashMap<>();
				
				int studentId = studentDetailsResultSet.getInt("Student_id");
				int bookId = studentDetailsResultSet.getInt("Book_id");
				Date borrowDate = studentDetailsResultSet.getDate("Borrow_date");

				String borrowedDetailsCacheKey = "studentId:" + studentId + ":bookId:" + bookId;
				String studentDetailsCacheKey = "studentId:" + studentId;

				if (redisClient != null) {
					
					try {
						borrowedDataMap = redisClient.hgetAll(borrowedDetailsCacheKey);
						studentDataMap = redisClient.hgetAll(studentDetailsCacheKey);
						
						if ((borrowedDataMap != null && borrowedDataMap.containsKey("studentId"))
								| (studentDataMap != null && studentDataMap.containsKey("studentId"))) {
							
							fromCache = true;
							System.out.println("Fetched studentId " + studentId + " from Redis cache");
						}
						
					} catch (Exception e) {
						System.err.println("Redis read failed. Using DB record for student " + studentId);
					}
				}

				if (fromCache) {
					recordMap.put("studentId", safeParseInt(studentDataMap.get("studentId"), 0));
					recordMap.put("studentName", studentDataMap.get("studentName"));
					recordMap.put("bookId", safeParseInt(borrowedDataMap.get("bookId"), 0));
					recordMap.put("copiesBorrowed", safeParseInt(borrowedDataMap.get("borrowedCopies"), 0));
					recordMap.put("borrowDate", borrowedDataMap.get("borrowDate"));
					recordMap.put("dueDate", borrowedDataMap.get("dueDate"));
					recordMap.put("fine", safeParseInt(borrowedDataMap.get("fine"), 0));
				} else {
					recordMap.put("studentId", studentId);
					recordMap.put("studentName", studentDetailsResultSet.getString("Student_name"));
					recordMap.put("bookId", bookId);
					recordMap.put("copiesBorrowed", studentDetailsResultSet.getInt("Copies_borrowed"));
					recordMap.put("borrowDate", borrowDate);

					Date dueDate = borrowDate != null ? new Date(borrowDate.getTime() + TimeUnit.DAYS.toMillis(7))
							: null;
					recordMap.put("dueDate", dueDate);

					int fine = 0;
					
					if (borrowDate != null && currentDate.after(dueDate)) {
						long daysDifference = TimeUnit.MILLISECONDS.toDays(currentDate.getTime() - dueDate.getTime());
						fine = (int) daysDifference * 10; // ₹10 per day
						recordMap.put("fine", fine);
						updateFineInDB(connection, studentId, bookId, fine);
					} else {
						recordMap.put("fine", 0);
					}

					if (redisClient != null) {
						storingInCache(redisClient, recordMap);
					}
				}

				recordMap.put("fromCache", fromCache);
				System.out.println(recordMap);
				System.out.println(fromCache ? "Cache" : "Database");
				recordList.add(recordMap);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return recordList;
	}

	private void updateFineInDB(Connection connection, int studentId, int bookId, long fine) {
		
		String fetchFineDetailsQuery = "SELECT * FROM fine WHERE student_id=? AND book_id=?";
		String putFineQuery = "INSERT INTO fine VALUES (?, ?, ?)";
		
		try (PreparedStatement fetchFineStatement = connection.prepareStatement(fetchFineDetailsQuery)) {

			fetchFineStatement.setInt(1, studentId);
			fetchFineStatement.setInt(2, bookId);
			ResultSet fetchFineResultSet = fetchFineStatement.executeQuery();

			if (!fetchFineResultSet.next()) {
				
				try (PreparedStatement putFineStatement = connection.prepareStatement(putFineQuery)) {
					putFineStatement.setInt(1, studentId);
					putFineStatement.setInt(2, bookId);
					putFineStatement.setLong(3, fine);
					putFineStatement.executeUpdate();
				}
			}

		} catch (SQLException e) {
			System.err.println("Fine update failed: " + e.getMessage());
		}
	}

	private static int safeParseInt(Object value, int defaultValue) {
		
		if (value == null)
			return defaultValue;
		
		try {
			
			String string = value.toString().trim();
			
			if (string.isEmpty() || string.equalsIgnoreCase("null"))
				return defaultValue;
			
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private void storingInCache(Jedis redisClient, Map<String, Object> recordMap) {
		
		Map<String, String> dataMap = new HashMap<>();
		
		String studentDetailsCacheKey = "studentId:" + recordMap.get("studentId");
		
		dataMap.put("studentId", String.valueOf(recordMap.get("studentId")));
		dataMap.put("studentName", String.valueOf(recordMap.get("studentName")));
		dataMap.put("bookId", String.valueOf(recordMap.get("bookId")));
		dataMap.put("copiesBorrowed", String.valueOf(recordMap.get("copiesBorrowed")));
		dataMap.put("borrowDate", recordMap.get("borrowDate") != null ? recordMap.get("borrowDate").toString() : "");
		dataMap.put("dueDate", recordMap.get("dueDate") != null ? recordMap.get("dueDate").toString() : "");
		dataMap.put("fine", String.valueOf(recordMap.get("fine")));
		redisClient.hmset(studentDetailsCacheKey, dataMap);
	}
}
