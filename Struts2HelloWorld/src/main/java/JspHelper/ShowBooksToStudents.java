package JspHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redisconnection.RedisService;

public class ShowBooksToStudents {

	public List<Map<String, Object>> getAllBooks() {
		
		List<Map<String, Object>> booksList = new ArrayList<>();
		
		Map<String, String> cachedDataMap = null;
		
		String getAllBooksQuery = "SELECT * FROM Books ORDER BY book_id";

		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient();
				PreparedStatement getAllBooksStatement = connection.prepareStatement(getAllBooksQuery);
				ResultSet getAllBooksResultSet = getAllBooksStatement.executeQuery()) {

			while (getAllBooksResultSet.next()) {
				
				Map<String, Object> recordMap = new HashMap<>();
				
				int bookId = getAllBooksResultSet.getInt("book_id");
				String redisKey = "bookId:" + bookId;

				if (redisClient != null) {
					
					try {
						
						cachedDataMap = redisClient.hgetAll(redisKey);
						
						if (cachedDataMap.isEmpty()) {
							cachedDataMap = null;
						}
						
					} catch (Exception e) {
						System.err.println("Redis read failed for " + redisKey);
					}
				}
			
				boolean fromCache = cachedDataMap != null;

				if (fromCache) {
					recordMap.put("bookId", Integer.parseInt(cachedDataMap.get("bookId")));
					recordMap.put("bookTitle", cachedDataMap.get("bookTitle"));
					recordMap.put("availableCopies", Integer.parseInt(cachedDataMap.get("bookAvailableCopies")));
					recordMap.put("totalCopies", Integer.parseInt(cachedDataMap.get("bookTotalCopies")));
				} else {
					recordMap.put("bookId", bookId);
					recordMap.put("bookTitle", getAllBooksResultSet.getString("book_title"));
					recordMap.put("availableCopies", getAllBooksResultSet.getInt("book_availablecopies"));
					recordMap.put("totalCopies", getAllBooksResultSet.getInt("book_totalcopies"));
					
					if (redisClient != null) {
						try {
							storingInCache(redisClient, recordMap);
						} catch (Exception e) {
							System.err.println("Redis write failed, continuing without cache: " + e.getMessage());
						}
					}
					
				}

				recordMap.put("fromCache", fromCache);
				System.out.println(recordMap);
				System.out.println(fromCache ? "Cache" : "Database");
				booksList.add(recordMap);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return booksList;
	}

	private void storingInCache(Jedis redisClient, Map<String, Object> recordMap) {
		try {
			
			String key = "bookId:" + recordMap.get("bookId");
			
			Map<String, String> data = new HashMap<>();
			
			data.put("bookId", String.valueOf(recordMap.get("bookId")));
			data.put("bookTitle", String.valueOf(recordMap.get("bookTitle")));
			data.put("bookAvailableCopies", String.valueOf(recordMap.get("availableCopies")));
			data.put("bookTotalCopies", String.valueOf(recordMap.get("totalCopies")));
			redisClient.hmset(key, data);
			
		} catch (JedisConnectionException e) {
			System.err.println(e.getMessage());
		}
	}
}
