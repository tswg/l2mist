package core.gameserver.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import core.commons.dbutils.DbUtils;
import core.gameserver.Config;
import core.gameserver.database.DatabaseFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopPlayersSystemDAO
{
	private static final Logger _log = LoggerFactory.getLogger(TopPlayersSystemDAO.class);
	private static final TopPlayersSystemDAO _instance = new TopPlayersSystemDAO();
	
	public static final String SELECT_PVP_SQL_QUERY = "SELECT obj_Id FROM characters ORDER BY pvpkills DESC LIMIT 1";
	public static final String INSERT_PVP_SQL_QUERY = "REPLACE INTO character_variables(obj_id, type, name, value, expire_time) VALUES (?,?,?,?,?)";
	public static final String UPDATE_PVP_SQL_QUERY = "DELETE FROM character_variables WHERE name = 'TopPlayerPvP' ";
	public static final String SELECT_PK_SQL_QUERY = "SELECT obj_Id FROM characters ORDER BY pkkills DESC LIMIT 1";
	public static final String INSERT_PK_SQL_QUERY = "REPLACE INTO character_variables(obj_id, type, name, value, expire_time) VALUES (?,?,?,?,?)";
	public static final String UPDATE_PK_SQL_QUERY = "DELETE FROM character_variables WHERE name = 'TopPlayerPK' ";
	public static final String SELECT_LEVEL_SQL_QUERY = "SELECT char_obj_id FROM character_subclasses ORDER BY level DESC LIMIT 1";
	public static final String INSERT_LEVEL_SQL_QUERY = "REPLACE INTO character_variables(obj_id, type, name, value, expire_time) VALUES (?,?,?,?,?)";
	public static final String UPDATE_LEVEL_SQL_QUERY = "DELETE FROM character_variables WHERE name = 'TopPlayerLevel' ";
	public static final String SELECT_COINS_SQL_QUERY = "SELECT owner_id FROM items WHERE item_id="+Config.COINS_TOP_PLAYERS_SYSTEM+" ORDER BY count DESC LIMIT 1";
	public static final String INSERT_COINS_SQL_QUERY = "REPLACE INTO character_variables(obj_id, type, name, value, expire_time) VALUES (?,?,?,?,?)";
	public static final String UPDATE_COINS_SQL_QUERY = "DELETE FROM character_variables WHERE name = 'TopPlayerCoins' ";

	public static TopPlayersSystemDAO getInstance()
	{
		return _instance;
	}

	public int selectPvP()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;		
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_PVP_SQL_QUERY);
			rset = statement.executeQuery();
			if(rset.next())
				return rset.getInt("obj_Id");
		}
		catch(Exception e)
		{
			_log.info("TopPlayersSystemDAO.select(int): " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return 0;
	}

	public void insertPvP()
	{
		if (selectPvP() != 0)
		{
			Connection con = null;
			PreparedStatement statement = null;
			
			String _type = "user-var";
			String _name = "TopPlayerPvP";
			int _value = 1;
			int _expire_time = -1;
			
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(INSERT_PVP_SQL_QUERY);
				statement.setInt(1, selectPvP());
				statement.setString(2, _type);
				statement.setString(3, _name);
				statement.setInt(4, _value);
				statement.setInt(5, _expire_time);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.info("TopPlayersSystemDAO.insert(int): " + e, e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public void updatePvP()
	{
		Connection con = null;
		PreparedStatement statement = null;	
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_PVP_SQL_QUERY);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.info("TopPlayersSystemDAO.insert(int): " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public int selectPK()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;		
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_PK_SQL_QUERY);
			rset = statement.executeQuery();
			if(rset.next())
				return rset.getInt("obj_Id");
		}
		catch(Exception e)
		{
			_log.info("TopPlayersSystemDAO.select(int): " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return 0;
	}

	public void insertPK()
	{
		if (selectPK() != 0)
		{
			Connection con = null;
			PreparedStatement statement = null;
			
			String _type = "user-var";
			String _name = "TopPlayerPK";
			int _value = 1;
			int _expire_time = -1;
			
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(INSERT_PK_SQL_QUERY);
				statement.setInt(1, selectPK());
				statement.setString(2, _type);
				statement.setString(3, _name);
				statement.setInt(4, _value);
				statement.setInt(5, _expire_time);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.info("TopPlayersSystemDAO.insert(int): " + e, e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public void updatePK()
	{
		Connection con = null;
		PreparedStatement statement = null;	
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_PK_SQL_QUERY);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.info("TopPlayersSystemDAO.insert(int): " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public int selectLevel()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;		
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_LEVEL_SQL_QUERY);
			rset = statement.executeQuery();
			if(rset.next())
				return rset.getInt("char_obj_id");
		}
		catch(Exception e)
		{
			_log.info("TopPlayersSystemDAO.select(int): " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return 0;
	}

	public void insertLevel()
	{
		if (selectLevel() != 0)
		{
			Connection con = null;
			PreparedStatement statement = null;
			
			String _type = "user-var";
			String _name = "TopPlayerLevel";
			int _value = 1;
			int _expire_time = -1;
			
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(INSERT_LEVEL_SQL_QUERY);
				statement.setInt(1, selectLevel());
				statement.setString(2, _type);
				statement.setString(3, _name);
				statement.setInt(4, _value);
				statement.setInt(5, _expire_time);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.info("TopPlayersSystemDAO.insert(int): " + e, e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public void updateLevel()
	{
		Connection con = null;
		PreparedStatement statement = null;	
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_LEVEL_SQL_QUERY);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.info("TopPlayersSystemDAO.insert(int): " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public int selectCoins()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;		
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_COINS_SQL_QUERY);
			rset = statement.executeQuery();
			if(rset.next())
				return rset.getInt("owner_id");
		}
		catch(Exception e)
		{
			_log.info("TopPlayersSystemDAO.select(int): " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return 0;
	}

	public void insertCoins()
	{
		if (selectCoins() != 0)
		{
			Connection con = null;
			PreparedStatement statement = null;
			
			String _type = "user-var";
			String _name = "TopPlayerCoins";
			int _value = 1;
			int _expire_time = -1;
			
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(INSERT_COINS_SQL_QUERY);
				statement.setInt(1, selectCoins());
				statement.setString(2, _type);
				statement.setString(3, _name);
				statement.setInt(4, _value);
				statement.setInt(5, _expire_time);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.info("TopPlayersSystemDAO.insert(int): " + e, e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public void updateCoins()
	{
		Connection con = null;
		PreparedStatement statement = null;	
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_COINS_SQL_QUERY);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.info("TopPlayersSystemDAO.insert(int): " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}