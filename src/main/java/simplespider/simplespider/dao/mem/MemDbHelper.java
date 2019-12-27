package simplespider.simplespider.dao.mem;

import simplespider.simplespider.dao.DbHelper;
import simplespider.simplespider.dao.LinkDao;
import simplespider.simplespider.throttle.host.HostThrottler;

import java.sql.SQLException;

/**
 * Created by Mike on 18.02.2017.
 */

class MemDbHelper implements DbHelper {

    private final MemDbHelperFactory memDbHelperFactory;

    public MemDbHelper(final MemDbHelperFactory memDbHelperFactory) {
        this.memDbHelperFactory = memDbHelperFactory;
    }

	@Override
    public void beginTransaction() {
        // No transaction support
    }

	@Override
    public void close() throws SQLException {
        // Nothing to do
    }

	@Override
    public void commitTransaction() throws SQLException {
        // No transaction support
    }

	@Override
    public LinkDao getLinkDao() {
        return new MemLinkDao(this);
    }

	@Override
    public void rollbackTransaction() throws SQLException {
        // No transaction support
    }

	@Override
    public void shutdown() throws SQLException {
        memDbHelperFactory.shutdown();
    }

    SimpleSet<String> getQueue() {
        return memDbHelperFactory.queue;
    }

    HostThrottler getHostThrottler() {
        return memDbHelperFactory.hostThrottler;
    }
}
