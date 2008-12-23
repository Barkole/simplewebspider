package simplespider.simplespider.dao.jdbc;

import java.sql.SQLException;

import simplespider.simplespider.dao.DbHelper;
import simplespider.simplespider.dao.DbHelperFactory;

public class JdbcDbHelperFactory implements DbHelperFactory {
	private static final String	DRIVER	= "org.hsqldb.jdbcDriver";

	public JdbcDbHelperFactory() {
		try {
			Class.forName(DRIVER).newInstance();
		} catch (final Exception e) {
			throw new RuntimeException("Failed to load " + DRIVER, e);
		}
	}

	/* (non-Javadoc)
	 * @see simplespider.simplespider_core.dao.jdbc.DbHelperFactory#buildDbHelper()
	 */
	public DbHelper buildDbHelper() throws SQLException {
		final JdbcDbHelper jdbcDbHelper = new JdbcDbHelper();
		jdbcDbHelper.createConnection();
		return jdbcDbHelper;
	}
}
