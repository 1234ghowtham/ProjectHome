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

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redisconnection.RedisService;

public class SearchAllRecords {

	public List<Map<String, Object>> searchBorrowedRecordsByMonth(int year, int month) {

		List<Map<String, Object>> searchBborrowRecordsList = new ArrayList<>();

		Map<String, String> cachedRecordMap = null;

		String fetchAllBorrowRecordForMonthQuery = """
				SELECT * FROM Borrowedlist
				WHERE EXTRACT(YEAR FROM Borrow_date) = ?
				  AND EXTRACT(MONTH FROM Borrow_date) = ?
				ORDER BY Borrow_date DESC
				""";

		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient();
				PreparedStatement borrowRecordForMonthStatement = connection
						.prepareStatement(fetchAllBorrowRecordForMonthQuery)) {

			borrowRecordForMonthStatement.setInt(1, year);
			borrowRecordForMonthStatement.setInt(2, month);

			try (ResultSet borrowRecordForMonthResultSet = borrowRecordForMonthStatement.executeQuery()) {

				while (borrowRecordForMonthResultSet.next()) {

					int studentId = borrowRecordForMonthResultSet.getInt("Student_id");
					int bookId = borrowRecordForMonthResultSet.getInt("Book_id");
					Date dueDate = borrowRecordForMonthResultSet.getDate("Due_date");

					if (redisClient != null) {

						try {
							String cacheKey = buildBorrowRecordCacheKey(studentId, bookId, dueDate);
							cachedRecordMap = getCachedBorrowRecord(redisClient, cacheKey);
						} catch (Exception e) {
							System.err.println("Redis read failed — using DB values.");
						}
					}

					Map<String, Object> recordMap = getBorrowRecordFromCacheOrDb(borrowRecordForMonthResultSet,
							cachedRecordMap);
					searchBborrowRecordsList.add(recordMap);
				}

			}
		} catch (SQLException e) {
			System.err.println("Database error while fetching borrowed records: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
		}

		return searchBborrowRecordsList;
	}

	private String buildBorrowRecordCacheKey(int studentId, int bookId, Date dueDate) {

		return "studentId:" + studentId + ":bookId:" + bookId + ":bookIdBorrowList:" + dueDate;
	}

	private Map<String, String> getCachedBorrowRecord(Jedis redisClient, String cacheKey) {

		Map<String, String> cachedMap = redisClient.hgetAll(cacheKey);
		return (cachedMap != null && cachedMap.containsKey("dueDate")) ? cachedMap : null;
	}

	private Map<String, Object> getBorrowRecordFromCacheOrDb(ResultSet borrowRecordForMonthResultSet,
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

			recordMap.put("studentId", borrowRecordForMonthResultSet.getInt("Student_id"));
			recordMap.put("bookId", borrowRecordForMonthResultSet.getInt("Book_id"));
			recordMap.put("borrowDate", borrowRecordForMonthResultSet.getDate("Borrow_date"));
			recordMap.put("dueDate", borrowRecordForMonthResultSet.getDate("Due_date"));
			recordMap.put("returnDate", borrowRecordForMonthResultSet.getDate("Return_date"));
		}

		recordMap.put("fromCache", fromCache);

		System.out.println(recordMap);
		System.out.println(fromCache ? "Cache" : "Database");

		return recordMap;
	}
}
