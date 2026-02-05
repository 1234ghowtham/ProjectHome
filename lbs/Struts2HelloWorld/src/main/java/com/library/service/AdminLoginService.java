package com.library.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import com.library.model.AdminLogin;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redisconnection.RedisService;

public class AdminLoginService {

	public String authenticateAdmin(AdminLogin adminLoginRequest) {

		String result = "error";
		String authenticationCacheKey = buildAuthenticationCacheKey(adminLoginRequest.getAdminId(),
				adminLoginRequest.getAdminPassword());

		try (Jedis redisClient = RedisService.getInstance().getClient();
				Connection connection = DatabaseService.getInstance().getConnection()) {

			if (isAuthenticatedFromCache(redisClient, authenticationCacheKey, connection)) {
				return result = "success";
			}

			if (isAuthenticatedFromDatabase(redisClient, connection, authenticationCacheKey, adminLoginRequest)) {
				return result = "success";
			}

			System.out.println("Authentication Failed...");

		} catch (Exception e) {
			e.printStackTrace();
			result = "error";
		}

		return result;
	}

	private String buildAuthenticationCacheKey(int adminId, String adminPassword) {
		return "login:" + adminId + ":" + adminPassword;
	}

	private boolean isAuthenticatedFromCache(Jedis redisClient, String authenticationCacheKey, Connection connection) {

		if (redisClient != null) {
			
			try {
				
				if (redisClient.exists(authenticationCacheKey)) {
					
					hasFine(connection, redisClient);
					System.out.println("Redis cache hit. Name: " + redisClient.get(authenticationCacheKey));
					System.out.println("Admin Authenticated from cache...");
					return true;
				}
				
			} catch (Exception e) {
				System.out.println("Redis access failed. Ignoring Redis. " + e.getMessage());
			}
		}

		return false;
	}

	private boolean isAuthenticatedFromDatabase(Jedis redisClient, Connection connection, String authenticationCacheKey,
			AdminLogin admin) {

		String checkAdminCredentialsQuery = "SELECT Admin_name FROM Admins "
				+ "WHERE Admin_id = ? AND Admin_password = ?";

		try (PreparedStatement checkAdminCredentialsStatement = connection
				.prepareStatement(checkAdminCredentialsQuery)) {

			checkAdminCredentialsStatement.setInt(1, admin.getAdminId());
			checkAdminCredentialsStatement.setString(2, admin.getAdminPassword());

			System.out.println("Executing authentication query...");

			try (ResultSet resultSet = checkAdminCredentialsStatement.executeQuery()) {

				System.out.println("Entered into Admin Interface");

				if (resultSet.next()) {
					
					admin.setAdminName(resultSet.getString("Admin_name"));
					System.out.println("Admin Authenticated from DB...");

					if (hasFine(connection, redisClient)) {
						
						updateRedisCache(redisClient, authenticationCacheKey, connection, admin.getAdminName());
						return true;
					}
					
				}
			}

		} catch (Exception e) {
			System.out.println("DB Authentication failed: " + e.getMessage());
		}

		return false;
	}

	private void updateRedisCache(Jedis redisClient, String authenticationCacheKey, Connection connection, String adminName) {

		if (redisClient != null) {
			
			try {
				
				CacheSynchronizer sync = new CacheSynchronizer();

				redisClient.setex(authenticationCacheKey, 3600, adminName);

				sync.syncBookDetailsToCache(redisClient, connection);
				sync.syncStudentDetailsToCache(redisClient, connection);
				sync.syncStudentBorrowedBookDetailsToRedis(redisClient, connection);
				sync.syncStudentFineDetailsToRedis(redisClient, connection);
				sync.syncAllBorrowRecordsToRedis(redisClient, connection);

				System.out.println("Redis updated from DB");

			} catch (Exception e) {
				System.err.println("Redis cache update failed: " + e.getMessage());
			}
		}
	}
	private boolean hasFine(Connection connection, Jedis redisClient) throws Exception {

		String getBorrowedDetailsQuery = "SELECT * FROM Borrowbooks";
		String getFineDetailsQuery = "Select * from Fine Where Student_id = ? and Book_id = ?";
		String insertFineDetailsQuery = "INSERT INTO Fine VALUES (?, ?, ?)";
		String updateFineDetailsQuery = "UPDATE Fine SET fine_amount = ? Where Student_id = ? and Book_id = ?";

		try (PreparedStatement getBorrowedDetailsStatement = connection.prepareStatement(getBorrowedDetailsQuery);
				ResultSet getBorrowedDetailsResultSet = getBorrowedDetailsStatement.executeQuery();
				PreparedStatement getFineDetailsStatement = connection.prepareStatement(getFineDetailsQuery);) {

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
									updateFineDetailsStatement.setInt(2, getBorrowedDetailsResultSet.getInt("Student_id"));
									updateFineDetailsStatement.setInt(3, getBorrowedDetailsResultSet.getInt("Book_id"));
									
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
			}catch (SQLException | DateTimeException e) {
				e.printStackTrace();
				return false;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		
		return true;
		}
	}
