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
package simplespider.simplespider;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LimitThroughPut {

	private static final Log	LOG		= LogFactory.getLog(LimitThroughPut.class);

	final private int			maxPerMinute;
	final private List<Date>	times	= new LinkedList<Date>();

	public LimitThroughPut(final int maxPerMinute) {
		this.maxPerMinute = maxPerMinute;
	}

	public void next() {
		final long wait = cleanup();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Wait for milliseconds: " + wait);
		}
		try {
			TimeUnit.MILLISECONDS.sleep(wait);
		} catch (final InterruptedException e) {
			LOG.warn("Sleep was interrupted", e);
		}
		put();
	}

	private void put() {
		this.times.add(new Date());
	}

	private long cleanup() {
		final Date beforeOneMinute = getDateBeforeOneMinute();

		Date mostBlocking = null;

		final int beforeCleanup = this.times.size();

		for (final Iterator<Date> iterator = this.times.iterator(); iterator.hasNext();) {
			final Date item = iterator.next();

			if (item.before(beforeOneMinute)) {
				// Removing all timestamps before the last minute
				iterator.remove();
			} else if (this.times.size() < this.maxPerMinute) {
				// If less than maximum allowed after removing all old timestamp no job to do
				break;
			} else {
				// Remove timestamp, that is already in time span, but hold it
				iterator.remove();
				mostBlocking = item;
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Cleanup - before: " + beforeCleanup + ", after: " + this.times.size() + ", max per minute: " + this.maxPerMinute);
		}

		if (mostBlocking == null) {
			// If there is no blocking, return zero for sleeping
			return 0;
		}

		return mostBlocking.getTime() - beforeOneMinute.getTime();
	}

	private Date getDateBeforeOneMinute() {
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -1);
		final Date current = calendar.getTime();
		return current;
	}

}
