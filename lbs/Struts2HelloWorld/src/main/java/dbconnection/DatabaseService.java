package dbconnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//Bill Pugh Singleton
public class DatabaseService {
	
	private static final String URL = "jdbc:postgresql://localhost:5432/struts_tutorial";
	private static final String NAME = "postgres";
	private static final String PASSWORD = "376788";

	private DatabaseService() {
		try {
			Class.forName("org.postgresql.Driver");
			System.out.println("Successfully to connect to DB.");
		} catch (ClassNotFoundException e) {
			System.out.println("Failed to connect to DB: " + e.getMessage());
		}
	}

	private static class holder {
		private static final DatabaseService instance = new DatabaseService();
	}

	public static DatabaseService getInstance() {
		return holder.instance;
	}

	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(URL, NAME, PASSWORD);
	}
}
