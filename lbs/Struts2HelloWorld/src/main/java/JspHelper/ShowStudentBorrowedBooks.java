package JspHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redisconnection.RedisService;

public class ShowStudentBorrowedBooks {

	public List<Map<String, Object>> getBorrowedBooks(int studentId) {
		
		List<Map<String, Object>> borrowedBooksList = new ArrayList<>();
		Map<String, Object> recordMap = new HashMap<>();
		
		Map<String, String> cachedDataMap = null;
		Date currentDate = null; 
		
		boolean fromCache = false;

		String getCurrentDateQuery = "SELECT CURRENT_DATE";
		String getBorrowedDetailsQuery = """
				SELECT Book_id, Copies_borrowed, borrow_date
				FROM Borrowbooks
				WHERE Student_id = ?
				""";

		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient();
				PreparedStatement currentDateStatement = connection.prepareStatement(getCurrentDateQuery);
				PreparedStatement borrowedDetailsStatement = connection.prepareStatement(getBorrowedDetailsQuery)) {

			borrowedDetailsStatement.setInt(1, studentId);

			try (ResultSet currentDateResultSet = currentDateStatement.executeQuery();
					ResultSet borrowedDetailsResultSet = borrowedDetailsStatement.executeQuery()) {

				if (currentDateResultSet.next()) {
		            currentDate = currentDateResultSet.getDate(1);
				}
		        
			while (borrowedDetailsResultSet.next()) {
				
				int bookId = borrowedDetailsResultSet.getInt("Book_id");
				int borrowedCopies = borrowedDetailsResultSet.getInt("Copies_borrowed");
				Date borrowDate = borrowedDetailsResultSet.getDate("borrow_date");

				Date dueDate = null;
				
				if (borrowDate != null) {
					long millis = borrowDate.getTime() + TimeUnit.DAYS.toMillis(7);
					dueDate = new java.sql.Date(millis);
				}

				String borrowedBookDetailsCacheKey = "studentId:" + studentId + ":bookId:" + bookId;

				if (redisClient != null) {
					try {
						
						cachedDataMap = redisClient.hgetAll(borrowedBookDetailsCacheKey);
						
						if ((cachedDataMap != null && cachedDataMap.containsKey("studentId"))) {
							fromCache = true;
							System.out.println("Fetched studentId " + studentId + " from Redis cache");
						}
						
					} catch (Exception e) {
						System.err.println("Redis read failed, fallback to DB: " + e.getMessage());
					}
				}
				
				if (fromCache) {
					
					recordMap.put("bookId", Integer.parseInt(cachedDataMap.get("bookId")));
					recordMap.put("borrowedCopies", Integer.parseInt(cachedDataMap.get("borrowedCopies")));
					recordMap.put("borrowDate", cachedDataMap.get("borrowDate"));
					recordMap.put("dueDate", cachedDataMap.get("dueDate"));
					recordMap.put("fine", cachedDataMap.get("fine") != null ? Integer.parseInt(cachedDataMap.get("fine")) : 0);

				} else {
					
					int fine = calculateFine(connection, redisClient != null ? redisClient : null, studentId, bookId,
							borrowDate, dueDate, currentDate);
					recordMap.put("bookId", bookId);
					recordMap.put("borrowedCopies", borrowedCopies);
					recordMap.put("borrowDate", borrowDate);
					recordMap.put("dueDate", dueDate);
					recordMap.put("fine", fine);

					if (redisClient != null) {
						try {
							storingInCache(redisClient, studentId, bookId, recordMap);
						} catch (Exception e) {
							System.err.println("Redis write failed, continuing without cache: " + e.getMessage());
						}
					}
				}
				
				recordMap.put("fromCache", fromCache);
				System.out.println(recordMap);
				System.out.println(fromCache ? "Cache" : "Database");
				borrowedBooksList.add(recordMap);
			}
		}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return borrowedBooksList;
	}

	private int calculateFine(Connection connection, Jedis redisClient, int studentId, int bookId, Date borrowDate,
			Date dueDate, Date currentDate) throws SQLException {

		String checkFineQuery = "SELECT * FROM fine WHERE student_id=? AND book_id=?";
		String insertFineQuery = "INSERT INTO Fine(student_id, book_id, fine_amount) VALUES (?, ?, ?)";
		
		int fine = 0;
		
		if (borrowDate != null && dueDate != null && currentDate.after(dueDate)) {
			
			int diffDays = (int) TimeUnit.MILLISECONDS.toDays(currentDate.getTime() - dueDate.getTime());
			fine = diffDays * 10;

			// Check if fine already exists
			try (PreparedStatement checkFineStatement = connection.prepareStatement(checkFineQuery)) {
				checkFineStatement.setInt(1, studentId);
				checkFineStatement.setInt(2, bookId);
				ResultSet checkFineResultSet = checkFineStatement.executeQuery();

				if (!checkFineResultSet.next()) {
					try (PreparedStatement insertFineStatement = connection.prepareStatement(insertFineQuery)) {
						insertFineStatement.setInt(1, studentId);
						insertFineStatement.setInt(2, bookId);
						insertFineStatement.setInt(3, fine);
						insertFineStatement.executeUpdate();
					}
				}
			}

			if (redisClient != null) {
				redisClient.hset("studentId:" + studentId + ":bookId:" + bookId, "fine", String.valueOf(fine));
			}
		}
		
		return fine;
	}

	private void storingInCache(Jedis redisClient, int studentId, int bookId, Map<String, Object> recordMap) {
		
		String borrowDetailsCacheKey = "studentId:" + studentId + ":bookId:" + bookId;
		
		Map<String, String> dataMap = new HashMap<>();
		
		dataMap.put("bookId", String.valueOf(recordMap.get("bookId")));
		dataMap.put("borrowedCopies", String.valueOf(recordMap.get("borrowedCopies")));
		dataMap.put("borrowDate", recordMap.get("borrowDate") != null ? recordMap.get("borrowDate").toString() : "null");
		dataMap.put("dueDate", recordMap.get("dueDate") != null ? recordMap.get("dueDate").toString() : "null");
		dataMap.put("fine", String.valueOf(recordMap.get("fine")));
		
		redisClient.hmset(borrowDetailsCacheKey, dataMap);
	}
}
