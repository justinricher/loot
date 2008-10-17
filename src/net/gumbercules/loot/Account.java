package net.gumbercules.loot;

import java.util.ArrayList;
import java.util.Date;

import android.database.*;
import android.database.sqlite.*;

public class Account
{
	private int id;
	String name;
	double initialBalance;
	private static int currentAccount;
	private double actual_balance;
	private double posted_balance;
	private double budget_balance;
	
	public Account()
	{
		this.id = -1;
	}
	
	public int id()
	{
		return this.id;
	}
	
	public int write()
	{
		if (this.id == -1)
			return newAccount();
		else
			return updateAccount();
	}
	
	private int newAccount()
	{
		// insert the new row into the database
		String insert = "insert into accounts (name,balance,timestamp) values (?,?,strftime('%%s','now'))";
		Object[] bindArgs = {this.name, new Double(this.initialBalance)};
		SQLiteDatabase lootDB = Database.getDatabase();
		try
		{
			lootDB.beginTransaction();
			lootDB.execSQL(insert, bindArgs);
			lootDB.setTransactionSuccessful();
		}
		catch (SQLException e)
		{
			return -1;
		}
		finally
		{
			lootDB.endTransaction();
		}
		
		// get the id of that row
		String[] columns = {"max(id)"};
		Cursor cur = lootDB.query("accounts", columns, null, null, null, null, null);
		this.id = cur.getInt(0);
		return this.id;
	}
	
	private int updateAccount()
	{
		// update the row in the database
		String update = "update accounts set name = ?, balance = ?, " +
						"timestamp = strftime('%%s','now') where id = ?";
		Object[] bindArgs = {this.name, new Double(this.initialBalance), new Integer(this.id)};
		SQLiteDatabase lootDB = Database.getDatabase();
		try
		{
			lootDB.beginTransaction();
			lootDB.execSQL(update, bindArgs);
			lootDB.setTransactionSuccessful();
		}
		catch (SQLException e)
		{
			return -1;
		}
		finally
		{
			lootDB.endTransaction();
		}
		
		return this.id;
	}
	
	public boolean erase()
	{
		// mark the row as 'purged' in the database, so it is still recoverable later
		String del = "update accounts set purged = 1, timestamp = strftime('%%s','now') where id = ?";
		Object[] bindArgs = {new Integer(this.id)};
		SQLiteDatabase lootDB = Database.getDatabase();
		try
		{
			lootDB.beginTransaction();
			lootDB.execSQL(del, bindArgs);
			lootDB.setTransactionSuccessful();
		}
		catch (SQLException e)
		{
			return false;
		}
		finally
		{
			lootDB.endTransaction();
		}
		
		return true;
	}
	
	private double calculateBalance(String clause)
	{
		SQLiteDatabase lootDB = Database.getDatabase();
		String[] columns = {"sum(amount)"};
		String[] sArgs = {Integer.toString(this.id)};
		Cursor cur = lootDB.query("transactions", columns, clause, sArgs, null, null, null);
		return cur.getDouble(0);
	}
	
	public double getActualBalance()
	{
		return this.actual_balance;
	}
	
	public double calculateActualBalance()
	{
		this.actual_balance = calculateBalance("account = ? and purged = 0 and budget = 0");
		return this.actual_balance;
	}
	
	public double getPostedBalance()
	{
		return this.posted_balance;
	}
	
	public double calculatePostedBalance()
	{
		this.posted_balance = calculateBalance("account = ? and posted = 1 and purged = 0");
		return this.posted_balance;
	}
	
	public double getBudgetBalance()
	{
		return this.budget_balance;
	}
	
	public double calculateBudgetBalance()
	{
		this.budget_balance = calculateBalance("account = ? and purged = 0");
		return this.budget_balance;
	}
	
	public static Account getLastUsedAccount()
	{
		Account acct = new Account();
		acct.loadById( (int)Database.getOptionInt("last_used") );
		return acct;
	}
	
	public void loadById(int id)
	{
		Account acct = Account.getAccountById(id);
		this.id = acct.id;
		this.name = acct.name;
		this.initialBalance = acct.initialBalance;
	}
	
	public boolean setLastUsed()
	{
		return Database.setOption("last_used", this.id);
	}
	
	public static int getCurrentAccountNum()
	{
		return currentAccount;
	}
	
	public void setCurrentAccountNum()
	{
		currentAccount = this.id;
	}
	
	public static String[] getAccountNames()
	{
		SQLiteDatabase lootDB;
		try
		{
			lootDB = Database.getDatabase();
		}
		catch (SQLException e)
		{
			return null;
		}
		
		String[] columns = {"name"};
		Cursor cur = lootDB.query("accounts", columns, "purged = 0", null, null, null, null);
		ArrayList<String> accounts = new ArrayList<String>();
		
		do
		{
			accounts.add(cur.getString(0));
		} while (cur.moveToNext());
		
		return (String[])accounts.toArray();
	}
	
	public static int[] getAccountIds()
	{
		SQLiteDatabase lootDB;
		try
		{
			lootDB = Database.getDatabase();
		}
		catch (SQLException e)
		{
			return null;
		}
		
		String[] columns = {"id"};
		Cursor cur = lootDB.query("accounts", columns, "purged = 0", null, null, null, null);
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		do
		{
			ids.add(cur.getInt(0));
		} while (cur.moveToNext());
		
		// convert the Integer ArrayList to int[]
		int[] acc_ids = new int[ids.size()];
		for (int i = 0; i < ids.size(); ++i)
		{
			acc_ids[i] = ids.get(i).intValue();
		}
		
		return acc_ids;
	}
	
	public static Account getAccountByName( String name )
	{
		SQLiteDatabase lootDB;
		try
		{
			lootDB = Database.getDatabase();
		}
		catch (SQLException e)
		{
			return null;
		}
		
		String[] columns = {"id", "name", "balance"};
		String[] sArgs = {name};
		Cursor cur = lootDB.query("accounts", columns, "name = ? and purged = 0", sArgs,
				null, null, null, "LIMIT 1");
		Account acct = new Account();
		acct.id = cur.getInt(0);
		acct.name = cur.getString(1);
		acct.initialBalance = cur.getDouble(2);
		
		return acct;
	}
	
	public static Account getAccountById( int id )
	{
		SQLiteDatabase lootDB;
		try
		{
			lootDB = Database.getDatabase();
		}
		catch (SQLException e)
		{
			return null;
		}
		
		String[] columns = {"id", "name", "balance"};
		String[] sArgs = {Integer.toString(id)};
		Cursor cur = lootDB.query("accounts", columns, "id = ? and purged = 0", sArgs,
				null, null, null, "LIMIT 1");
		Account acct = new Account();
		acct.id = cur.getInt(0);
		acct.name = cur.getString(1);
		acct.initialBalance = cur.getDouble(2);
		
		return acct;
	}

	public int getNextCheckNum()
	{
		SQLiteDatabase lootDB = Database.getDatabase();
		String[] columns = {"max(check_num)"};
		String[] sArgs = {Integer.toString(this.id)};
		Cursor cur = lootDB.query("transactions", columns, "account = ?", sArgs, null, null, null);
		int check_num = cur.getInt(0);
		if (check_num >= 0)
			check_num += 1;
		
		return check_num;
	}
	
	public boolean setLastTransactionDate( Date d )
	{
		return Database.setOption("last_trans_" + this.id, d.getTime());
	}
	
	public long getLastTransactionDate()
	{
		return Database.getOptionInt("last_trans_" + this.id);
	}
}