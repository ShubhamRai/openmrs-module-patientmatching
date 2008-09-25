package org.regenstrief.linkage.io;

import java.beans.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.regenstrief.linkage.Record;
import org.regenstrief.linkage.util.DataColumn;
import org.regenstrief.linkage.util.LinkDataSource;

/**
 * Class implements storing Records in a database.
 * @author jegg
 *
 */

public class DataBaseRecordStore implements RecordStore {
	Connection db_connection;
	LinkDataSource lds;
	String table_name, driver, url, user, password;
	
	PreparedStatement insert_stmt;
	List<String> insert_demographics;
	
	String quote_string;
	int insert_count;
	
	public static final String UID_COLUMN = "import_uid";
	public static final String INVALID_COLUMN_CHARS = "\\W";
	
	/**
	 * 
	 * @param db	the database connection to create the table of Records
	 * @param lds	information on what fields the Records will have
	 * @param driver	
	 * @param url	
	 * @param user	
	 * @param password	
	 */
	public DataBaseRecordStore(Connection db, LinkDataSource lds, String driver, String url, String user, String password){
		db_connection = db;
		this.lds = lds;
		table_name = "import";
		this.driver = driver;
		this.url = url;
		this.user = user;
		this.password = password;
		
		insert_demographics = new ArrayList<String>();
		insert_count = 0;
		try{
			quote_string = db_connection.getMetaData().getIdentifierQuoteString();
		}
		catch(SQLException sqle){
			quote_string = "";
		}
		
		dropTableIfExists(table_name);
		createTable();
		insert_stmt = createInsertQuery();
		
	}
	
	protected boolean createTable(){
		String query_text = "CREATE TABLE " + table_name + "(" + UID_COLUMN + "\tbigint";
		
		// iteratoe over lds to see what fields to expect, and set order in insert_demographics
		Enumeration<String> e = lds.getIncludedDataColumns().keys();
		while(e.hasMoreElements()){
			String column = e.nextElement();
			
			insert_demographics.add(column);
			query_text += ", " + quote_string + column + quote_string + "\tvarchar(255)";

		}
		query_text += ")";
		
		try{
			db_connection.createStatement().execute(query_text);
		}
		catch(SQLException sqle){
			return false;
		}
		return true;
	}
	
	protected void dropTableIfExists(String table){
		try{
			db_connection.createStatement().execute("DROP TABLE " + table);
		}
		catch(SQLException sqle){
			
		}
	}
	
	protected PreparedStatement createInsertQuery(){
		PreparedStatement stmt = null;
		String insert_text = "INSERT INTO " + table_name;
		String column_clause = "(" + UID_COLUMN;
		String values_clause = "VALUES (?";
		for(int i = 0; i < insert_demographics.size(); i++){
			String demographic = insert_demographics.get(i);
			column_clause += ", " + quote_string + demographic + quote_string;
			values_clause += ", ?";
		}
		column_clause += ")";
		values_clause += ")";
		insert_text += column_clause + " " + values_clause;
		
		try{
			stmt = db_connection.prepareStatement(insert_text);
		}
		catch(SQLException sqle){
			return null;
		}
		return stmt;
	}
	
	/**
	 * Returns a LinkDataSource describing the connection parameters and fields that will be in a Reader
	 * created from the database table
	 */
	public LinkDataSource getRecordStoreLinkDataSource() {
		String access = driver + "," + url + "," + user + "," + password;
		LinkDataSource ret = new LinkDataSource(table_name, "DataBase", access, 0);
		ret.setUniqueID(UID_COLUMN);
        DataColumn dc = new DataColumn(UID_COLUMN);
        dc.setIncludePosition(0);
        dc.setName(UID_COLUMN);
        dc.setType(lds.getColumnTypeByName(UID_COLUMN));
        ret.addDataColumn(dc);
		for(int i = 0; i < insert_demographics.size(); i++){
			String demographic = insert_demographics.get(i);
			dc = new DataColumn(demographic);
			dc.setIncludePosition(i + 1);
			dc.setName(demographic);
			dc.setType(lds.getColumnTypeByName(demographic));
			ret.addDataColumn(dc);
		}
		return ret;
	}
	
	public Connection getRecordStoreConnection(){
		return db_connection;
	}

	/**
	 * Stores the Records in the database
	 */
	public boolean storeRecord(Record r) {
		try{
			insert_stmt.setInt(1, insert_count++);
			for(int i = 0; i < insert_demographics.size(); i++){
				String demographic = insert_demographics.get(i);
				insert_stmt.setString(i + 2, r.getDemographic(demographic));
			}
			return insert_stmt.executeUpdate() > 0;
		}
		catch(SQLException sqle){
			return false;
		}
	}

}
