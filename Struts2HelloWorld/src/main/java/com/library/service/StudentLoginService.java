package com.library.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.library.model.StudentLogin;
import com.opensymphony.xwork2.ActionContext;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redisconnection.RedisService;

public class StudentLoginService {

	public String authenticateStudent(StudentLogin studentLogin) {

		String result = "error";

		try (Jedis redisClient = RedisService.getInstance().getClient();
				Connection connection = DatabaseService.getInstance().getConnection()) {

			if (isAuthenticatedFromCache(redisClient, connection, studentLogin)) {
				return result = "success";
			}

			if (isAuthenticatedFromDataBase(connection, redisClient, studentLogin)) {

				cacheStudentDetailsAfterLogin(redisClient, connection, studentLogin);
				return result = "success";
			}

		} catch (Exception e) {
			System.out.println("Exception during login: " + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

	private boolean isAuthenticatedFromCache(Jedis redisClient, Connection connection, StudentLogin studentLogin) {

		String studentLoginDetailsCacheKey = buildStudentLoginCacheKey(studentLogin);

		try {
			
			if (redisClient != null && redisClient.exists(studentLoginDetailsCacheKey)) {

				System.out.println("Redis cache hit. Name: " + redisClient.get(studentLoginDetailsCacheKey));

				setSession(studentLogin);
				hasFine(connection, redisClient, studentLogin);
				System.out.println("Student login validated from cache.");
				return true;
			}
			
		} catch (Exception e) {
			System.out.println("Redis access failed: " + e.getMessage());
		}

		return false;
	}

	private boolean isAuthenticatedFromDataBase(Connection connection, Jedis redisClient, StudentLogin studentLogin) {

		String getStudentNameQuery = "SELECT Student_name FROM Students " + "WHERE Student_id = ? AND Student_password = ?";

		try (PreparedStatement getStudentNameStatement = connection.prepareStatement(getStudentNameQuery)) {

			getStudentNameStatement.setInt(1, studentLogin.getStudentId());
			getStudentNameStatement.setString(2, studentLogin.getStudentPassword());

			try (ResultSet getStudentNameResultset = getStudentNameStatement.executeQuery()) {

				if (getStudentNameResultset.next()) {

					studentLogin.setStudentName(getStudentNameResultset.getString("Student_name"));
					setSession(studentLogin);
					
					hasFine(connection, redisClient, studentLogin);
					System.out.println("Student login validated from DB.");
					return true;
				}
			}

		} catch (Exception e) {
			System.out.println("DB validation failed: " + e.getMessage());
		}

		return false;
	}

	private void cacheStudentDetailsAfterLogin(Jedis redisClient, Connection connection, StudentLogin studentLogin) {

		if (redisClient == null) {
			return;
		}

		try {

			String studentLoginCacheKey = buildStudentLoginCacheKey(studentLogin);

			redisClient.setex(studentLoginCacheKey, 3600, studentLogin.getStudentName());

			CacheSynchronizer sync = new CacheSynchronizer();

			sync.syncBookDetailsToCache(redisClient, connection);
			sync.syncStudentDetailsToCache(redisClient, connection);
			sync.syncStudentBorrowedBookDetailsToRedis(redisClient, connection);
			sync.syncStudentFineDetailsToRedis(redisClient, connection);

			System.out.println("Redis cache updated from DB.");

		} catch (Exception e) {
			System.err.println("Failed to update Redis: " + e.getMessage());
		}
	}

	private void setSession(StudentLogin studentLogin) {

		Map<String, Object> session = ActionContext.getContext().getSession();
		session.put("studentId", studentLogin.getStudentId());

		System.out.println("Session updated: studentId = " + studentLogin.getStudentId());
	}

	private String buildStudentLoginCacheKey(StudentLogin stulogin) {
		return "login:" + stulogin.getStudentId() + ":" + stulogin.getStudentPassword();
	}
	
	private boolean hasFine(Connection connection, Jedis redisClient, StudentLogin studentLogin) throws Exception {

		String getBorrowedDetailsQuery = "SELECT * FROM Borrowbooks Where Student_id = ?";
		String getFineDetailsQuery = "Select * from Fine Where Student_id = ? and Book_id = ?";
		String insertFineDetailsQuery = "INSERT INTO Fine(student_id,book_id,fine_amount) VALUES (?, ?, ?)";
		String updateFineDetailsQuery = "UPDATE Fine SET fine_amount = ? Where Student_id = ? and Book_id = ?";

		try (PreparedStatement getBorrowedDetailsStatement = connection.prepareStatement(getBorrowedDetailsQuery);
				PreparedStatement getFineDetailsStatement = connection.prepareStatement(getFineDetailsQuery);) {
			
			getBorrowedDetailsStatement.setInt(1, studentLogin.getStudentId());

			try(ResultSet getBorrowedDetailsResultSet = getBorrowedDetailsStatement.executeQuery())
			{
				
				while (getBorrowedDetailsResultSet.next()) {

					Date dueDate = getBorrowedDetailsResultSet.getDate("Due_date");
					Date currentDate = Date.valueOf(LocalDate.now());

					if (currentDate.after(dueDate)) {

						long diffMs = currentDate.getTime() - dueDate.getTime();
						int diffDays = (int) TimeUnit.MILLISECONDS.toDays(diffMs);
						int fine = diffDays * 10;

						int getStudentId = getBorrowedDetailsResultSet.getInt("Student_id");
						int getBookId = getBorrowedDetailsResultSet.getInt("Book_id");
						
						getFineDetailsStatement.setInt(1, getStudentId);
						getFineDetailsStatement.setInt(2, getBookId);
						
						try (ResultSet getFineDetailsResultSet = getFineDetailsStatement.executeQuery()) {
							
							if (!getFineDetailsResultSet.next()) {
								
								try (PreparedStatement insertFineDetailsStatement = connection
										.prepareStatement(insertFineDetailsQuery)) {

									insertFineDetailsStatement.setInt(1, getStudentId);
									insertFineDetailsStatement.setInt(2, getBookId);
									insertFineDetailsStatement.setInt(3, fine);

									insertFineDetailsStatement.executeUpdate();
									
									String studentFineDetailsCacheKey = "studentId:"
											+ getBorrowedDetailsResultSet.getInt("Student_id") + ":bookId:"
											+ getBorrowedDetailsResultSet.getInt("Book_id");

									redisClient.hset(studentFineDetailsCacheKey, "fine", String.valueOf(fine));
									System.out.println("Fine Amount inserted Successfully in Db and Redis");
								
								} 
								
							} else {
								
								try (PreparedStatement updateFineDetailsStatement = connection
										.prepareStatement(updateFineDetailsQuery)) {

									updateFineDetailsStatement.setInt(1, fine);
									updateFineDetailsStatement.setInt(2, getStudentId);
									updateFineDetailsStatement.setInt(3, getBookId);
									
									updateFineDetailsStatement.executeUpdate();
									
									String studentFineDetailsCacheKey = "studentId:"
											+ getBorrowedDetailsResultSet.getInt("Student_id") + ":bookId:"
											+ getBorrowedDetailsResultSet.getInt("Book_id");

									redisClient.hset(studentFineDetailsCacheKey, "fine", String.valueOf(fine));
									System.out.println("Fine Updated inserted Successfully in Db and Redis");
								} 
							}
						}
					}
				}
			} catch (JedisConnectionException e) {
				System.err.println("No Fine Amount inserted or updated in Redis");
			} catch (SQLException | DateTimeException e) {
				e.printStackTrace();
				return false;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			return true;
		}
	}
}
