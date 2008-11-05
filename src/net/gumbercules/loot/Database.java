package net.gumbercules.loot;

import android.database.*;
import android.database.sqlite.*;

public class Database
{
	private final static String DB_NAME		= "LootDB";
	private final static String DB_PATH		= "/data/data/net.gumbercules.loot/" + DB_NAME + ".db";
	private final static int DB_VERSION		= 1;
	private static SQLiteDatabase lootDB	= null;
	
	public Database()
	{
		openDB();
	}
	
	private void openDB()
	{
		// if the database is already set, we don't need to open it again
		if ( lootDB != null )
			return;
		
		try
		{
			lootDB = SQLiteDatabase.openDatabase( DB_PATH, null, SQLiteDatabase.OPEN_READWRITE );
			if ( lootDB.needUpgrade( DB_VERSION ) )
				if ( !this.upgradeDB( DB_VERSION ) )
					lootDB = null;
		}
		// catch SQLiteException if the database doesn't exist, then create it
		catch (SQLiteException sqle)
		{
			try
			{
				lootDB = SQLiteDatabase.openOrCreateDatabase( DB_PATH, null);
				if ( !this.createDB() )
					lootDB = null;
			}
			// something went wrong creating the database
			catch ( SQLiteException e )
			{
				lootDB = null;
			}
		}
		
		// throw an exception if we've made it through the try/catch block
		// and the database is still not opened
		if ( lootDB == null )
		{
			throw new SQLException( "Database could not be opened" );
		}
	}
	
	private boolean createDB()
	{
		String[] createSQL = new String[15];
		
		createSQL[0] = "create table accounts(\n" + 
					"	id integer primary key autoincrement,\n" +
					"	name varchar(100) not null unique,\n" + 
					"	balance real default 0.0,\n" + 
					"	timestamp integer default 0,\n" +
					"	purged bool default 0)";

		createSQL[1] = "create table transactions(\n" +
					"	id integer primary key autoincrement,\n" +
					"	posted bool default 0,\n" +
					"	account integer not null,\n" +
					"	date integer not null,\n" +
					"	party varchar(64),\n" +
					"	amount real default 0.0,\n" +
					"	check_num integer default 0,\n" +
					"	timestamp integer default 0,\n" +
					"	purged bool default 0,\n" +
					"	budget bool default 0)";

		createSQL[2] = "create table tags(\n" +
					"	trans_id integer,\n" +
					"	name varchar(40),\n" +
					"	primary key (trans_id, name))";

		createSQL[3] = "create table options(\n" +
					"	option varchar(20) primary key,\n" +
					"	value varchar(50))";

		createSQL[4] = "create table transfers(\n" +
					"	trans_id1 integer,\n" +
					"	trans_id2 integer,\n" +
					"	unique ( trans_id1, trans_id2 ))";

		createSQL[5] = "create table repeat_pattern(\n" +
					"	id integer primary key autoincrement,\n" +
					"	start_date integer,\n" +
					"	due integer,\n" +
					"	end_date integer,\n" +
					"	iterator integer,\n" +
					"	frequency integer,\n" +
					"	custom integer)";

		createSQL[6] = "create table repeat_transactions(\n" +
					"	trans_id integer not null,\n" +
					"	repeat_id integer not null,\n" +
					"	account integer not null,\n" +
					"	date integer not null,\n" +
					"	party varchar(64),\n" +
					"	amount real,\n" +
					"	check_num integer,\n" +
					"	budget bool,\n" +
					"	tags text,\n" +
					"	transfer_id integer,\n" +
					"	primary key (trans_id, repeat_id))";

		createSQL[7] = "insert into options values ('last_used','-1')";
		createSQL[8] = "insert into options values ('bal_visible','0')";
		createSQL[9] = "insert into options values ('sort_column','3')";
		createSQL[10] = "insert into options values ('sort_order','0')";
		createSQL[11] = "insert into options values ('auto_purge_days','-1')";
		createSQL[12] = "insert into options values ('del_transfer_warn','1')";
		createSQL[13] = "insert into options values ('post_repeats_early','2')";

		createSQL[14] = "create index idx_trans_id on transactions ( id asc )";
		
		try
		{
			lootDB.beginTransaction();
			
			// loop through creation strings to create entire database
			for ( String sql_str : createSQL )
			{
				lootDB.execSQL( sql_str );
			}
			lootDB.setTransactionSuccessful();
			lootDB.setVersion( DB_VERSION );
		}
		catch ( SQLException e )
		{
			return false;
		}
		finally
		{
			lootDB.endTransaction();
		}

		return true;
	}
	
	private boolean upgradeDB( int max_version )
	{
		if ( lootDB.getVersion() == max_version )
			return true;

		// in the future, this is where upgrades happen
		return false;
	}
	
	@SuppressWarnings("unused")
	public static SQLiteDatabase getDatabase()
	{
		if ( lootDB == null )
		{
			Database db = new Database();
		}
		return lootDB;
	}
	
	public static void closeDatabase()
	{
		lootDB.close();
		lootDB = null;
	}
	
	@SuppressWarnings("unused")
	private static boolean privSetOption( Object option, Object value )
	{
		Object dummy = getOptionString( (String)option );
		String sql;
		if ( dummy == null )
			sql = "insert into options (value,option) values (?,?)";
		else
			sql = "update options set value = ? where option = ?";

		try
		{
			Database db = new Database();
		}
		catch ( SQLException e )
		{
			return false;
		}
		
		lootDB.beginTransaction();
		try
		{
			Object[] bindArgs = { option, value };
			lootDB.execSQL(sql, bindArgs);
			lootDB.setTransactionSuccessful();
		}
		catch ( SQLException e )
		{
			return false;
		}
		finally
		{
			lootDB.endTransaction();
		}
		
		return true;
	}
	
	public static boolean setOption( String option, String value )
	{
		return privSetOption(option, value);
	}
	
	public static boolean setOption( String option, long value )
	{
		return privSetOption(option, value);
	}
	
	@SuppressWarnings("unused")
	private static Cursor cursorGetOption( String option )
	{
		try
		{
			Database db = new Database();
		}
		catch ( SQLException e )
		{
			return null;
		}

		String[] columns = {"value"};
		String[] sArgs = {option};
		Cursor cur = lootDB.query("options", columns, "option = ?", sArgs, null, null, null, "1");
		if (!cur.moveToFirst())
		{
			cur.close();
			return null;
		}
		
		return cur;
	}
	
	public static String getOptionString( String option )
	{
		Cursor cur = cursorGetOption(option);
		String str = null;
		if (cur != null)
		{
			str = cur.getString(0);
			cur.close();
		}
		
		return str;
	}
	
	public static long getOptionInt( String option )
	{
		Cursor cur = cursorGetOption(option);
		long l = -1;
		if (cur != null)
		{
			l = cur.getLong(0);
			cur.close();
		}
		
		return l;
	}
	
	public static boolean getBoolean( int b )
	{
		if (b == 0)
			return false;
		return true;
	}
	
	public static int setBoolean(boolean b)
	{
		if (!b)
			return 0;
		return 1;
	}
}
