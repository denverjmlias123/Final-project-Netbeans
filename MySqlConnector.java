package groupprojectexe;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnector {

    private static final String URL = "jdbc:mysql://localhost:3306/projectexe?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

        public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
            con.setAutoCommit(true);

            // REMOVE THIS LINE to stop console spam:
            // System.out.println("Database Connected Successfully!"); 
            
            return con;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL Driver not found. Add mysql-connector-j to libraries.");
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed: " + e.getMessage());
        }

    }
}