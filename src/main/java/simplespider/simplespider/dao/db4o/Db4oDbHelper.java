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
	public void commitTransaction() throws SQLException {
		this.container.commit();
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
