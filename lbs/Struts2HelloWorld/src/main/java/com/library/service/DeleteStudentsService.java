package com.library.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.library.model.DeleteStudent;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redisconnection.RedisService;

public class DeleteStudentsService {

	public String deleteStudents(DeleteStudent deleteStudent) {

		String result = "error";
		String studentDetailsCacheKey = buildStudentDetailsCacheKey(deleteStudent);

		try (Jedis redisClient = RedisService.getInstance().getClient();
				Connection connection = DatabaseService.getInstance().getConnection()) {

			System.out.println("Deleting StudentId: " + deleteStudent.getStudentId());
			
			if (hasUnreturnedBooks(connection, deleteStudent)) {
				return result;
			}

			if (isStudentDeletedFromDataBase(connection, deleteStudent)) {
				
				deleteStudentFromRedis(redisClient, studentDetailsCacheKey, deleteStudent);
				result = "success";
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = "error";
		}

		return result;
	}

	private String buildStudentDetailsCacheKey(DeleteStudent deleteStudent) {
		return "studentId:" + deleteStudent.getStudentId();
	}

	private boolean hasUnreturnedBooks(Connection connection, DeleteStudent deleteStudent) throws SQLException {

		String getBorrowBookDetailsQuery = "SELECT * FROM Borrowbooks WHERE Student_id = ? AND Copies_borrowed > 0";

		try (PreparedStatement borrowBookDetailsStatement = connection.prepareStatement(getBorrowBookDetailsQuery)) {

			borrowBookDetailsStatement.setInt(1, deleteStudent.getStudentId());
			System.out.println("Executing query (checkBorrowSql)...");

			try (ResultSet borrowBookDetailsResultSet = borrowBookDetailsStatement.executeQuery()) {

				if (borrowBookDetailsResultSet.next()) {
					System.out.println(
							"StudentId: " + deleteStudent.getStudentId() + " has unreturned books and cannot be deleted.");
					return true;
				}
			}

		} catch (Exception e) {
			System.out.println("Error checking borrowed books: " + e.getMessage());
		}

		return false;
	}

	private boolean isStudentDeletedFromDataBase(Connection connection, DeleteStudent deleteStudent) throws SQLException {

		String deleteStudentByStudentIdQuery = "DELETE FROM students WHERE student_id = ?";

		try (PreparedStatement deleteStudentStatement = connection.prepareStatement(deleteStudentByStudentIdQuery)) {

			deleteStudentStatement.setInt(1, deleteStudent.getStudentId());
			int studentDeletedCount = deleteStudentStatement.executeUpdate();
			
			System.out.println("Executed query (deleteStudentSql)...");

			if (studentDeletedCount > 0) {

				System.out.println("Student deleted successfully in DB, StudentId: " + deleteStudent.getStudentId());
				return true;
				
			} else {
				System.out.println("No student record found for StudentId: " + deleteStudent.getStudentId());
			}

		} catch (Exception e) {
			System.out.println("Error deleting student from DB: " + e.getMessage());
		}

		return false;
	}

	private void deleteStudentFromRedis(Jedis redisClient, String redisKey, DeleteStudent deleteStudent) {

		if (redisClient != null) {
			
			try {
				
				if (redisClient.exists(redisKey)) {
					
					redisClient.del(redisKey);
					System.out.println("Student deleted successfully in Redis, StudentId: " + deleteStudent.getStudentId());
					
				} else {
					System.out.println("No student found in Redis for StudentId: " + deleteStudent.getStudentId());
				}
				
			} catch (Exception e) {
				System.out.println("Error deleting student from Redis: " + e.getMessage());
			}
			
		} else {
			System.out.println("Redis client not available!");
		}
	}
}
