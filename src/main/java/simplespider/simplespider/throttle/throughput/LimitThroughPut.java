package simplespider.simplespider.throttle.throughput;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LimitThroughPut {
    private static final Log    LOG                         = LogFactory.getLog(LimitThroughPut.class);

    private static final long   MIN_THREAD_SPAWN_WAIT       = 100L;
    private static final String BOT_URLS_PER_MINUTE         = "bot.urls-per-minute";
    private static final int    BOT_URLS_PER_MINUTE_DEFAULT = 10;

    private final List<Date>    times                       = new LinkedList<Date>();
    private final int           maxPerMinute;
    private final long          staticWaitTime;

    // use ReentrantLock instead of synchronized for scalability
    private final ReentrantLock lock                        = new ReentrantLock(false);

    public LimitThroughPut(Configuration configuration) {
        int maxPerMinute = configuration.getInt(BOT_URLS_PER_MINUTE, BOT_URLS_PER_MINUTE_DEFAULT);
        if (maxPerMinute <= 0) {
            LOG.warn("Configuration " + BOT_URLS_PER_MINUTE + " is invalid. Using default value: " + BOT_URLS_PER_MINUTE);
            maxPerMinute = BOT_URLS_PER_MINUTE_DEFAULT;
        }
        this.maxPerMinute = maxPerMinute;
        this.staticWaitTime = 60000L / maxPerMinute;
    }

    private long waitTime() {
        long waitTime = cleanup();
        if (waitTime < staticWaitTime) {
            waitTime = (waitTime + staticWaitTime) / 2L;
        }
        if (waitTime < MIN_THREAD_SPAWN_WAIT) {
            waitTime = MIN_THREAD_SPAWN_WAIT;
        }
        return waitTime;
    }

    public void next() {
        try {
            final long wait = waitTime();
            TimeUnit.MILLISECONDS.sleep(wait);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Sleep was interrupted", e);
        }
        put();
    }

    private void put() {
        lock.lock();
        try {
            this.times.add(new Date());
        } finally {
            lock.unlock();
        }
    }

    private long cleanup() {
        lock.lock();
        try {
            final Date beforeOneMinute = getDateBeforeOneMinute();

            Date firstBlocking = null;

            for (final Iterator<Date> iterator = this.times.iterator(); iterator.hasNext();) {
                final Date item = iterator.next();

                if (item.before(beforeOneMinute)) {
                    // Removing all timestamps before the last minute
                    iterator.remove();
                } else if (this.times.size() < maxPerMinute) {
                    // If less than maximum allowed after removing all old timestamp no job to do
                    break;
                } else {
                    firstBlocking = item;
                    break;
                }
            }

            if (firstBlocking == null) {
                // If there is no blocking, return zero for sleeping
                return 0;
            }

            return firstBlocking.getTime() - beforeOneMinute.getTime();
        } finally {
            lock.unlock();
        }
    }

    private Date getDateBeforeOneMinute() {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -1);
        final Date current = calendar.getTime();
        return current;
    }

}
