package simplespider.simplespider.dao.db4o;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.dao.LinkDao;
import simplespider.simplespider.enity.Link;
import simplespider.simplespider.util.ValidityHelper;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Predicate;
import com.db4o.query.Query;

public class Db4oLinkDao implements LinkDao {

	private static final Log	LOG	= LogFactory.getLog(Db4oLinkDao.class);

	private final Db4oDbHelper	dbHelper;

	//	private static class QueryByUrl extends Predicate<Link> {
	//
	//		private static final long	serialVersionUID	= 2607574799548754285L;
	//
	//		private final String		url;
	//
	//		private QueryByUrl(final String url) {
	//			super();
	//			this.url = url;
	//		}
	//
	//		@Override
	//		public boolean match(final Link candidate) {
	//			return this.url.equals(candidate.getUrl());
	//		}
	//	}

	private static class QueryByNotDone extends Predicate<Link> {
		private static final long	serialVersionUID	= 306198406406817207L;

		@Override
		public boolean match(final Link candidate) {
			return !candidate.isDone();
		}

	}

	private static class QueryByBottstrapAndNotDone extends Predicate<Link> {
		private static final long	serialVersionUID	= -3582070061492062489L;

		@Override
		public boolean match(final Link candidate) {
			return !candidate.isDone() && candidate.isBootstrap();
		}

	}

	Db4oLinkDao(final Db4oDbHelper db4oDbHelper) {
		this.dbHelper = db4oDbHelper;
	}

	@Override
	public Link getByUrl(final String url) {
		ValidityHelper.checkNotEmpty("url", url);

		final ObjectContainer container = this.dbHelper.getContainer();
		final Query query = container.query();
		query.constrain(Link.class);
		query.descend("url").constrain(url);
		final ObjectSet<Link> links = query.execute();
		if (!links.hasNext()) {
			return null;
		}

		final Link next = links.next();
		container.activate(next, 5);

		if (links.hasNext()) {
			throw new RuntimeException("Failed to get link with url \"" + url + "\": was not unique");
		}

		return next;
	}

	@Override
	public Link getNext() {
		final List<Link> links = getNext(1);
		if (ValidityHelper.isEmpty(links)) {
			return null;
		}
		return links.get(0);
	}

	@Override
	public List<Link> getNext(final int limit) {
		final ObjectContainer container = this.dbHelper.getContainer();
		final ObjectSet<Link> links = container.query(new QueryByNotDone());

		final List<Link> activatedLinks = new ArrayList<Link>();
		int count = 0;
		while (++count < limit && links.hasNext()) {
			final Link next = links.next();
			container.activate(next, 5);
			activatedLinks.add(next);
		}

		return activatedLinks;
	}

	@Override
	public Link getNextUpBootstrap() {
		final List<Link> links = getNextUpBootstrap(1);
		if (ValidityHelper.isEmpty(links)) {
			return null;
		}
		return links.get(0);
	}

	@Override
	public List<Link> getNextUpBootstrap(final int limit) {
		final ObjectContainer container = this.dbHelper.getContainer();
		final ObjectSet<Link> links = container.query(new QueryByBottstrapAndNotDone());

		final List<Link> activatedLinks = new ArrayList<Link>();
		int count = 0;
		while (++count < limit && links.hasNext()) {
			final Link next = links.next();
			container.activate(next, 5);
			activatedLinks.add(next);
		}

		return activatedLinks;
	}

	@Override
	public boolean isAvailable(final String url) {
		//		final Link links = getByUrl(url);
		//		return links != null;
		return false;
	}

	@Override
	public void save(final Link link) {
		final ObjectContainer container = this.dbHelper.getContainer();
		container.store(link);
	}

	void delete(final Link link) {
		final ObjectContainer container = this.dbHelper.getContainer();
		container.delete(link);
	}

	public void logAllEntities() {
		if (!LOG.isDebugEnabled()) {
			return;
		}

		LOG.debug("Dump all available links...");
		final ObjectContainer container = this.dbHelper.getContainer();
		final ObjectSet<Link> allLinks = container.query(Link.class);
		long count = 0;
		while (allLinks.hasNext()) {
			final Link link = allLinks.next();
			container.activate(link, 5);
			LOG.debug(link.toString());
			count++;
		}
		LOG.debug("Count links: " + count);
	}
}
