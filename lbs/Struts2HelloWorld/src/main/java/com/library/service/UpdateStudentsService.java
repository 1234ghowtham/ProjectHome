package com.library.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.library.model.UpdateStudent;

import dbconnection.DatabaseService;
import redis.clients.jedis.Jedis;
import redisconnection.RedisService;

public class UpdateStudentsService {

	public String updateStudent(UpdateStudent updateStudent) {

		final String insertingStudentDetailsQuery = "INSERT INTO Students " + "(Student_id, Student_password, Student_name) "
				+ "VALUES (?, ?, ?)";

		final String studentDetailsCacheKey = buildStudentDetailsCacheKey(updateStudent);

		if (!isStudentNameFormatIsValid(updateStudent.getStudentName())) {
			System.out.println("Student name format is incorrect.");
			return "error";
		}

		try (Connection connection = DatabaseService.getInstance().getConnection();

				PreparedStatement insertingStudentDetailsStatement = connection
						.prepareStatement(insertingStudentDetailsQuery);

				Jedis redisClient = RedisService.getInstance().getClient()) {

			updateStudentDetailsToDataBase(insertingStudentDetailsStatement, updateStudent);

			int studentDetailsInsertedCount = insertingStudentDetailsStatement.executeUpdate();

			if (studentDetailsInsertedCount > 0) {

				System.out.println("Student details inserted successfully into DB.");

				updateStudentDetailsInCache(redisClient, studentDetailsCacheKey, updateStudent);

				return "success";

			} else {

				System.out.println("Student details insertion failed.");
				return "error";
			}

		} catch (Exception e) {
			e.printStackTrace();
			return "error";
		}
	}

	private String updateStudentDetailsToDataBase(PreparedStatement insertingStudentDetailsStatement,
			UpdateStudent updateStudent) throws SQLException {

		String result = "error";

		if (updateStudent.getStudentId() > 0) {
			insertingStudentDetailsStatement.setInt(1, updateStudent.getStudentId());
		} else {
			return result;
		}

		insertingStudentDetailsStatement.setString(2, updateStudent.getStudentPassword());
		insertingStudentDetailsStatement.setString(3, updateStudent.getStudentName());

		System.out.println("Student Details Updated Successfully In DB.");

		return result = "success";
	}

	private boolean isStudentNameFormatIsValid(String studentName) {

		if (studentName == null || studentName.isEmpty()) {
			return false;
		}

		for (char ch : studentName.toCharArray()) {
			if (!Character.isLetter(ch)) {
				return false;
			}
		}

		System.out.println("Student name format is correct.");

		return true;
	}

	private void updateStudentDetailsInCache(Jedis redisClient, String redisKey, UpdateStudent updateStudent) {

		if (redisClient == null) {
			System.out.println("Redis client not available, skipping cache.");
			return;
		}

		try {

			redisClient.hset(redisKey, "studentId", String.valueOf(updateStudent.getStudentId()));
			redisClient.hset(redisKey, "studentPassword", updateStudent.getStudentPassword());
			redisClient.hset(redisKey, "studentName", updateStudent.getStudentName());

			redisClient.expire(redisKey, 3600);

			System.out.println("Student cached in Redis.");

		} catch (Exception e) {
			System.out.println("Redis caching failed!");
			e.printStackTrace();
		}
	}

	private String buildStudentDetailsCacheKey(UpdateStudent uptstu) {

		return "studentId:" + uptstu.getStudentId();
	}
}
