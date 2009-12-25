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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.dao.LinkDao;
import simplespider.simplespider.throttle.host.HostThrottler;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.constraints.UniqueFieldValueConstraintViolationException;
import com.db4o.query.Query;

class Db4oLinkDao implements LinkDao {

	private static final Log	LOG	= LogFactory.getLog(Db4oLinkDao.class);

	private final Db4oDbHelper	dbHelper;
	private final Random		random;
	private final HostThrottler	hostThrottler;

	Db4oLinkDao(final Db4oDbHelper db4oDbHelper, final HostThrottler hostThrottler) {
		this.dbHelper = db4oDbHelper;
		this.hostThrottler = hostThrottler;
		this.random = new Random();
	}

	@Override
	public String removeNextAndCommit() {
		final ObjectContainer queueContainer = this.dbHelper.getQueueContainer();
		// Get list with all Link objects
		final Query query = queueContainer.query();
		query.constrain(Link.class);
		final ObjectSet<Link> links = query.execute();

		// If list is empty, there is none URL that could be loaded
		final int size = links.size();
		if (size == 0) {
			return null;
		}

		// Determine how many Link objects has to be loaded into memory for host throttling
		final int maxUrlsAtOnce = this.hostThrottler.getUrlsAtOnce();
		final int loadUrlsCount = Math.min(size, maxUrlsAtOnce);

		// Required for host throttler
		final List<String> urls = new ArrayList<String>(loadUrlsCount);
		// Required to get original Link object for URL string
		final Map<String, Link> urlStringsToLink = new HashMap<String, Link>(loadUrlsCount);

		// Activate all Links, that are be checked for the best one
		for (int i = 0; i < loadUrlsCount; i++) {
			final int randomElement = this.random.nextInt(size);
			final Link link = links.get(randomElement);
			queueContainer.activate(link, 1);

			final String url = link.getUrl();
			urls.add(url);
			urlStringsToLink.put(url, link);
		}

		// Get the best URL
		final String nextUrlString = this.hostThrottler.getBestFitting(urls);

		// Remove the best one from database
		final Link nextLink = urlStringsToLink.get(nextUrlString);
		if (nextLink == null) {
			throw new IllegalStateException("Can not find link for " + nextUrlString + " in internal map");
		}
		queueContainer.delete(nextLink);

		// Clean up db4o management and heap
		for (final Link link : urlStringsToLink.values()) {
			queueContainer.deactivate(link, 1);
		}

		// Commit removing
		this.dbHelper.commitTransaction();

		return nextUrlString;
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
