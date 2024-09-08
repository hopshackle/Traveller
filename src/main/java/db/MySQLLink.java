package db;

import java.sql.*;


public class MySQLLink {

    private Connection connection;
    String database = "traveller";

    public MySQLLink() {
        // default to use traveller database
    }

    // constructor to use a different database (e.g. for testing)
    public MySQLLink(String database) {
        this.database = database;
    }

    // This creates a connection via JDBC to a MySQL database
    public Connection getConnection() {

        if (connection != null) {
            return connection;
        }
        // Code to connect to the database
        try {
            // This will load the MySQL driver, each DB has its own driver

            Class.forName("com.mysql.cj.jdbc.Driver");
            String password = System.getenv().get("MYSQL_PASSWORD");
            String user = System.getenv().get("MYSQL_USER");
            // Setup the connection with the DB
            connection = DriverManager.getConnection("jdbc:mysql://localhost/" + database, user, password);
            return connection;
        } catch (Error | SQLException e) {
            System.out.println("Connection Failed! Check output console");
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            System.out.println("Where is your MySQL JDBC Driver?");
            throw new RuntimeException(e);
        }
    }

    public void closeConnection() {
        if (connection == null) {
            throw new RuntimeException("Connection is already closed");
        }
        try {
            connection.close();
            connection = null;
        } catch (SQLException e) {
            e.printStackTrace();


        }
    }

    public static void main(String[] args) {
        MySQLLink mySQLLink = new MySQLLink();
        Connection connection = mySQLLink.getConnection();
        try {
            String update = "INSERT INTO systems (name, location, sector) VALUES ('Sol', '1827', 'Solomani Rim')";

            Statement statement = connection.createStatement();
            statement.executeUpdate(update);

            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
