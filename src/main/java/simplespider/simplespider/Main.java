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
import simplespider.simplespider.dao.jdbc.JdbcDbHelperFactory;
import simplespider.simplespider.enity.Link;

/**
 * Hello world!
 */
public class Main {
	private static final int	BOOTSTRAPPING_LINK_MAX_ID	= 1000;
	private static final String	PID_FILENAME_KEY			= "sws.daemon.pidfile";
	private static final String	PID_FILENAME_DEFAULT		= "simple-web-spider.pid";
	private static final int	WAIT_FOR_THREAD_ON_SHUTDOWN	= 1;
	private static final int	MAX_CURRENT_THREADS			= 4;
	private static final int	MAX_THREADS_PER_MINUTE		= 10;
	private static final Log	LOG							= LogFactory.getLog(Main.class);

	private static Thread		mainThread;

	private static Thread getMainDaemonThread() {
		return mainThread;
	}

	private volatile boolean		cancled	= false;
	private final DbHelperFactory	dbHelperFactory;
	final HttpClientFactory			httpClientFactory;

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
		final Thread listener = new Thread() {
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

		listener.setDaemon(true);

		Runtime.getRuntime().addShutdownHook(listener);
	}

	private void runCrawler() throws SQLException {
		final DbHelper db = this.dbHelperFactory.buildDbHelper();

		final LimitThroughPut limitThroughPut = new LimitThroughPut(Main.MAX_THREADS_PER_MINUTE);
		{
			final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(MAX_CURRENT_THREADS, MAX_CURRENT_THREADS, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());

			LOG.info("Bootstrapping (all LINK entries with id lower than " + BOOTSTRAPPING_LINK_MAX_ID + ")");
			runCrawler(this.dbHelperFactory, this.httpClientFactory, threadPool, limitThroughPut, true);

			// Waiting for ending of all bootstrapping jobs
			LOG.info("Invoke shutting down bootstrapping threads...");
			threadPool.shutdown();
			LOG.info("Waiting for ending of all bootstrapping jobs");
			try {
				threadPool.awaitTermination(WAIT_FOR_THREAD_ON_SHUTDOWN, TimeUnit.MINUTES);
			} catch (final InterruptedException e) {
				LOG.warn("failed to wait for ending of all threads", e);
			}
		}

		{
			final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(MAX_CURRENT_THREADS, MAX_CURRENT_THREADS, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());

			runCrawler(this.dbHelperFactory, this.httpClientFactory, threadPool, limitThroughPut, false);

			LOG.info("Invoke shutting down threads...");
			threadPool.shutdown();
			try {
				threadPool.awaitTermination(WAIT_FOR_THREAD_ON_SHUTDOWN, TimeUnit.MINUTES);
			} catch (final InterruptedException e) {
				LOG.warn("failed to wait for ending of all threads", e);
			}
			threadPool.shutdownNow();
		}
		db.shutdown();
		LOG.info("Crawler stops");
	}

	private void runCrawler(final DbHelperFactory dbHelperFactory, final HttpClientFactory httpClientFactory, final ThreadPoolExecutor threadPool,
			final LimitThroughPut limitThroughPut, final boolean bootstrapping) throws SQLException {
		final DbHelper db = dbHelperFactory.buildDbHelper();
		final LinkDao linkDao = db.getLinkDao();

		int retryCountOnNoLinks = 0;
		while (!this.cancled) {
			// Block while to much threads were working in last minute
			limitThroughPut.next();

			final Link next = bootstrapping ? linkDao.getNextUpToId(BOOTSTRAPPING_LINK_MAX_ID) : linkDao.getNext();
			if (next == null) {
				// On bootstrapping don't do any retry, if no more links are available
				if (bootstrapping) {
					LOG.info("Bootstrapping: No more links available...");
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
				LOG.info("No more links available... Waiting for running thread and retry... Count " + retryCountOnNoLinks);
				try {
					threadPool.awaitTermination(WAIT_FOR_THREAD_ON_SHUTDOWN, TimeUnit.MINUTES);
				} catch (final InterruptedException e) {
					LOG.warn("failed to wait for ending of all threads", e);
				}
				continue;
			} else {
				retryCountOnNoLinks = 0;
			}

			next.setDone(true);
			linkDao.save(next);
			db.commitTransaction();

			final String baseUrl = next.getUrl();
			LOG.info("Start crawling URL: \"" + baseUrl + "\"");

			final LinkExtractor extractor = new StreamExtractor();
			final Crawler crawler = new CrawlerImpl(dbHelperFactory, extractor, httpClientFactory);
			threadPool.execute(new CrawlerRunner(crawler, baseUrl));
		}
	}

	public static void main(final String[] args) throws Exception {
		try {
			// do sanity checks and startup actions
			daemonize();
		} catch (final Throwable e) {
			LOG.fatal("Startup failed", e);
		}

		final DbHelperFactory dbHelperFactory = new JdbcDbHelperFactory();

		final HttpClientFactory httpClientFactory;
		if (args.length == 2) {
			httpClientFactory = new ApacheHttpClientFactory(args[0], Integer.parseInt(args[1]));
		} else {
			httpClientFactory = new ApacheHttpClientFactory();
		}

		final Main main = new Main(dbHelperFactory, httpClientFactory);
		main.startCancleListener();
		main.runCrawler();
	}
}
