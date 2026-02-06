package com.library.service;

import java.sql.Connection;
import java.sql.PreparedStatement;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redisconnection.RedisService;
import com.library.model.UpdateBook;

public class UpdateBooksService {

	public String updateBooks(UpdateBook updateBook) {

		updateBook.bookAvailableCopies = updateBook.getBookTotalCopies();

		String result = "error";
		String bookDetailsCacheKey = buildBookDetailsCacheKey(updateBook.getBookId());

		try (Jedis redisClient = RedisService.getInstance().getClient();
				Connection connection = DatabaseService.getInstance().getConnection()) {

			if (isBookUpdatedInDataBase(connection, updateBook)) {

				updateBookInCache(redisClient, bookDetailsCacheKey, updateBook);
				result = "success";
			}

		} catch (Exception e) {
			System.out.println("Error while updating books: " + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

	private String buildBookDetailsCacheKey(int bookId) {
		return "bookId:" + bookId;
	}

	private boolean isBookUpdatedInDataBase(Connection connection, UpdateBook updateBook) {

		String insertBookDetailsQuery = "INSERT INTO Books(Book_id, Book_title, Book_totalcopies, Book_availablecopies) "
				+ "VALUES (?, ?, ?, ?)";

		try (PreparedStatement insertBookDetailsStatement = connection.prepareStatement(insertBookDetailsQuery)) {

			if (updateBook.getBookId() > 0) {
				insertBookDetailsStatement.setInt(1, updateBook.getBookId());
			} else {
				return false;
			}

			if (updateBook.getBookTitle() != null) {
				insertBookDetailsStatement.setString(2, updateBook.getBookTitle());
			} else {
				return false;
			}

			if (updateBook.getBookTotalCopies() > 0) {

				insertBookDetailsStatement.setInt(3, updateBook.getBookTotalCopies());
				insertBookDetailsStatement.setInt(4, updateBook.bookAvailableCopies);

			} else {
				return false;
			}

			System.out.println("Executing SQL to insert book: " + updateBook.getBookTitle());

			int bookInsertedCount = insertBookDetailsStatement.executeUpdate();

			if (bookInsertedCount > 0) {

				System.out.println("Book inserted successfully into DB (ID: " + updateBook.getBookId() + ")");
				return true;

			} else {
				System.out.println("Book insertion failed for ID: " + updateBook.getBookId());
			}

		} catch (Exception e) {
			System.out.println("Database error while inserting book: " + e.getMessage());
		}

		return false;
	}

	private void updateBookInCache(Jedis redisClient, String redisKey, UpdateBook updateBook) {

		if (redisClient == null) {
			System.out.println("Redis client is null — skipping cache update.");
			return;
		}

		try {

			redisClient.hset(redisKey, "bookId", String.valueOf(updateBook.getBookId()));
			redisClient.hset(redisKey, "bookTitle", updateBook.getBookTitle());
			redisClient.hset(redisKey, "bookTotalCopies", String.valueOf(updateBook.getBookTotalCopies()));
			redisClient.hset(redisKey, "bookAvailableCopies", String.valueOf(updateBook.bookAvailableCopies));

			redisClient.expire(redisKey, 3600);

			System.out.println("Book cached successfully in Redis (Key: " + redisKey + ")");

		} catch (Exception e) {
			System.out.println("Redis cache update failed: " + e.getMessage());
		}
	}
}
