package edu.virginia.cs;

import java.lang.System;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DocDb {
	private String mMysqlHost = null, 
		mMysqlUser = null,
		mMysqlPass = null,
		mMysqlDb = null;
	private boolean mIsConnected = false;
	private Connection mSqlConnection = null;
	private PreparedStatement mInsertPackageStmt = null;
	private PreparedStatement mInsertDocumentationStmt = null;
	private PreparedStatement mInsertSourceStmt = null;
	private PreparedStatement mGetPackageIdStmt = null;
	private PreparedStatement mGetSourceIdStmt = null;
	private PreparedStatement mUpdateSourceStmt = null;

	private static final String INSERT_PACKAGE_SQL = "INSERT INTO package (name, package_file_name, package_url) VALUES (?,?,?)";
	private static final String INSERT_DOCUMENTATION_SQL = "INSERT INTO documentation (package_id, source_id, documentation) VALUES (?,?,?)";
	private static final String INSERT_SOURCE_SQL = "INSERT INTO source (package_id, type, name, source) VALUES (?,?,?,?)";
	private static final String SELECT_PACKAGE_ID_SQL = "SELECT id FROM package WHERE name=?";
	private static final String SELECT_SOURCE_ID_SQL = "SELECT id FROM source WHERE package_id=? and name=?";
	private static final String UPDATE_SOURCE_SQL = "UPDATE source SET source=? WHERE package_id=? and id=?";

	public DocDb(String mysqlHost,
		String mysqlUser,
		String mysqlPass,
		String mysqlDb) {
		mMysqlHost = mysqlHost;
		mMysqlUser = mysqlUser;
		mMysqlPass = mysqlPass;
		mMysqlDb = mysqlDb;
	}

	public boolean connect() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			mSqlConnection = DriverManager.getConnection("jdbc:mysql://" +
				mMysqlHost + 
				"/" + mMysqlDb + 
				"?user=" + mMysqlUser + 
				"&password=" + mMysqlPass);
			mIsConnected = true;
		} catch (Exception e) {
			System.out.println("Could not connect to MySql database: "+e.toString());
			mSqlConnection = null;
		}
		if (mIsConnected) 
			prepareStatements();

		return mIsConnected;
	}

	private void prepareStatements() {
		try {
			mInsertPackageStmt = mSqlConnection.prepareStatement(
				INSERT_PACKAGE_SQL, 
				Statement.RETURN_GENERATED_KEYS);
			mInsertDocumentationStmt = mSqlConnection.prepareStatement(
				INSERT_DOCUMENTATION_SQL,
				Statement.RETURN_GENERATED_KEYS);
			mInsertSourceStmt = mSqlConnection.prepareStatement(
				INSERT_SOURCE_SQL,
				Statement.RETURN_GENERATED_KEYS);
			mGetPackageIdStmt = mSqlConnection.prepareStatement(
				SELECT_PACKAGE_ID_SQL);
			mGetSourceIdStmt = mSqlConnection.prepareStatement(
				SELECT_SOURCE_ID_SQL);
			mUpdateSourceStmt = mSqlConnection.prepareStatement(
				UPDATE_SOURCE_SQL);
		} catch (SQLException e) {
			mIsConnected = false;
		}
	}

	private int getAutoId(Statement stmt) {
		int id = -1;
		ResultSet rs = null;
		try {
			rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				id = rs.getInt(1);
			}

			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			System.err.println("SQL Warning: Could not fetch auto id: " + 
				e.toString());
		}
		return id;
	}

	public int getPackageIdFromName(String name) {
		if (!mIsConnected) return -1;
		try {
			ResultSet idRs = null;
			int id = -1;

			mGetPackageIdStmt.clearParameters();
			mGetPackageIdStmt.setString(1, name);

			mGetPackageIdStmt.execute();

			idRs = mGetPackageIdStmt.getResultSet();
			if (idRs.next()) {
				id = idRs.getInt(1);
			}

			if (idRs != null) {
				idRs.close();
			}

			return id;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public int getSourceIdFromName(int packageId, String name) {
		if (!mIsConnected) return -1;
		try {
			ResultSet idRs = null;
			int id = -1;

			mGetSourceIdStmt.clearParameters();
			mGetSourceIdStmt.setInt(1, packageId);
			mGetSourceIdStmt.setString(2, name);

			mGetSourceIdStmt.execute();

			idRs = mGetSourceIdStmt.getResultSet();
			if (idRs.next()) {
				id = idRs.getInt(1);
			}

			if (idRs != null) {
				idRs.close();
			}

			return id;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public int addPackage(String name, String filename, String url) {
		if (!mIsConnected) return -1;
		try {
			ResultSet aiRs = null;
			int key = -1;

			mInsertPackageStmt.clearParameters();
			mInsertPackageStmt.setString(1, name);
			mInsertPackageStmt.setString(2, filename);
			mInsertPackageStmt.setString(3, url);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mInsertPackageStmt.execute();

			return getAutoId(mInsertPackageStmt);
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public int addSource(int packageId, String type, String name, String code) {
		if (!mIsConnected) return -1;
		try {
			mInsertSourceStmt.clearParameters();
			mInsertSourceStmt.setInt(1, packageId);
			mInsertSourceStmt.setString(2, type);
			mInsertSourceStmt.setString(3, name);
			mInsertSourceStmt.setString(4, code);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mInsertSourceStmt.execute();

			return getAutoId(mInsertSourceStmt);
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public boolean updateSource(int packageId, int sourceId, String source) {
		if (!mIsConnected) return false;
		try {
			mUpdateSourceStmt.clearParameters();
			mUpdateSourceStmt.setString(1, source);
			mUpdateSourceStmt.setInt(2, packageId);
			mUpdateSourceStmt.setInt(3, sourceId);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mUpdateSourceStmt.execute();

			return true;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return false;
		}
	}

	public int addDocumentation(int packageId, int sourceId, String doc) {
		if (!mIsConnected) return -1;

		try {
			mInsertDocumentationStmt.clearParameters();
			mInsertDocumentationStmt.setInt(1, packageId);
			mInsertDocumentationStmt.setInt(2, sourceId);
			mInsertDocumentationStmt.setString(3, doc);

			mInsertDocumentationStmt.execute();

			return getAutoId(mInsertDocumentationStmt);
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public boolean disconnect() {
		if (mIsConnected) {
			try {
				mSqlConnection.close();
				mIsConnected = false;
				return true;
			} catch (SQLException e) {
				System.out.println("Error occurred disconnecting from DocDb: " + 
					e.toString());
				return false;
			}
		}
		return true;
	}
}