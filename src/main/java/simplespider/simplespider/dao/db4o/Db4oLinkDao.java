package simplespider.simplespider.dao.db4o;

import simplespider.simplespider.dao.LinkDao;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;

public class Db4oLinkDao implements LinkDao {

	private final Db4oDbHelper	dbHelper;

	Db4oLinkDao(final Db4oDbHelper db4oDbHelper) {
		this.dbHelper = db4oDbHelper;
	}

	@Override
	public String removeNext() {
		final ObjectContainer container = this.dbHelper.getContainer();
		final ObjectSet<Link> links = container.query(Link.class);

		if (!links.hasNext()) {
			return null;
		}

		final Link next = links.next();
		container.activate(next, 1);
		final String url = next.getUrl();
		container.delete(next);
		container.deactivate(next, 1);

		return url;
	}

	@Override
	public void save(final String url) {
		final ObjectContainer container = this.dbHelper.getContainer();
		final Link link = new Link(url);
		container.store(link);
	}
}
