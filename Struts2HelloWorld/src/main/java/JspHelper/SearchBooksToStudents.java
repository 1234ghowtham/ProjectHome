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
import redisconnection.RedisService;

public class SearchBooksToStudents {

	public List<Map<String, Object>> searchBooks(String bookTitle) {

		List<Map<String, Object>> booksList = new ArrayList<>();
		
		Map<String, String> cachedDataMap = null;
		boolean fromCache = false;
		
		String getBookByTitleQuery = "SELECT * FROM books WHERE book_title ILIKE ?";

		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient();
				PreparedStatement getBookByTitleStatement = connection
						.prepareStatement(getBookByTitleQuery);
				ResultSet getBookByTitleResultSet = getBookByTitleStatement.executeQuery()) {
			
			getBookByTitleStatement.setString(1, bookTitle + "%");

				while (getBookByTitleResultSet.next()) {

					int bookId = getBookByTitleResultSet.getInt("book_id");
					String bookDetailsCacheKey = buildBookDetailsCacheKey(bookId);

					// Attempt to fetch from Redis
					if (redisClient != null) {
						
						try {
						
							cachedDataMap = redisClient.hgetAll(bookDetailsCacheKey);
							
							if (cachedDataMap != null && !cachedDataMap.isEmpty()) {
								fromCache = true;
							} else {
								cachedDataMap = null;
							}
							
						}catch (Exception e) {
							System.err.println("Redis read failed for bookId: " + bookId);
						}
					} 

					// Build book record
					Map<String, Object> bookMap = new HashMap<>();

					if (fromCache) {
						bookMap.put("bookId", Integer.parseInt(cachedDataMap.get("bookId")));
						bookMap.put("bookTitle", cachedDataMap.get("bookTitle"));
						bookMap.put("bookAvailableCopies", Integer.parseInt(cachedDataMap.get("bookAvailableCopies")));
						bookMap.put("bookTotalCopies", Integer.parseInt(cachedDataMap.get("bookTotalCopies")));
					} else {
						bookMap.put("bookId", bookId);
						bookMap.put("bookTitle", getBookByTitleResultSet.getString("book_title"));
						bookMap.put("bookAvailableCopies", getBookByTitleResultSet.getInt("book_availablecopies"));
						bookMap.put("bookTotalCopies", getBookByTitleResultSet.getInt("book_totalcopies"));

						// Cache in Redis for next time
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

					booksList.add(bookMap);
				}

		} catch (Exception e) {
			e.printStackTrace();
		} 

		return booksList;
	}

	private String buildBookDetailsCacheKey(int bookId) {
		return "bookId:" + bookId;
	}
	
	private void storingInCache(Jedis redisClient, Map<String, Object> book) {
		
		String getBookDetailsCacheKey = "bookId:" + book.get("bookId");

		Map<String, String> store = new HashMap<>();
		
		store.put("bookId", String.valueOf(book.get("bookId")));
		store.put("bookTitle", String.valueOf(book.get("bookTitle")));
		store.put("bookAvailableCopies", String.valueOf(book.get("bookAvailableCopies")));
		store.put("bookTotalCopies", String.valueOf(book.get("bookTotalCopies")));

		redisClient.hmset(getBookDetailsCacheKey, store);
	}
}
