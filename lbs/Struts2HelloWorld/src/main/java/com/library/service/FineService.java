package com.library.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

import com.library.model.Fine;
import com.opensymphony.xwork2.ActionContext;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redisconnection.RedisService;

public class FineService {

	private final LocalDate returnDate = LocalDate.now();
	private final Date returnDateSql = Date.valueOf(returnDate);

	public String fetchingFineAmount(Fine fine) {

		String result = "error";

		if (!loadUserFromSession(fine)) {
			System.out.println("User not logged in. Cannot pay fine.");
			return result = "error";
		}

		try (Connection connection = DatabaseService.getInstance().getConnection();
				Jedis redisClient = RedisService.getInstance().getClient()) {

			if (fine.getFine() > 0) {
				result = handleFinePayment(connection, redisClient, fine);
			}

		} catch (Exception e) {
			System.out.println("Error occurred: " + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

	private boolean loadUserFromSession(Fine fine) {

		Map<String, Object> session = ActionContext.getContext().getSession();

		Integer studentId = (Integer) session.get("studentId");

		if (studentId == null) {
			return false;
		}

		fine.setStudentId(studentId);

		System.out.println("Session loaded successfully: studentId = " + fine.getStudentId());
		return true;
	}

	private String handleFinePayment(Connection connection, Jedis redisClient, Fine fine) {

		String result = "error";

		String getFineAmountQuery = "SELECT Fine_amount FROM fine WHERE Student_id = ? AND Book_id = ?";
		String payFineAmountQuery = "UPDATE fine SET Fine_amount = Fine_amount - ? WHERE Student_id = ? AND Book_id = ?";
		String deleteFineWhenZeroQuery = "DELETE FROM fine WHERE Fine_amount = 0 AND Student_id = ? AND Book_id = ?";
		String setBorrowBookDatesNullQuery = "UPDATE Borrowbooks SET borrow_date = NULL, due_date = NULL "
				+ "WHERE Student_id = ? AND Book_id = ?";
		String deleteBorrowBookDetailsQuery = "DELETE FROM Borrowbooks WHERE Student_id = ? AND Book_id = ?";
		String increaseBookCopyQuery = "UPDATE Books SET Book_availablecopies = Book_availablecopies + 1 "
				+ "WHERE Book_id = ?";

		try (PreparedStatement getFineAmountStatement = connection.prepareStatement(getFineAmountQuery);
				PreparedStatement payFineAmountStatement = connection.prepareStatement(payFineAmountQuery);
				PreparedStatement deleteFineStatement = connection.prepareStatement(deleteFineWhenZeroQuery);
				PreparedStatement clearBorrowBookDatesStatement = connection.prepareStatement(setBorrowBookDatesNullQuery);
				PreparedStatement deleteBorrowStatement = connection.prepareStatement(deleteBorrowBookDetailsQuery);
				PreparedStatement increaseBookCopyStatement = connection.prepareStatement(increaseBookCopyQuery)) {

			getFineAmountStatement.setInt(1, fine.getStudentId());
			getFineAmountStatement.setInt(2, fine.getBookId());

			try (ResultSet getFineAmountResultSet = getFineAmountStatement.executeQuery()) {

				if (getFineAmountResultSet.next()) {

					int fineAmount = getFineAmountResultSet.getInt("Fine_amount");

					if (fine.getFine() == fineAmount) {

						payFine(payFineAmountStatement, fine);
						deleteFine(deleteFineStatement, fine);
						clearBorrowDates(clearBorrowBookDatesStatement, fine);
						deleteBorrowRecord(deleteBorrowStatement, fine);
						updateBookRecord(increaseBookCopyStatement, fine);
						updateReturnDateInBorrowList(connection, fine);
						clearRedisData(redisClient, fine);

						result = "success";

					} else {
						System.out.println("Incorrect fine amount entered by user.");
					}
				}
			}

		} catch (Exception e) {
			System.out.println("Error paying fine: " + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

	private void payFine(PreparedStatement payFineAmountStatement, Fine fine) throws Exception {

		payFineAmountStatement.setInt(1, (int) fine.getFine());
		payFineAmountStatement.setInt(2, fine.getStudentId());
		payFineAmountStatement.setInt(3, fine.getBookId());

		System.out.println("Executing payFine...");
		payFineAmountStatement.executeUpdate();
	}

	private void deleteFine(PreparedStatement deleteFineStatement, Fine fine) throws Exception {

		deleteFineStatement.setInt(1, fine.getStudentId());
		deleteFineStatement.setInt(2, fine.getBookId());

		System.out.println("Executing deleteFine...");
		deleteFineStatement.executeUpdate();
	}

	private void clearBorrowDates(PreparedStatement clearBorrowBookDatesStatement, Fine fine) throws Exception {

		clearBorrowBookDatesStatement.setInt(1, fine.getStudentId());
		clearBorrowBookDatesStatement.setInt(2, fine.getBookId());

		System.out.println("Clearing borrow/due dates...");
		clearBorrowBookDatesStatement.executeUpdate();
	}

	private void deleteBorrowRecord(PreparedStatement deleteBorrowStatement, Fine fine) throws Exception {

		deleteBorrowStatement.setInt(1, fine.getStudentId());
		deleteBorrowStatement.setInt(2, fine.getBookId());

		System.out.println("Deleting borrow record...");
		deleteBorrowStatement.executeUpdate();
	}

	private void updateBookRecord(PreparedStatement increaseBookCopyStatement, Fine fine) throws Exception {

		increaseBookCopyStatement.setInt(1, fine.getBookId());

		System.out.println("Updating book record (available copies incremented)...");
		increaseBookCopyStatement.executeUpdate();
	}

	private void updateReturnDateInBorrowList(Connection connection, Fine fine) throws SQLException {

		String setReturnDateInBorrowedListQuery = "UPDATE borrowedlist SET Return_date = ? " 
		                                              + "WHERE Student_id = ? AND Book_id = ?";

		try (PreparedStatement setReturnDateStatement = connection.prepareStatement(setReturnDateInBorrowedListQuery)) {

			setReturnDateStatement.setDate(1, returnDateSql);
			setReturnDateStatement.setInt(2, fine.getStudentId());
			setReturnDateStatement.setInt(3, fine.getBookId());

			setReturnDateStatement.executeUpdate();
			System.out.println("Borrow list updated with return date.");
		}
	}

	private void clearRedisData(Jedis redisClient, Fine fine) {

		if (redisClient == null) {
			System.out.println("Redis client not available.");
			return;
		}

		String bookDetailsCacheKey = "bookId:" + fine.getBookId();
		
		String borrowedBookDetailsCacheKey = "studentId:" + fine.getStudentId() + ":bookId:" + fine.getBookId();

		String dueDate = redisClient.hget(borrowedBookDetailsCacheKey, "dueDate");

		String borrowRecordCacheKey = "studentId:" + fine.getStudentId() + ":bookId:" + fine.getBookId()
				+ ":bookIdBorrowList:" + dueDate;

		try {
			
			System.out.println("Redis key exists? " + redisClient.exists(borrowedBookDetailsCacheKey));

			redisClient.hincrBy(bookDetailsCacheKey, "bookAvailableCopies", 1);

			redisClient.hset(borrowRecordCacheKey, "returnDate",
					String.valueOf(new java.sql.Date(System.currentTimeMillis())));

			redisClient.del(borrowedBookDetailsCacheKey);

			System.out.println("Fine-related data cleared from Redis.");

		} catch (Exception e) {
			System.out.println("Redis operation failed: " + e.getMessage());
		}
	}
}
