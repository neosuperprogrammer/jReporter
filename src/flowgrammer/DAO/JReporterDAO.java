package flowgrammer.DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import flowgrammer.FileUploader;
import flowgrammer.Model.Photo;
import flowgrammer.exception.JReporterException;


public class JReporterDAO {
	private static String jdbcUrl = "jdbc:mysql://localhost/servlet_test";
	private static String userId = "servletuser";
	private static String userPass = "servletpassword";
	
	static Logger log = Logger.getLogger(FileUploader.class);
	
	private static boolean checkJdbc() {
		boolean jdbcEnabled = true;
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
//			System.out.print("Class Not Found Exception" + e.getMessage());
			jdbcEnabled = false;
		}
		return jdbcEnabled;
	}
	
	public static boolean register(String id, String passwd) throws JReporterException
	{
		Connection conn = null;
		PreparedStatement insertStatement = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		if (!checkJdbc()) {
			throw new JReporterException("jdbc error!!!");
		}
		
		try {
			conn = DriverManager.getConnection(jdbcUrl, userId, userPass);
			
			String sql = "SELECT username FROM login WHERE username='" + id + "' limit 1";
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				throw new JReporterException("already registered!!!");
			}
			
			sql = "INSERT INTO login(username, pass) VALUES(?,?)";
			
			insertStatement = conn.prepareStatement(sql);
			insertStatement.setString(1, id);
			insertStatement.setString(2, passwd);
			int ret = insertStatement.executeUpdate();
//			int ret = stmt.executeUpdate(sql);
			if (ret == 0) {
				throw new JReporterException("insert failed!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.print("exception : " + e.getMessage());
			throw new JReporterException(e);
		} finally {
			if (rs != null) { try { rs.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (insertStatement != null) { try { insertStatement.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (stmt != null) { try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (conn != null) { try { conn.close(); } catch (SQLException e) { e.printStackTrace(); } }
		}
		return true;
	}
	
	public static int login(String id, String passwd)
	{
		Connection conn = null;
		Statement stmt = null;
		PreparedStatement insertStatement = null;
		ResultSet rs = null;
		
		if (!checkJdbc()) {
			throw new JReporterException("jdbc error!!!");
		}
		
		try {
			conn = DriverManager.getConnection(jdbcUrl, userId, userPass);
			
			String sql = "SELECT IdUser, username FROM login WHERE username=? AND pass=? limit 1";
			insertStatement = conn.prepareStatement(sql);
			insertStatement.setString(1, id);
			insertStatement.setString(2, passwd);
			rs = insertStatement.executeQuery();
			
			while (rs.next()) {
				int userId = rs.getInt("IdUser");
				String userName = rs.getString("username");
				return userId;
			}
			return -1;
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.print("exception : " + e.getMessage());
			return -1;
		} finally {
			if (rs != null) { try { rs.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (stmt != null) { try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (insertStatement != null) { try { insertStatement.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (conn != null) { try { conn.close(); } catch (SQLException e) { e.printStackTrace(); } }
		}
	}
	
	public static Connection getConnection() {
		Connection conn = null;
		if (!checkJdbc()) {
			throw new JReporterException("jdbc error!!!");
		}
		try {
			conn = DriverManager.getConnection(jdbcUrl, userId, userPass);
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("getConnection error : " + e.getMessage());
		}
		return conn;
	}

	public static int upload(Connection conn, int idUser, String title) {
		PreparedStatement insertStatement = null;
		Statement stmt = null;
		ResultSet rs = null;
		int photoId = -1;
		if (!checkJdbc()) {
			throw new JReporterException("jdbc error!!!");
		}
		
		try {
			String sql = "INSERT INTO photo(IdUser,title) VALUES(?,?)";
			
			insertStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			insertStatement.setInt(1, idUser);
			insertStatement.setString(2, title);
			int ret = insertStatement.executeUpdate();
			if (ret == 0) {
				throw new JReporterException("insert failed!");
			}

	        rs = insertStatement.getGeneratedKeys();
	        if (rs.next()){
	        	photoId = rs.getInt(1);
	        }
			
			System.out.println("photo id : " + photoId);
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.print("exception : " + e.getMessage());
			throw new JReporterException(e);
		} finally {
			if (rs != null) { try { rs.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (insertStatement != null) { try { insertStatement.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (stmt != null) { try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); } }
		}
		return photoId;
	}

	public static ArrayList<Photo> stream(int idUser, String idPhoto) {
		Connection conn = null;
		Statement stmt = null;
		PreparedStatement insertStatement = null;
		ResultSet rs = null;
		
		if (!checkJdbc()) {
			throw new JReporterException("jdbc error!!!");
		}

		try {
			conn = DriverManager.getConnection(jdbcUrl, userId, userPass);

			
			String sql;
			if (idPhoto == null || idPhoto.length() < 1) {
				sql = "SELECT IdPhoto, title, l.IdUser, username FROM photo p JOIN login l ON (l.IdUser = p.IdUser) ORDER BY IdPhoto DESC LIMIT 50";
				insertStatement = conn.prepareStatement(sql);
			}
			else {
				sql = "SELECT IdPhoto, title, l.IdUser, username FROM photo p JOIN login l ON (l.IdUser = p.IdUser) WHERE p.IdPhoto=? LIMIT 1";
				insertStatement = conn.prepareStatement(sql);
				insertStatement.setString(1, idPhoto);
			}
			
			rs = insertStatement.executeQuery();

			ArrayList<Photo> photoList = new ArrayList<Photo>();
			
			while (rs.next()) {
				Photo photo = new Photo();
				photo.IdPhoto = String.valueOf(rs.getInt("IdPhoto"));
				photo.title = rs.getString("title");
				photo.username = rs.getString("username");
				
				photoList.add(photo);
			}
			
			return photoList;
		} 
		catch (SQLException e) {
			e.printStackTrace();
			System.out.print("exception : " + e.getMessage());
			return null;
		} 
		finally {
			if (rs != null) { try { rs.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (stmt != null) { try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (insertStatement != null) { try { insertStatement.close(); } catch (SQLException e) { e.printStackTrace(); } }
			if (conn != null) { try { conn.close(); } catch (SQLException e) { e.printStackTrace(); } }
		}
	}
}
