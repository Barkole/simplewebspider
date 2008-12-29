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
