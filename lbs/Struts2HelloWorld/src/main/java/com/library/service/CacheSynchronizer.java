package com.library.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import redis.clients.jedis.Jedis;

public class CacheSynchronizer {

	public void syncBookDetailsToCache(Jedis redisService, Connection connection) {

		String getBookDetailsQuery = "select * from Books";

		try (PreparedStatement getBookDetailsStatement = connection.prepareStatement(getBookDetailsQuery);
				ResultSet getBookDetailsResultSet = getBookDetailsStatement.executeQuery()) {

			while (getBookDetailsResultSet.next()) {

				int bookId = getBookDetailsResultSet.getInt("Book_id");
				int bookTotalCopies = getBookDetailsResultSet.getInt("Book_totalcopies");
				int bookAvailableCopies = getBookDetailsResultSet.getInt("Book_availablecopies");
				String bookTitle = getBookDetailsResultSet.getString("Book_title");

				String bookDetailsCacheKey = "bookId:" + bookId;

				redisService.hset(bookDetailsCacheKey, "bookId", String.valueOf(bookId));
				redisService.hset(bookDetailsCacheKey, "bookTitle", bookTitle);
				redisService.hset(bookDetailsCacheKey, "bookTotalCopies", String.valueOf(bookTotalCopies));
				redisService.hset(bookDetailsCacheKey, "bookAvailableCopies", String.valueOf(bookAvailableCopies));

				redisService.expire(bookDetailsCacheKey, 3600);
			}

			System.out.println("Redis catched book deatils.");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void syncStudentDetailsToCache(Jedis redisService, Connection connection) {

		String getStudentDetailsQuery = "select * from Students";

		try (PreparedStatement getStudentDetailsStatement = connection.prepareStatement(getStudentDetailsQuery);
				ResultSet getStudentDetailsResultSet = getStudentDetailsStatement.executeQuery()) {

			while (getStudentDetailsResultSet.next()) {

				int studentId = getStudentDetailsResultSet.getInt("Student_id");
				String studentPassword = getStudentDetailsResultSet.getString("Student_password");
				String studentName = getStudentDetailsResultSet.getString("Student_name");

				String studentDetailsCacheKey = "studentId:" + studentId;

				redisService.hset(studentDetailsCacheKey, "studentId", String.valueOf(studentId));
				redisService.hset(studentDetailsCacheKey, "studentPassword", studentPassword);
				redisService.hset(studentDetailsCacheKey, "studentName", studentName);

				redisService.expire(studentDetailsCacheKey, 3600);
			}

			System.out.println("Redis catched student details");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void syncStudentBorrowedBookDetailsToRedis(Jedis redisService, Connection connection) {

		String getStudentBorrowedBookDetailsQuery = "select * from borrowbooks";

		try (PreparedStatement getStudentBorrowedBookDetailsStatement = connection
				.prepareStatement(getStudentBorrowedBookDetailsQuery);
				ResultSet getStudentBorrowedBookDetailsResultSet = getStudentBorrowedBookDetailsStatement
						.executeQuery()) {

			while (getStudentBorrowedBookDetailsResultSet.next()) {

				int studentId = getStudentBorrowedBookDetailsResultSet.getInt("Student_id");
				int bookId = getStudentBorrowedBookDetailsResultSet.getInt("book_id");
				int borrowBookCopies = getStudentBorrowedBookDetailsResultSet.getInt("Copies_borrowed");

				Date borrowDate = getStudentBorrowedBookDetailsResultSet.getDate("Borrow_date");
				Date dueDate = getStudentBorrowedBookDetailsResultSet.getDate("Due_date");

				String borrowDateStr = borrowDate.toString();
				String dueDateStr = dueDate.toString();

				String studentBorrowedBookDetailsCacheKey = "studentId:" + studentId + ":bookId:" + bookId;

				redisService.hset(studentBorrowedBookDetailsCacheKey, "studentId", String.valueOf(studentId));
				redisService.hset(studentBorrowedBookDetailsCacheKey, "bookId", String.valueOf(bookId));
				redisService.hset(studentBorrowedBookDetailsCacheKey, "borrowedCopies", String.valueOf(borrowBookCopies));
				redisService.hset(studentBorrowedBookDetailsCacheKey, "borrowDate", borrowDateStr);
				redisService.hset(studentBorrowedBookDetailsCacheKey, "dueDate", dueDateStr);
			}

			System.out.println("Redis catched borrow details...");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void syncStudentFineDetailsToRedis(Jedis redisService, Connection connection) {

		String getStudentFineDetailsQuery = "select * from fine";

		try (PreparedStatement getStudentFineDetailsStatement = connection.prepareStatement(getStudentFineDetailsQuery);
				ResultSet getStudentFineDetailsResultSet = getStudentFineDetailsStatement.executeQuery()) {

			while (getStudentFineDetailsResultSet.next()) {

				int studentId = getStudentFineDetailsResultSet.getInt("Student_id");
				int bookId = getStudentFineDetailsResultSet.getInt("Book_id");
				int fine = getStudentFineDetailsResultSet.getInt("Fine_amount");

				String studentFineDetailsCacheKey = "studentId:" + studentId + ":bookId:" + bookId;

				redisService.hset(studentFineDetailsCacheKey, "fine", String.valueOf(fine));
			}

			System.out.println("Redis catched fine details...");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void syncAllBorrowRecordsToRedis(Jedis redisService, Connection connection) {

		String getAllBorrowedRecordsQuery = "SELECT * from Borrowedlist";

		try (PreparedStatement getAllBorrowedRecordsStatement = connection.prepareStatement(getAllBorrowedRecordsQuery);
				ResultSet getAllBorrowedRecordsResultSet = getAllBorrowedRecordsStatement.executeQuery()) {

			while (getAllBorrowedRecordsResultSet.next()) {

				int studentId = getAllBorrowedRecordsResultSet.getInt("Student_id");
				int bookId = getAllBorrowedRecordsResultSet.getInt("book_id");

				Date borrowDate = getAllBorrowedRecordsResultSet.getDate("Borrow_date");
				Date dueDate = getAllBorrowedRecordsResultSet.getDate("Due_date");
				Date returnDate = getAllBorrowedRecordsResultSet.getDate("Return_date");

				String borrowDateStr = borrowDate.toString();
				String dueDateStr = dueDate.toString();
				String returnDateStr = returnDate.toString();

				String borrowedRecordCacheKey = "studentId:" + studentId + ":bookId:" + bookId + ":bookIdBorrowList:"
						+ dueDate;

				redisService.hset(borrowedRecordCacheKey, "studentId", String.valueOf(studentId));
				redisService.hset(borrowedRecordCacheKey, "bookId", String.valueOf(bookId));
				redisService.hset(borrowedRecordCacheKey, "borrowDate", borrowDateStr);
				redisService.hset(borrowedRecordCacheKey, "dueDate", dueDateStr);
				redisService.hset(borrowedRecordCacheKey, "returnDate", returnDate != null ? returnDateStr : "null");
			}

			System.out.println("Redis catched borrowed list...");

		} catch (NullPointerException e) {
			System.out.println("Return date is null");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
