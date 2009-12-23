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

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.dao.LinkDao;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.constraints.UniqueFieldValueConstraintViolationException;
import com.db4o.query.Query;

public class Db4oLinkDao implements LinkDao {

	private static final Log	LOG	= LogFactory.getLog(Db4oLinkDao.class);

	private final Db4oDbHelper	dbHelper;
	private final Random		random;

	Db4oLinkDao(final Db4oDbHelper db4oDbHelper) {
		this.dbHelper = db4oDbHelper;
		this.random = new Random();
	}

	@Override
	public String removeNextAndCommit() {
		final ObjectContainer queueContainer = this.dbHelper.getQueueContainer();
		final Query query = queueContainer.query();
		query.constrain(Link.class);
		final ObjectSet<Link> links = query.execute();

		final int size = links.size();
		if (size == 0) {
			return null;
		}

		final int randomElement = this.random.nextInt(size);
		final Link next = links.get(randomElement);
		queueContainer.activate(next, 1);
		final String url = next.getUrl();
		queueContainer.delete(next);
		queueContainer.deactivate(next, 1);
		this.dbHelper.commitTransaction();

		return url;
	}

	@Override
	public void saveAndCommit(final String url) {
		if (addHash(url)) {
			saveForced(url);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Ignoring duplicate: " + url);
		}
	}

	public void saveForced(final String url) {
		final ObjectContainer queueContainer = this.dbHelper.getQueueContainer();
		final Link link = new Link(url);
		queueContainer.store(link);
		try {
			this.dbHelper.commitTransaction();
		} catch (final UniqueFieldValueConstraintViolationException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Ignoring duplicate: " + url);
			}
		}
	}

	private boolean addHash(final String url) {
		final ObjectContainer hashesContainer = this.dbHelper.getHashesContainer();

		final String md5 = MD5.buildMD5(url);
		final Hash hash = new Hash(md5);
		try {
			hashesContainer.store(hash);
		} catch (final UniqueFieldValueConstraintViolationException e) {
			return false;
		}
		return true;
	}
}
