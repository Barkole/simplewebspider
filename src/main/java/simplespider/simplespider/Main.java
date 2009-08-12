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

import java.io.File;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.bot.Crawler;
import simplespider.simplespider.bot.CrawlerImpl;
import simplespider.simplespider.bot.CrawlerRunner;
import simplespider.simplespider.bot.extractor.LinkExtractor;
import simplespider.simplespider.bot.extractor.html.stream.StreamExtractor;
import simplespider.simplespider.bot.http.HttpClientFactory;
import simplespider.simplespider.bot.http.apache.ApacheHttpClientFactory;
import simplespider.simplespider.dao.DbHelper;
import simplespider.simplespider.dao.DbHelperFactory;
import simplespider.simplespider.dao.LinkDao;
import simplespider.simplespider.dao.db4o.Db4oDbHelperFactory;
import simplespider.simplespider.importing.EntityImporter;
import simplespider.simplespider.importing.simplefile.SimpleFileImporter;

/**
 * Hello world!
 */
public class Main {
	private static final Log	LOG							= LogFactory.getLog(Main.class);

	private static final String	PID_FILENAME_KEY			= "sws.daemon.pidfile";
	private static final String	PID_FILENAME_DEFAULT		= "simple-web-spider.pid";
	// TODO Configure this
	private static final String	LINK_IMPORT_FILENAME		= "bootstrapping.txt";
	// TODO Configure this
	private static final int	WAIT_FOR_THREAD_ON_SHUTDOWN	= 3;
	// TODO Configure this
	private static final int	MAX_CURRENT_THREADS			= 4;
	// TODO Configure this
	private static final int	MAX_THREADS_PER_MINUTE		= 10;
	// TODO Configure this
	private static final int	MAX_URL_LENGTH				= 1024;

	private static Thread		mainThread;

	private static Thread getMainDaemonThread() {
		return mainThread;
	}

	private volatile boolean		cancled	= false;
	private final DbHelperFactory	dbHelperFactory;
	final HttpClientFactory			httpClientFactory;
	private Thread					listener;

	private Main(final DbHelperFactory dbHelperFactory, final HttpClientFactory httpClientFactory) {
		this.dbHelperFactory = dbHelperFactory;
		this.httpClientFactory = httpClientFactory;
	}

	static private void daemonize() {
		mainThread = Thread.currentThread();
		getPidFile().deleteOnExit();
	}

	private static File getPidFile() {
		return new File(System.getProperty(PID_FILENAME_KEY, PID_FILENAME_DEFAULT));
	}

	private void startCancleListener() {
		this.listener = new Thread() {
			@Override
			public void run() {
				LOG.warn("Invoke stopping crawler...");
				Main.this.cancled = true;

				try {
					getMainDaemonThread().join();
				} catch (final InterruptedException e) {
					LOG.error("Interrupted which waiting on main daemon thread to complete.");
				}

			}

		};

		if (LOG.isInfoEnabled()) {
			LOG.info("Add shutdown hook...");
		}
		this.listener.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(this.listener);
	}

	private void interruptCancleListener() {
		if (LOG.isInfoEnabled()) {
			LOG.info("Interrupting cancle listner...");
		}
		Runtime.getRuntime().removeShutdownHook(this.listener);
		this.listener.interrupt();
	}

	private void runCrawler() throws SQLException {
		if (LOG.isInfoEnabled()) {
			LOG.info("Start crawler...");
			LOG.info("Open database connection... This could took time...");
		}
		final DbHelper db = this.dbHelperFactory.buildDbHelper();
		try {
			if (LOG.isInfoEnabled()) {
				LOG.info("Importing bootstrap links...");
			}
			importLinks(db, LINK_IMPORT_FILENAME);
			db.commitTransaction();

			final LimitThroughPut limitThroughPut = new LimitThroughPut(Main.MAX_THREADS_PER_MINUTE);
			if (LOG.isInfoEnabled()) {
				LOG.info("Crawl LINK entries...");
			}

			final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(MAX_CURRENT_THREADS, MAX_CURRENT_THREADS, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());

			runCrawler(this.dbHelperFactory, this.httpClientFactory, threadPool, limitThroughPut, false);

			if (LOG.isInfoEnabled()) {
				LOG.info("Invoke shutting down threads...");
			}
			threadPool.shutdown();
			try {
				threadPool.awaitTermination(WAIT_FOR_THREAD_ON_SHUTDOWN, TimeUnit.MINUTES);
			} catch (final InterruptedException e) {
				LOG.warn("failed to wait for ending of all threads", e);
			}
			threadPool.shutdownNow();
			if (LOG.isInfoEnabled()) {
				LOG.info("Crawler stops");
			}
		} finally {
			if (LOG.isInfoEnabled()) {
				LOG.info("Shutting down database");
			}
			db.shutdown();

		}
	}

	private void importLinks(final DbHelper db, final String filename) {
		final EntityImporter importer = new SimpleFileImporter(filename);
		final long importLink = importer.importLink(db.getLinkDao());
		if (LOG.isInfoEnabled()) {
			LOG.info("Imported links from file \"" + filename + "\": " + importLink);
		}
	}

	private void runCrawler(final DbHelperFactory dbHelperFactory, final HttpClientFactory httpClientFactory, final ThreadPoolExecutor threadPool,
			final LimitThroughPut limitThroughPut, final boolean bootstrapping) throws SQLException {
		final DbHelper db = dbHelperFactory.buildDbHelper();
		final LinkDao linkDao = db.getLinkDao();

		int retryCountOnNoLinks = 0;
		while (!this.cancled) {
			// Block while to much threads were working in last minute
			limitThroughPut.next();

			// Check for next link, if there is none, wait and try again
			final String next = linkDao.removeNext();
			try {
				db.commitTransaction();
			} catch (final RuntimeException e) {
				LOG.error("Failed to commit LINK entity " + next.toString() + " on done", e);
			}

			if (next == null) {
				// On bootstrapping don't do any retry, if no more links are available
				if (bootstrapping) {
					if (LOG.isInfoEnabled()) {
						LOG.info("Bootstrapping: No more links available...");
					}
					break;
				}
				// Seconds try fails
				if (threadPool.getActiveCount() == 0 //
						|| retryCountOnNoLinks > 3) {
					LOG.fatal("No more links available...");
					break;
				}

				// Wait for all running treads, perhaps there are any and they create some new LINK entities
				retryCountOnNoLinks++;
				if (LOG.isInfoEnabled()) {
					LOG.info("No more links available... Waiting for running thread and retry... Count " + retryCountOnNoLinks);
				}
				try {
					threadPool.awaitTermination(WAIT_FOR_THREAD_ON_SHUTDOWN, TimeUnit.MINUTES);
				} catch (final InterruptedException e) {
					LOG.warn("failed to wait for ending of all threads", e);
				}
				continue;
			} else {
				retryCountOnNoLinks = 0;
			}

			if (LOG.isInfoEnabled()) {
				LOG.info("Start crawling URL: \"" + next + "\"");
			}

			final LinkExtractor extractor = new StreamExtractor(MAX_URL_LENGTH);
			final Crawler crawler = new CrawlerImpl(dbHelperFactory, extractor, httpClientFactory);
			threadPool.execute(new CrawlerRunner(crawler, next));
		}
	}

	public static void main(final String[] args) throws Exception {
		if (LOG.isInfoEnabled()) {
			LOG.info("Starting program...");
		}
		try {
			// do sanity checks and startup actions
			daemonize();
		} catch (final Throwable e) {
			LOG.fatal("Startup failed", e);
		}

		final DbHelperFactory dbHelperFactory = new Db4oDbHelperFactory("sws.db4o");

		final HttpClientFactory httpClientFactory;
		if (args.length == 2) {
			httpClientFactory = new ApacheHttpClientFactory(args[0], Integer.parseInt(args[1]));
		} else {
			httpClientFactory = new ApacheHttpClientFactory();
		}

		final Main main = new Main(dbHelperFactory, httpClientFactory);
		main.startCancleListener();
		try {
			main.runCrawler();
		} catch (final RuntimeException e) {
			LOG.error("Uncaught and unhandled error occurs. Please report this bug", e);
			main.interruptCancleListener();
			System.exit(1);
		}
		System.exit(0);
	}
}
