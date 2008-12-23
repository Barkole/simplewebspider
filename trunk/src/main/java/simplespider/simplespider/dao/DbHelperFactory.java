package simplespider.simplespider.dao;

import java.sql.SQLException;


public interface DbHelperFactory {

	public abstract DbHelper buildDbHelper() throws SQLException;

}