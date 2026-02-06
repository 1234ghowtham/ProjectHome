package JspHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redisconnection.RedisService;

public class SearchBook {

	public Map<String, Object> searchBook(int idPrefix) {
		
		Map<String, Object> bookMap = new HashMap<>();
		
		Map<String, String> cachedDataMap = null;
		
		String getBookByBookIdQuery = "SELECT * FROM books WHERE Book_id = ?";


		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient();
				PreparedStatement getBookStatement = connection.prepareStatement(getBookByBookIdQuery)) {

			getBookStatement.setInt(1, idPrefix);

			try (ResultSet getBookResultSet = getBookStatement.executeQuery()) {

				while (getBookResultSet.next()) {

					int bookId = getBookResultSet.getInt("book_id");
					String bookDetailsCacheKey = buildBookDetailsCacheKey(bookId);

					if (redisClient != null) {
						
						try {

							cachedDataMap = redisClient.hgetAll(bookDetailsCacheKey);

							if (cachedDataMap != null && cachedDataMap.containsKey("bookId")) {
								System.out.println("Book from cache: " + bookDetailsCacheKey);
							} else {
								cachedDataMap = null;
							}

						} catch (Exception e) {
							System.err.println("Redis fetch failed: " + e.getMessage());
						}
					}

					 bookMap = getBookRecordFromCacheOrDb(getBookResultSet, cachedDataMap, bookMap, redisClient);
				}
			}

		} catch (Exception e) {
			System.err.println("Error fetching books: " + e.getMessage());
		} 

		return bookMap;
	}

	private String buildBookDetailsCacheKey(int bookId) {
		return "bookId:" + bookId;
	}

	private Map<String, Object> getBookRecordFromCacheOrDb(ResultSet getBookResultSet, Map<String, String> cachedData,
			Map<String, Object> book, Jedis redisClient) throws SQLException {

		boolean fromCache = cachedData != null;
		
		if (fromCache) {
			book.put("bookId", cachedData.get("bookId"));
			book.put("bookTitle", cachedData.get("bookTitle"));
			book.put("bookAvailableCopies", cachedData.get("bookAvailableCopies"));
			book.put("bookTotalCopies", cachedData.get("bookTotalCopies"));

		} else {
			book.put("bookId", getBookResultSet.getInt("book_id"));
			book.put("bookTitle", getBookResultSet.getString("book_title"));
			book.put("bookAvailableCopies", getBookResultSet.getInt("book_availablecopies"));
			book.put("bookTotalCopies", getBookResultSet.getInt("book_totalcopies"));
			
			if (redisClient != null) {
				try {
					storingInCache(redisClient, book);
				} catch (Exception e) {
					System.err.println("Redis write failed, continuing without cache: " + e.getMessage());
				}
			}
		}

		book.put("fromCache", fromCache);

		System.out.println(book);
		System.out.println(fromCache ? "Cache" : "Database");

		return book;
	}
	
	private void storingInCache(Jedis redisClient, Map<String, Object> bookMap) {
		
		String putBookDetailsCacheKey = "bookId:" + bookMap.get("bookId");
		
		Map<String, String> store = new HashMap<>();
		
		store.put("bookId", String.valueOf(bookMap.get("bookId")));
		store.put("bookTitle", String.valueOf(bookMap.get("bookTitle")));
		store.put("bookAvailableCopies", String.valueOf(bookMap.get("bookAvailableCopies")));
		store.put("bookTotalCopies", String.valueOf(bookMap.get("bookTotalCopies")));

		redisClient.hmset(putBookDetailsCacheKey, store);
	}
}
