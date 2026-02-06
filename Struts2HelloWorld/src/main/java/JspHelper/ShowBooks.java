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

public class ShowBooks {

	public List<Map<String, Object>> getAllBooks() {
		
		List<Map<String, Object>> bookList = new ArrayList<>();
		
		Map<String, String> cachedBook = null;
		boolean fromCache = false;
		
		String getAllBooksQuery = "SELECT * FROM Books ORDER BY book_id";

		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient();
				PreparedStatement getAllBooksStatement = connection.prepareStatement(getAllBooksQuery);
				ResultSet getAllBooksResultSet = getAllBooksStatement.executeQuery()) {

			System.out.println("Executing query for all books...");

			while (getAllBooksResultSet.next()) {
				
				int bookId = getAllBooksResultSet.getInt("book_id");
				String redisKey = "bookId:" + bookId;

				if (redisClient != null) {
					try {
						cachedBook = redisClient.hgetAll(redisKey);
						if (cachedBook != null && cachedBook.containsKey("bookId")) {
							fromCache = true;
							System.out.println("Fetched bookId " + bookId + " from Redis cache");
						}
					} catch (Exception e) {
						System.err.println("Redis read failed for " + redisKey + ": " + e.getMessage());
					}
				}

				Map<String, Object> bookMap = showBookRecordFromCacheOrDb(getAllBooksResultSet, cachedBook, fromCache,
						redisClient);
				bookList.add(bookMap);
			}

		} catch (SQLException e) {
			System.err.println("Database error while fetching books: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
		} 

		return bookList;
	}

	private Map<String, Object> showBookRecordFromCacheOrDb(ResultSet getAllBooksResultSet,
			Map<String, String> cachedBook, boolean fromCache, Jedis redisClient) throws SQLException {

		Map<String, Object> bookMap = new HashMap<>();

		if (fromCache) {
			bookMap.put("bookId", cachedBook.get("bookId"));
			bookMap.put("bookTitle", cachedBook.get("bookTitle"));
			bookMap.put("bookAvailableCopies", cachedBook.get("bookAvailableCopies"));
			bookMap.put("bookTotalCopies", cachedBook.get("bookTotalCopies"));
		} else {
			bookMap.put("bookId", getAllBooksResultSet.getInt("book_id"));
			bookMap.put("bookTitle", getAllBooksResultSet.getString("book_title"));
			bookMap.put("bookAvailableCopies", getAllBooksResultSet.getInt("book_availablecopies"));
			bookMap.put("bookTotalCopies", getAllBooksResultSet.getInt("book_totalcopies"));
			
			if (redisClient != null) {
				try {
					storingInCache(redisClient, bookMap);
				} catch (Exception e) {
					System.err.println("Redis write failed, continuing without cache: " + e.getMessage());
				}
			}

		}

		bookMap.put("fromCache", fromCache);
		System.out.println(bookMap);
		System.out.println(fromCache ? "Cache" : "Database");

		return bookMap;
	}
	
	private void storingInCache(Jedis redisClient, Map<String, Object> bookMap) {
		
		try {
			
			String key = "bookId:" + bookMap.get("bookId");
			
			Map<String, String> data = new HashMap<>();
			
			data.put("bookId", String.valueOf(bookMap.get("bookId")));
			data.put("bookTitle", String.valueOf(bookMap.get("bookTitle")));
			data.put("bookAvailableCopies", String.valueOf(bookMap.get("availableCopies")));
			data.put("bookTotalCopies", String.valueOf(bookMap.get("totalCopies")));
			redisClient.hmset(key, data);
			
		} catch (JedisConnectionException e) {
			System.err.println(e.getMessage());
		}
	}
}
