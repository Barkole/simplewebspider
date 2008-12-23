package simplespider.simplespider.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface DbHelper {

	public abstract void beginTransaction();

	public abstract void close() throws SQLException;

	public abstract void commitTransaction() throws SQLException;

	// public abstract void createConnection() throws SQLException;

	public abstract LinkDao getLinkDao();

	public abstract PreparedStatement prepareStatement(final String sql) throws SQLException;

	public abstract void rollbackTransaction() throws SQLException;

	public abstract void shutdown() throws SQLException;

}