/*
	Jon de Bruijn.
	2020-04-24
	This is a generic class for creating a database connection to a MySQL database.
	DB username and password are requested and retrieved by user-terminal input.
*/

package borehole;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import java.util.logging.Logger;


public class DatabaseHelper
{
	private static final String class_name="DatabaseHelper";
	private static final Logger log = Logger.getLogger(class_name);
	
	private static String database_ip = "192.168.88.122";
	private static String database = "borehole";
	private static String username = "jannie";
	private static String password = "Jackal51";
	
	public static Connection getConnection()
	{
	//	System.out.println(class_name+" starting db connection...");//debug**
		Connection conn=null;
		
		try
		{Class.forName("com.mysql.cj.jdbc.Driver");}
		catch(Exception e)
		{log.severe(class_name+" ERROR! Failed to initialise db driver: \n"+e);}
		String url = "jdbc:mysql://"+database_ip+":3306/"+database+"?allowMultiQueries=true&user="+username+"&password="+password;
		
		try
		{conn = DriverManager.getConnection(url);}
		catch(SQLException se)
		{
			log.severe(class_name+" ERROR! SQLException while trying to start db connection:\n"+se);
			return null;
		}//catch().
	//	System.out.println(class_name+" Connection established.");//debug**
		
		if(conn==null)
		{log.severe(class_name+" ERROR! failed to create database connection!");}
		
		return conn;
	}//getConnection().
	

}//class databaseHelper.




