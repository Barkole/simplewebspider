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

package simplespider.simplespider.dao.db4o;

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.dao.DbHelper;

import com.db4o.ObjectContainer;

public class Db4oDbHelper implements DbHelper {

	private static final Log	LOG	= LogFactory.getLog(Db4oDbHelper.class);

	private ObjectContainer		container;

	private Db4oDbHelperFactory	helperFactory;

	@Override
	public void beginTransaction() {
		// Nothing to do

	}

	@Override
	public void close() throws SQLException {
		this.container.close();
		this.container = null;
	}

	@Override
	public void commitTransaction() {
		try {
			this.container.commit();
		} catch (final RuntimeException e) {
			try {
				this.container.rollback();
			} catch (final RuntimeException e2) {
				LOG.error("Failed to rollback database transaction", e);
			}

			throw e;
		}
	}

	@Override
	public Db4oLinkDao getLinkDao() {
		return new Db4oLinkDao(this);
	}

	@Override
	public void rollbackTransaction() throws SQLException {
		this.container.rollback();
	}

	@Override
	public void shutdown() throws SQLException {
		this.helperFactory.shutdown();
	}

	void createConnection(final Db4oDbHelperFactory db4oDbHelperFactory) {
		this.helperFactory = db4oDbHelperFactory;
		this.container = this.helperFactory.getConnection();
	}

	ObjectContainer getContainer() {
		return this.container;
	}

}
