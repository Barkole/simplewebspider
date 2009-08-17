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
		final ObjectContainer container = this.dbHelper.getContainer();
		final Query query = container.query();
		query.constrain(Link.class);
		final ObjectSet<Link> links = query.execute();

		final int size = links.size();
		if (size == 0) {
			return null;
		}

		final int randomElement = this.random.nextInt(size);
		final Link next = links.get(randomElement);
		container.activate(next, 1);
		final String url = next.getUrl();
		container.delete(next);
		container.deactivate(next, 1);
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
		final ObjectContainer container = this.dbHelper.getContainer();
		final Link link = new Link(url);
		container.store(link);
		try {
			this.dbHelper.commitTransaction();
		} catch (final UniqueFieldValueConstraintViolationException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Ignoring duplicate: " + url);
			}
		}
	}

	private boolean addHash(final String url) {
		final ObjectContainer container = this.dbHelper.getContainer();

		final String md5 = MD5.buildMD5(url);
		final Hash hash = new Hash(md5);
		try {
			container.store(hash);
		} catch (final UniqueFieldValueConstraintViolationException e) {
			return false;
		}
		return true;
	}
}
