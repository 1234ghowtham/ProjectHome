package com.library.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

import com.library.model.UpdateBorrowBook;
import com.opensymphony.xwork2.ActionContext;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redisconnection.RedisService;

public class UpdateBorrowBookService {

	private String bookDetailsCacheKey;
	private String borrowedBookDetailsCacheKey;
	private String borrowRecordCacheKey;

	private final LocalDate borrowDate = LocalDate.now();
	private final LocalDate dueDate = borrowDate.plusDays(7);

	private final Date sqlBorrowDate = Date.valueOf(borrowDate);
	private final Date sqlDueDate = Date.valueOf(dueDate);

	public String updateBorrowBooks(UpdateBorrowBook updateBorrowBooks) {

		if (!isStudentLoggedIn(updateBorrowBooks)) {
			return "error";
		}

		if (updateBorrowBooks.getBorrowBookCopies() <= 0) {
			System.out.println("Invalid number of copies. Must be greater than 0.");
			return "error";
		}

		buildCacheKeys(updateBorrowBooks);

		try (Jedis redisClient = RedisService.getInstance().getClient();
				Connection connection = DatabaseService.getInstance().getConnection()) {

			if (!updateAvailableCopies(connection, updateBorrowBooks)) {
				System.out.println("Not enough copies available for book ID: " + updateBorrowBooks.getBookId());
				return "error";
			}

			if (isBorrowedDetailsExistInDataBase(connection, updateBorrowBooks)) {
				updateBorrowRecord(connection, updateBorrowBooks);
			} else {
				insertBorrowRecord(connection, updateBorrowBooks);
			}

			int availableCopies = fetchAvailableBookCopies(connection, updateBorrowBooks);

			updateBorrowRecordInCache(redisClient, updateBorrowBooks);

			updateBookAndBorrowedBookDetailsInCache(redisClient, availableCopies, updateBorrowBooks);

			System.out.println("Borrow successful for Student ID: " + updateBorrowBooks.getStudentId());
			return "success";

		} catch (Exception e) {
			System.out.println("Error while borrowing book: " + e.getMessage());
			e.printStackTrace();
			return "error";
		}
	}

	private boolean isStudentLoggedIn(UpdateBorrowBook updateBorrowBooks) {

		Map<String, Object> session = ActionContext.getContext().getSession();

		Integer studentId = (Integer) session.get("studentId");

		if (studentId == null) {
			System.out.println("Student not logged in. Please log in first.");
			return false;
		}

		updateBorrowBooks.setStudentId(studentId);
		return true;
	}

	private void buildCacheKeys(UpdateBorrowBook updateBorrowBooks) {

		bookDetailsCacheKey = "bookId:" + updateBorrowBooks.getBookId();

		borrowedBookDetailsCacheKey = "studentId:" + updateBorrowBooks.getStudentId() + ":bookId:" + updateBorrowBooks.getBookId();

		borrowRecordCacheKey = "studentId:" + updateBorrowBooks.getStudentId() + ":bookId:" + updateBorrowBooks.getBookId()
				+ ":bookIdBorrowList:" + dueDate;
	}

	private boolean updateAvailableCopies(Connection connection, UpdateBorrowBook updateBorrowBooks) throws SQLException {

		String updateBookDetailsQuery = """
				UPDATE Books
				SET book_availablecopies = book_availablecopies - ?
				WHERE Book_id = ? AND book_availablecopies >= ?
				""";

		try (PreparedStatement updateBookDetailsStatement = connection.prepareStatement(updateBookDetailsQuery)) {

			updateBookDetailsStatement.setLong(1, updateBorrowBooks.getBorrowBookCopies());
			updateBookDetailsStatement.setInt(2, updateBorrowBooks.getBookId());
			updateBookDetailsStatement.setLong(3, updateBorrowBooks.getBorrowBookCopies());

			return updateBookDetailsStatement.executeUpdate() > 0;
		}
	}

	private boolean isBorrowedDetailsExistInDataBase(Connection connection, UpdateBorrowBook updateBorrowBooks)
			throws SQLException {

		//checking existence of the query(Select 1) don't want unnecessary data to process.
		String checkBorrowedDetailsExistQuery = "SELECT 1 FROM Borrowbooks " + "WHERE Student_id = ? AND Book_id = ?";

		try (PreparedStatement checkBorrowedDetailsExistStatement = connection
				.prepareStatement(checkBorrowedDetailsExistQuery)) {

			checkBorrowedDetailsExistStatement.setInt(1, updateBorrowBooks.getStudentId());
			checkBorrowedDetailsExistStatement.setInt(2, updateBorrowBooks.getBookId());

			try (ResultSet checkBorrowedDetailsExistResultSet = checkBorrowedDetailsExistStatement.executeQuery()) {
				return checkBorrowedDetailsExistResultSet.next();
			}
		}
	}

	private void updateBorrowRecord(Connection connection, UpdateBorrowBook updateBorrowBooks) throws SQLException {

		String updateCopyOfBorrowedBookQuery = "UPDATE Borrowbooks " + "SET Copies_borrowed = Copies_borrowed + ? "
				+ "WHERE Student_id = ? AND Book_id = ?";

		try (PreparedStatement updateCopyOfBorrowedBookStatement = connection
				.prepareStatement(updateCopyOfBorrowedBookQuery)) {

			updateCopyOfBorrowedBookStatement.setLong(1, updateBorrowBooks.getBorrowBookCopies());
			updateCopyOfBorrowedBookStatement.setInt(2, updateBorrowBooks.getStudentId());
			updateCopyOfBorrowedBookStatement.setInt(3, updateBorrowBooks.getBookId());

			updateCopyOfBorrowedBookStatement.executeUpdate();

			System.out.println("Updated existing Borrowbooks Details for Book ID: " + updateBorrowBooks.getBookId());
		}
	}

	private void insertBorrowRecord(Connection connection, UpdateBorrowBook updateBorrowBooks) throws SQLException {

		String insertBorrowedDetailsQuery = """
				INSERT INTO Borrowbooks
				(Student_id, Book_id, Copies_borrowed, Borrow_date, Due_date)
				VALUES (?, ?, ?, ?, ?)
				""";

		try (PreparedStatement insertBorrowedDetailsStatement = connection.prepareStatement(insertBorrowedDetailsQuery)) {

			insertBorrowedDetailsStatement.setInt(1, updateBorrowBooks.getStudentId());
			insertBorrowedDetailsStatement.setInt(2, updateBorrowBooks.getBookId());
			insertBorrowedDetailsStatement.setLong(3, updateBorrowBooks.getBorrowBookCopies());
			insertBorrowedDetailsStatement.setDate(4, sqlBorrowDate);
			insertBorrowedDetailsStatement.setDate(5, sqlDueDate);

			insertBorrowedDetailsStatement.executeUpdate();
		}

		String insertBorrowedDetailsInRecordQuery = "INSERT INTO borrowedlist "
				+ "(Student_id, Book_id, Borrow_date, Due_date) " + "VALUES (?, ?, ?, ?)";

		try (PreparedStatement insertBorrowedDetailsInRecordStatement = connection
				.prepareStatement(insertBorrowedDetailsInRecordQuery)) {

			insertBorrowedDetailsInRecordStatement.setInt(1, updateBorrowBooks.getStudentId());
			insertBorrowedDetailsInRecordStatement.setInt(2, updateBorrowBooks.getBookId());
			insertBorrowedDetailsInRecordStatement.setDate(3, sqlBorrowDate);
			insertBorrowedDetailsInRecordStatement.setDate(4, sqlDueDate);

			insertBorrowedDetailsInRecordStatement.executeUpdate();
		}

		System.out.println("Inserted new borrow record for Book ID: " + updateBorrowBooks.getBookId());
	}

	private int fetchAvailableBookCopies(Connection connection, UpdateBorrowBook updateBorrowBooks) throws SQLException {

		String getAvailableBookCopiesQuery = "SELECT book_availablecopies FROM Books " + "WHERE Book_id = ?";

		try (PreparedStatement getAvailableBookCopiesStatement = connection
				.prepareStatement(getAvailableBookCopiesQuery)) {

			getAvailableBookCopiesStatement.setInt(1, updateBorrowBooks.getBookId());

			try (ResultSet getAvailableBookCopiesStatementResultSet = getAvailableBookCopiesStatement.executeQuery()) {
				if (getAvailableBookCopiesStatementResultSet.next()) {
					return getAvailableBookCopiesStatementResultSet.getInt(1);
				}
			}
		}

		return 0;
	}

	private void updateBorrowRecordInCache(Jedis redisClient, UpdateBorrowBook updateBorrowBooks) {

		if (redisClient == null) {
			System.out.println("Redis client unavailable for student borrow record update.");
			return;
		}
		
		Transaction transaction = redisClient.multi();
		
		try {

			transaction.hset(borrowRecordCacheKey, "studentId", String.valueOf(updateBorrowBooks.getStudentId()));
			transaction.hset(borrowRecordCacheKey, "bookId", String.valueOf(updateBorrowBooks.getBookId()));
			transaction.hset(borrowRecordCacheKey, "borrowDate", borrowDate.toString());
			transaction.hset(borrowRecordCacheKey, "dueDate", dueDate.toString());
			transaction.hset(borrowRecordCacheKey, "returnDate", "null");

			transaction.exec();

			System.out.println("Redis borrow Record updated successfully.");

		} catch (Exception e) {
			transaction.discard();
			System.out.println("Failed to update Redis borrow Record: " + e.getMessage());
		}
	}

	private void updateBookAndBorrowedBookDetailsInCache(Jedis redisClient, int availableCopies,
			UpdateBorrowBook updateBorrowBooks) {

		if (redisClient == null) {
			System.out.println("Redis client unavailable for cache update.");
			return;
		}
		
		Transaction transaction = redisClient.multi();

		try {

			transaction.hset(bookDetailsCacheKey, "bookAvailableCopies", String.valueOf(availableCopies));
			transaction.hincrBy(borrowedBookDetailsCacheKey, "borrowedCopies", updateBorrowBooks.getBorrowBookCopies());
			transaction.hset(borrowedBookDetailsCacheKey, "studentId", String.valueOf(updateBorrowBooks.getStudentId()));
			transaction.hset(borrowedBookDetailsCacheKey, "bookId", String.valueOf(updateBorrowBooks.getBookId()));
			transaction.hset(borrowedBookDetailsCacheKey, "borrowDate", borrowDate.toString());
			transaction.hset(borrowedBookDetailsCacheKey, "dueDate", dueDate.toString());
			
			transaction.expire(borrowedBookDetailsCacheKey, 3600);

			transaction.exec();

			System.out.println("Redis cache updated successfully for Student ID: " + updateBorrowBooks.getStudentId());

		} catch (Exception e) {
			transaction.discard();
			System.out.println("Redis cache update failed: " + e.getMessage());
		}
	}
}
