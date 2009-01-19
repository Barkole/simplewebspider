/**
 * Simple Web Spider - <http://simplewebspider.sourceforge.net/>
 * Copyright (C) 2009  <berendona@users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package simplespider.simplespider.dao.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import simplespider.simplespider.dao.DbHelper;
import simplespider.simplespider.dao.LinkDao;

public class JdbcDbHelper implements DbHelper {

	private Connection	connection;

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.dao.jdbc.DbHelper#beginTransaction()
	 */
	public void beginTransaction() {
		// Nothing to do
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.dao.jdbc.DbHelper#close()
	 */
	public void close() throws SQLException {
		this.connection.close();
		this.connection = null;
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.dao.jdbc.DbHelper#commitTransaction()
	 */
	public void commitTransaction() throws SQLException {
		this.connection.commit();
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.dao.jdbc.DbHelper#createConnection()
	 */
	void createConnection() throws SQLException {
		this.connection = DriverManager.getConnection("jdbc:hsqldb:file:testdb", "sa", "");
	}

	@Override
	public LinkDao getLinkDao() {
		return new JdbcLinkDao(this);
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.dao.jdbc.DbHelper#prepareStatement(java.lang.String)
	 */
	public PreparedStatement prepareStatement(final String sql) throws SQLException {
		return this.connection.prepareStatement(sql);
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.dao.jdbc.DbHelper#rollbackTransaction()
	 */
	public void rollbackTransaction() throws SQLException {
		this.connection.rollback();
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.dao.jdbc.DbHelper#shutdown()
	 */
	public void shutdown() throws SQLException {
		final Statement shutdown = this.connection.createStatement();
		shutdown.execute("SHUTDOWN");
		// There no need to close statement, because whole database connection is invalid
		this.connection = null;
	}

}
