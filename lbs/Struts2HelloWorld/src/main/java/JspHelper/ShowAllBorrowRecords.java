package JspHelper;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redisconnection.RedisService;

import java.sql.*;

public class ShowAllBorrowRecords {

	public List<Map<String, Object>> getBorrowedRecords() {

		List<Map<String, Object>> borrowRecordsList = new ArrayList<>();

		Map<String, String> cachedRecordMap = null;	

		String fetchBorrowRecordsQuery = "SELECT * FROM Borrowedlist ORDER BY Borrow_date DESC";

		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient();
				PreparedStatement borrowRecordsStatement = connection.prepareStatement(fetchBorrowRecordsQuery);
				ResultSet borrowRecordsResultSet = borrowRecordsStatement.executeQuery()) {

			while (borrowRecordsResultSet.next()) {

				int studentId = borrowRecordsResultSet.getInt("Student_id");
				int bookId = borrowRecordsResultSet.getInt("Book_id");
				Date dueDate = borrowRecordsResultSet.getDate("Due_date");
				
				if (redisClient != null) {
					
					try {
						String borrowRecordCacheKey = buildBorrowRecordCacheKey(studentId, bookId, dueDate);
						cachedRecordMap = getCachedBorrowRecord(redisClient, borrowRecordCacheKey);
					} catch (Exception e) {
						System.err.println("Redis read failed continuing with DB record.");
					}
					
				}

				Map<String, Object> recordMap = getBorrowRecordFromCacheOrDb(borrowRecordsResultSet, cachedRecordMap);
				borrowRecordsList.add(recordMap);
			}

		} catch (SQLException e) {
			System.err.println("Database error while fetching borrowed records: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
		}
		
		return borrowRecordsList;
	}

	private String buildBorrowRecordCacheKey(int studentId, int bookId, Date dueDate) {
		return "studentId:" + studentId + ":bookId:" + bookId + ":bookIdBorrowList:" + dueDate;
	}

	private Map<String, String> getCachedBorrowRecord(Jedis redisClient, String borrowRecordCacheKey) {

		Map<String, String> cachedMap = redisClient.hgetAll(borrowRecordCacheKey);
		return (cachedMap != null && cachedMap.containsKey("dueDate")) ? cachedMap : null;
		
	}

	private Map<String, Object> getBorrowRecordFromCacheOrDb(ResultSet getAllBorrowRecordResultSet,
			Map<String, String> cachedRecord) throws SQLException {

		Map<String, Object> recordMap = new HashMap<>();

		boolean fromCache = cachedRecord != null;

		if (fromCache) {

			recordMap.put("studentId", cachedRecord.get("studentId"));
			recordMap.put("bookId", cachedRecord.get("bookId"));
			recordMap.put("borrowDate", cachedRecord.get("borrowDate"));
			recordMap.put("dueDate", cachedRecord.get("dueDate"));
			recordMap.put("returnDate", cachedRecord.get("returnDate"));

		} else {

			recordMap.put("studentId", getAllBorrowRecordResultSet.getInt("Student_id"));
			recordMap.put("bookId", getAllBorrowRecordResultSet.getInt("Book_id"));
			recordMap.put("borrowDate", getAllBorrowRecordResultSet.getDate("Borrow_date"));
			recordMap.put("dueDate", getAllBorrowRecordResultSet.getDate("Due_date"));
			recordMap.put("returnDate", getAllBorrowRecordResultSet.getDate("Return_date"));
		}

		recordMap.put("fromCache", fromCache);

		System.out.println(recordMap);
		System.out.println(fromCache ? "Cache" : "Database");

		return recordMap;
	}
}
