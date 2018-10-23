package Persistence;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectUtility {

	public Connection getConnection() throws IOException, ClassNotFoundException, SQLException {
		FileInputStream fis = new FileInputStream("jdbcPersistence.properties");
		Properties p = new Properties();
		p.load(fis);
		String dname = (String) p.get("Dname");
		String url = (String) p.get("URL");
		String username = (String) p.get("Uname");
		String password = (String) p.get("password");
		Class.forName(dname);
		Connection con = DriverManager.getConnection(url, username, password);
		return con;
	}
}
