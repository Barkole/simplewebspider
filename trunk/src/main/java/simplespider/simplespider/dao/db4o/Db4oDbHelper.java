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
import simplespider.simplespider.throttle.host.HostThrottler;

import com.db4o.ObjectContainer;

class Db4oDbHelper implements DbHelper {

	private static final Log	LOG	= LogFactory.getLog(Db4oDbHelper.class);

	private ObjectContainer		containerHashes;
	private ObjectContainer		containerQueue;

	private Db4oDbHelperFactory	helperFactory;

	private final HostThrottler	hostThrottler;

	public Db4oDbHelper(final HostThrottler hostThrottler) {
		this.hostThrottler = hostThrottler;
	}

	@Override
	public void beginTransaction() {
		// Nothing to do

	}

	@Override
	public void close() throws SQLException {
		this.containerHashes.close();
		this.containerHashes = null;
		this.containerQueue.close();
		this.containerQueue = null;
	}

	@Override
	public void commitTransaction() {
		try {
			this.containerQueue.commit();
		} catch (final RuntimeException e) {
			try {
				this.containerQueue.rollback();
			} catch (final RuntimeException e2) {
				LOG.error("Failed to rollback queue database transaction", e);
			}
			try {
				this.containerHashes.rollback();
			} catch (final RuntimeException ee) {
				LOG.error("Failed to rollback hashes database transaction", e);
			}

			throw e;
		}

		try {
			this.containerHashes.commit();
		} catch (final RuntimeException e) {
			try {
				this.containerHashes.rollback();
			} catch (final RuntimeException ee) {
				LOG.error("Failed to rollback hashes database transaction", e);
			}

			throw e;
		}
	}

	@Override
	public Db4oLinkDao getLinkDao() {
		return new Db4oLinkDao(this, this.hostThrottler);
	}

	@Override
	public void rollbackTransaction() throws SQLException {
		try {
			this.containerHashes.rollback();
		} finally {
			this.containerQueue.rollback();
		}
	}

	@Override
	public void shutdown() throws SQLException {
		this.helperFactory.shutdown();
	}

	void createConnection(final Db4oDbHelperFactory db4oDbHelperFactory) {
		this.helperFactory = db4oDbHelperFactory;
		this.containerHashes = this.helperFactory.getHashesConnection();
		this.containerQueue = this.helperFactory.getQueueConnection();
	}

	ObjectContainer getQueueContainer() {
		return this.containerQueue;
	}

	ObjectContainer getHashesContainer() {
		return this.containerHashes;
	}

}
