package simplespider.simplespider.dao.mem;

import simplespider.simplespider.dao.DbHelper;
import simplespider.simplespider.dao.DbHelperFactory;
import simplespider.simplespider.throttle.host.HostThrottler;

import java.sql.SQLException;

import org.apache.commons.configuration.Configuration;

public class MemDbHelperFactory implements DbHelperFactory {

    private static final String KEY_QUEUE_SIZE  = "database.mem.queue.size";
    private static final int   DFLT_QUEUE_SIZE = 1_000_000;

    volatile HostThrottler     hostThrottler;
    volatile SimpleSet<String> queue;

    public MemDbHelperFactory(final Configuration configuration, final HostThrottler hostThrottler) {
        this.hostThrottler = hostThrottler;
        int size = configuration.getInt(KEY_QUEUE_SIZE, DFLT_QUEUE_SIZE);
        this.queue = new FixedSizeSet<String>(size);
    }

	@Override
    public DbHelper buildDbHelper() throws SQLException {
        return new MemDbHelper(this);
    }

    void shutdown() {
        this.queue = null;
        this.hostThrottler = null;
    }

}
