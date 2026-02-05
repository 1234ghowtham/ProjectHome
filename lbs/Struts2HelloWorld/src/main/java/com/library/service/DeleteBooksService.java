package com.library.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.library.model.DeleteBook;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redisconnection.RedisService;

public class DeleteBooksService {

	public String deleteBook(DeleteBook deleteBook) {

		String result = "error";
		String bookDetailsCacheKey = buildBookDetailsCacheKey(deleteBook.getBookId());

		try (Jedis redisClient = RedisService.getInstance().getClient();
				Connection connection = DatabaseService.getInstance().getConnection()) {

			if (isBookDeletedFromDataBase(redisClient, connection, bookDetailsCacheKey, deleteBook)) {
				return result = "success";
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = "error";
		}

		return result;
	}

	private String buildBookDetailsCacheKey(int bookId) {
		return "bookId:" + bookId;
	}

	private String buildStudentBorrowedBookDetailsCacheKey(int bookId, int stuId) {
		return "studentId:" + stuId + ":bookId:" + bookId;
	}

	private boolean isBookDeletedFromDataBase(Jedis redisClient, Connection connection, String bookDetailsCacheKey,
			DeleteBook deleteBook) {

		boolean result = false;

		String deleteBorrowBookByBookIdQuery = "DELETE FROM borrowbooks WHERE book_id = ?";
		String deleteBookByBookIdQuery = "DELETE FROM books WHERE book_id = ?";
		String getStudentIdByBookIdQuery = "SELECT student_id FROM borrowbooks WHERE book_id = ?";

		try (PreparedStatement getStudentIdStatement = connection.prepareStatement(getStudentIdByBookIdQuery)) {

			getStudentIdStatement.setInt(1, deleteBook.getBookId());
			System.out.println("Executing query (getStudentIdByBookIdQuery)...");

			try (ResultSet getStudentIdResultSet = getStudentIdStatement.executeQuery();
					PreparedStatement deleteBorrowedBookStatement = connection
							.prepareStatement(deleteBorrowBookByBookIdQuery);
					PreparedStatement deleteBookStatement = connection.prepareStatement(deleteBookByBookIdQuery)) {

				deleteBorrowedBookStatement.setInt(1, deleteBook.getBookId());
				deleteBookStatement.setInt(1, deleteBook.getBookId());

				System.out.println("Executing update query (deleteBorrowBookByBookIdQuery)...");
				int deletedBorrowedBookCount = deleteBorrowedBookStatement.executeUpdate();

				System.out.println("Executing update query (deleteBookByBookIdQuery)...");
				int deletedBookCount = deleteBookStatement.executeUpdate();

				if (deletedBookCount > 0 || deletedBorrowedBookCount > 0) {

					System.out.println("Book deleted successfully in DB, BookId: " + deleteBook.getBookId());
					result = true;

					while (getStudentIdResultSet.next()) {

						int studentId = getStudentIdResultSet.getInt("student_id");
						String studentBorrowedBookDetailsCacheKey = buildStudentBorrowedBookDetailsCacheKey(
								deleteBook.getBookId(), studentId);

						deleteBorrowedBookInCache(redisClient, studentBorrowedBookDetailsCacheKey, deleteBook);
					}

					deleteBookInCache(redisClient, bookDetailsCacheKey, deleteBook);

				} else {
					System.out.println("Book deletion failed, BookId: " + deleteBook.getBookId());
				}
			}
			
		} catch (Exception e) {
			System.out.println("DB operation failed: " + e.getMessage());
		}

		return result;
	}

	private void deleteBorrowedBookInCache(Jedis redisClient, String studentBorrowedBookDetailsCacheKey, DeleteBook deleteBook) {

		if (redisClient != null) {
			
			try {
				
				if (redisClient.exists(studentBorrowedBookDetailsCacheKey)) {

					redisClient.del(studentBorrowedBookDetailsCacheKey);
					System.out.println("Borrowbook deleted in Redis, BookId: " + deleteBook.getBookId());

				} else {
					System.out.println("Borrowbook key not found in Redis for BookId: " + deleteBook.getBookId());
				}
				
			} catch (Exception e) {
				System.out.println("Failed to delete Borrowbook in Redis: " + e.getMessage());
			}
			
		} else {
			System.out.println("Redis client not available!");
		}
	}

	private void deleteBookInCache(Jedis redisClient, String bookDetailsCacheKey, DeleteBook deleteBook) {

		if (redisClient != null) {
			
			try {
				
				if (redisClient.exists(bookDetailsCacheKey)) {

					redisClient.del(bookDetailsCacheKey);
					System.out.println("Book deleted in Redis, BookId: " + deleteBook.getBookId());

				} else {
					System.out.println("Book key not found in Redis for BookId: " + deleteBook.getBookId());
				}
				
			} catch (Exception e) {
				System.out.println("Failed to delete Book in Redis: " + e.getMessage());
			}
			
		} else {
			System.out.println("Redis client not available!");
		}
	}
}

