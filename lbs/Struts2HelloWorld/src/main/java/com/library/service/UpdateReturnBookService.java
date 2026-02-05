package com.library.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.library.model.UpdateReturnBook;
import com.opensymphony.xwork2.ActionContext;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redisconnection.RedisService;

public class UpdateReturnBookService {

	private String bookDetailsCacheKey;
	private String borrowedBookDetailsCacheKey;
	private String borrowRecordCacheKey;

	private final LocalDate returnDate = LocalDate.now();
	private final Date returnDateSql = Date.valueOf(returnDate);

	public String updateReturnBook(UpdateReturnBook updateReturnBook) {

		String result = "error";

		Map<String, Object> session = ActionContext.getContext().getSession();

		Integer studentId = (Integer) session.get("studentId");

		if (studentId == null) {
			System.out.println("Student not logged in.");
			return result;
		}

		updateReturnBook.setStudentId(studentId);

		if (updateReturnBook.getReturnBookCopies() <= 0) {
			System.out.println("Return book copies must be greater than 0.");
			return result;
		}	

		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient()) {

			buildCacheKeys(updateReturnBook, redisClient);
			
			if (hasFine(connection, updateReturnBook)) {
				System.out.println("Student has pending fine. Cannot return book.");
				return result;
			}

			int borrowedCopies = getBorrowedCopies(connection, updateReturnBook);

			if (borrowedCopies < updateReturnBook.getReturnBookCopies()) {
				System.out.println("Returning more copies than borrowed.");
				return result;
			}

			updateBorrowedCopies(connection, updateReturnBook);
			updateBookAvailableCopies(connection, updateReturnBook);
			deleteBorrowDetailsWhenNoCopies(connection);

			int updatedBookAvailableCopies = getBookAvailableCopies(connection, updateReturnBook);

			updateBorrowRecord(connection, updateReturnBook);

			updateReturn(redisClient, updatedBookAvailableCopies, updateReturnBook);

			updateFineRedis(redisClient, updateReturnBook);

			System.out.println("Books returned successfully for student ID: " + updateReturnBook.getStudentId());

			result = "success";

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	private boolean hasFine(Connection connection, UpdateReturnBook updateReturnBook) throws Exception {

		String getDueDateQuery = "SELECT Due_date FROM Borrowbooks WHERE Student_id = ? AND Book_id = ?";
		String insertFineDetailsQuery = "INSERT INTO Fine VALUES (?, ?, ?)";

		try (PreparedStatement getDueDateStatement = connection.prepareStatement(getDueDateQuery)) {

			getDueDateStatement.setInt(1, updateReturnBook.getStudentId());
			getDueDateStatement.setInt(2, updateReturnBook.getBookId());

			try (ResultSet getDueDateResultSet = getDueDateStatement.executeQuery()) {

				if (getDueDateResultSet.next()) {

					Date dueDate = getDueDateResultSet.getDate("Due_date");
					Date currentDate = Date.valueOf(LocalDate.now());

					if (currentDate.after(dueDate)) {

						long diffMs = currentDate.getTime() - dueDate.getTime();
						int diffDays = (int) TimeUnit.MILLISECONDS.toDays(diffMs);
						int fine = diffDays * 10;

						try (PreparedStatement insertFineDetailsStatement = connection
								.prepareStatement(insertFineDetailsQuery)) {

							insertFineDetailsStatement.setInt(1, updateReturnBook.getStudentId());
							insertFineDetailsStatement.setInt(2, updateReturnBook.getBookId());
							insertFineDetailsStatement.setInt(3, fine);
							
							insertFineDetailsStatement.executeUpdate();
						}
						catch (SQLException e) {
							System.out.println("Fine due: " + fine);
						}

						return true;
					}
				}
			}
		}

		return false;
	}

	private int getBorrowedCopies(Connection connection, UpdateReturnBook updateReturnBook) throws Exception {

		String getBorrowedCopiesQuery = "SELECT Copies_borrowed FROM Borrowbooks WHERE Book_id = ? AND Student_id = ?";

		try (PreparedStatement getBorrowedCopiesStatement = connection.prepareStatement(getBorrowedCopiesQuery)) {

			getBorrowedCopiesStatement.setInt(1, updateReturnBook.getBookId());
			getBorrowedCopiesStatement.setInt(2, updateReturnBook.getStudentId());

			try (ResultSet getBorrowedCopiesResultSet = getBorrowedCopiesStatement.executeQuery()) {
				if (getBorrowedCopiesResultSet.next()) {
					return getBorrowedCopiesResultSet.getInt("Copies_borrowed");
				}
			}
		}

		return 0;
	}

	private void updateBorrowedCopies(Connection connection, UpdateReturnBook updateReturnBook) throws Exception {

		String updateBorrowBookCopiesQuery = "UPDATE Borrowbooks SET Copies_borrowed = Copies_borrowed - ? "
				+ "WHERE Book_id = ? AND Student_id = ?";

		try (PreparedStatement updateBorrowBookCopiesStatement = connection.prepareStatement(updateBorrowBookCopiesQuery)) {

			updateBorrowBookCopiesStatement.setInt(1, updateReturnBook.getReturnBookCopies());
			updateBorrowBookCopiesStatement.setInt(2, updateReturnBook.getBookId());
			updateBorrowBookCopiesStatement.setInt(3, updateReturnBook.getStudentId());

			updateBorrowBookCopiesStatement.executeUpdate();
		}
	}

	private void updateBookAvailableCopies(Connection connection, UpdateReturnBook updateReturnBook) throws Exception {

		String updateBookCopiesQuery = "UPDATE Books SET Book_availablecopies = Book_availablecopies + ? "
				+ "WHERE Book_id = ?";

		try (PreparedStatement updateBookCopiesStatement = connection.prepareStatement(updateBookCopiesQuery)) {

			updateBookCopiesStatement.setInt(1, updateReturnBook.getReturnBookCopies());
			updateBookCopiesStatement.setInt(2, updateReturnBook.getBookId());

			updateBookCopiesStatement.executeUpdate();
		}
	}

	private void deleteBorrowDetailsWhenNoCopies(Connection connection) throws Exception {

		String deleteBorrowDetailsQuery = "DELETE FROM Borrowbooks WHERE Copies_borrowed = 0";

		try (PreparedStatement deleteBorrowDetailsStatement = connection.prepareStatement(deleteBorrowDetailsQuery)) {
			deleteBorrowDetailsStatement.executeUpdate();
		}
	}

	private int getBookAvailableCopies(Connection connection, UpdateReturnBook updateReturnBook) throws Exception {

		String getBookAvailableCopiesQuery = "SELECT Book_availablecopies FROM Books WHERE Book_id = ?";

		try (PreparedStatement getBookAvailableCopiesStatement = connection
				.prepareStatement(getBookAvailableCopiesQuery)) {

			getBookAvailableCopiesStatement.setInt(1, updateReturnBook.getBookId());

			try (ResultSet getBookAvailableCopiesResultSet = getBookAvailableCopiesStatement.executeQuery()) {
				if (getBookAvailableCopiesResultSet.next()) {
					return getBookAvailableCopiesResultSet.getInt("Book_availablecopies");
				}
			}
		}

		return 0;
	}

	private void updateBorrowRecord(Connection connection, UpdateReturnBook updateReturnBook) throws Exception {

		String updateBorrowRecordQuery = "UPDATE borrowedlist SET Return_date = ? "
				+ "WHERE Student_id = ? AND Book_id = ?";

		try (PreparedStatement updateBorrowRecordStatement = connection.prepareStatement(updateBorrowRecordQuery)) {

			updateBorrowRecordStatement.setDate(1, returnDateSql);
			updateBorrowRecordStatement.setInt(2, updateReturnBook.getStudentId());
			updateBorrowRecordStatement.setInt(3, updateReturnBook.getBookId());

			updateBorrowRecordStatement.executeUpdate();
		}
	}

	private void updateReturn(Jedis redisClient, int availableCopies, UpdateReturnBook updateReturnBook) {

		if (redisClient == null) {
			return;
		}

		if (!redisClient.exists(borrowedBookDetailsCacheKey)) {
			return;
		}

		try {

			Transaction transaction = redisClient.multi();

			transaction.hincrBy(bookDetailsCacheKey, "bookAvailableCopies", updateReturnBook.getReturnBookCopies());
			transaction.hincrBy(borrowedBookDetailsCacheKey, "borrowedCopies", -updateReturnBook.getReturnBookCopies());
			transaction.hset(borrowedBookDetailsCacheKey, "bookId", String.valueOf(updateReturnBook.getBookId()));
			transaction.hset(borrowRecordCacheKey, "returnDate", String.valueOf(new java.sql.Date(System.currentTimeMillis())));
			
			transaction.exec();

			String borrowedCopiesStr = redisClient.hget(borrowedBookDetailsCacheKey, "borrowedCopies");

			if ("0".equals(borrowedCopiesStr)) {
				redisClient.del(borrowedBookDetailsCacheKey);
				System.out.println("Deleted Redis key: " + borrowedBookDetailsCacheKey);
			}

			System.out.println("Redis updated successfully.");

		} catch (Exception e) {
			System.out.println("Failed to update Redis: " + e.getMessage());
		}
	}

	private void updateFineRedis(Jedis redisClient, UpdateReturnBook updateReturnBook) {

		if (redisClient == null) {
			System.err.println("Redis connection not available...");
			return;
		}

		try {

			String dueDateStr = redisClient.hget(borrowedBookDetailsCacheKey, "dueDate");

			if (dueDateStr == null) {
				return;
			}

			long dueTimestamp = Long.parseLong(dueDateStr);

			long currentTimestamp = System.currentTimeMillis();

			if (currentTimestamp > dueTimestamp) {

				long diffMs = currentTimestamp - dueTimestamp;

				long diffDays = TimeUnit.MILLISECONDS.toDays(diffMs);

				int fine = (int) (diffDays * 10);

				redisClient.hset(borrowedBookDetailsCacheKey, "fine", String.valueOf(fine));

				System.out.println("Book is overdue. Fine due: " + fine);
			}

		} catch (NumberFormatException e) {
			System.out.println("Book returned on time. No fine. " + e.getMessage());
		}
	}

	private void buildCacheKeys(UpdateReturnBook updateReturnBook, Jedis redisClient) {

		bookDetailsCacheKey = "bookId:" + updateReturnBook.getBookId();

		borrowedBookDetailsCacheKey = "studentId:" + updateReturnBook.getStudentId() + ":bookId:" + updateReturnBook.getBookId();
		
		String dueDate = redisClient.hget(borrowedBookDetailsCacheKey, "dueDate");
		
		borrowRecordCacheKey = "studentId:" + updateReturnBook.getStudentId() + ":bookId:" + updateReturnBook.getBookId()
		+ ":bookIdBorrowList:" + dueDate;
	}
}
