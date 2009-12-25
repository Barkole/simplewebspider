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
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
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
import simplespider.simplespider.importing.simplefile.SimpleFileImporter;
import simplespider.simplespider.throttle.host.HostThrottler;
import simplespider.simplespider.throttle.host.simple.SimpleHostThrottler;

/**
 * Hello world!
 */
public class Main {

	private static final Log	LOG										= LogFactory.getLog(Main.class);

	private static final String	PID_FILENAME_KEY						= "sws.daemon.pidfile";
	private static final String	PID_FILENAME_DEFAULT					= "simple-web-spider.pid";

	private static final String	BOT_MAX_CONCURRENT						= "bot.max_concurrent";
	private static final int	BOT_MAX_CONCURRENT_DEFAULT				= 4;

	private static final String	BOT_SHUTDOWN_MAX_WAIT_SECONDS			= "bot.shutdown-max-wait-seconds";
	private static final int	BOT_SHUTDOWN_MAX_WAIT_SECONDS_DEFAULT	= 180;

	private static Thread		mainThread;

	private static Thread getMainDaemonThread() {
		return mainThread;
	}

	private volatile boolean		cancled	= false;
	private final DbHelperFactory	dbHelperFactory;
	private final HttpClientFactory	httpClientFactory;
	private final Configuration		configuration;
	private Thread					listener;

	private Main(final DbHelperFactory dbHelperFactory, final HttpClientFactory httpClientFactory, final Configuration configuration) {
		this.dbHelperFactory = dbHelperFactory;
		this.httpClientFactory = httpClientFactory;
		this.configuration = configuration;
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
		int maxThreadPoolSize = this.configuration.getInt(BOT_MAX_CONCURRENT, BOT_MAX_CONCURRENT_DEFAULT);
		if (maxThreadPoolSize <= 0) {
			LOG.warn("Configuration " + BOT_MAX_CONCURRENT + " is invalid. Using default value: " + BOT_MAX_CONCURRENT_DEFAULT);
			maxThreadPoolSize = BOT_MAX_CONCURRENT_DEFAULT;
		}

		if (LOG.isInfoEnabled()) {
			LOG.info("Start crawler...");
			LOG.info("Open database connection... This could took time...");
		}
		final DbHelper db = this.dbHelperFactory.buildDbHelper();
		try {
			final LimitThroughPut limitThroughPut = new LimitThroughPut(this.configuration);
			if (LOG.isInfoEnabled()) {
				LOG.info("Crawl LINK entries...");
			}

			final int waitForThreadOnShutdownSeconds = this.configuration
					.getInt(BOT_SHUTDOWN_MAX_WAIT_SECONDS, BOT_SHUTDOWN_MAX_WAIT_SECONDS_DEFAULT);
			final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(maxThreadPoolSize, maxThreadPoolSize, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());

			runCrawler(this.dbHelperFactory, this.httpClientFactory, threadPool, limitThroughPut, waitForThreadOnShutdownSeconds);

			if (LOG.isInfoEnabled()) {
				LOG.info("Invoke shutting down threads...");
			}
			threadPool.shutdown();
			try {
				threadPool.awaitTermination(waitForThreadOnShutdownSeconds, TimeUnit.NANOSECONDS);
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

	private void runCrawler(final DbHelperFactory dbHelperFactory, final HttpClientFactory httpClientFactory, final ThreadPoolExecutor threadPool,
			final LimitThroughPut limitThroughPut, final int waitForThreadOnShutdownSeconds) throws SQLException {
		final DbHelper db = dbHelperFactory.buildDbHelper();
		final Iterator<String> bootstrap = new SimpleFileImporter(this.configuration);
		try {
			final LinkDao linkDao = db.getLinkDao();

			int retryCountOnNoLinks = 0;
			while (!this.cancled) {
				// Block while to much threads were working in last minute
				limitThroughPut.next();

				// Check for next link, if there is none, wait and try again
				String next;
				try {
					// At first use bootstrap URLs, if there none, so try database queue
					if (bootstrap.hasNext()) {
						next = bootstrap.next();
					} else {
						next = linkDao.removeNextAndCommit();
					}
				} catch (final RuntimeException e) {
					LOG.error("Failed to get next url", e);
					try {
						db.rollbackTransaction();
					} catch (final Exception e2) {
						LOG.error("Failed to rollback database transaction", e2);
					}
					next = null;
				}

				if (next == null) {
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
						threadPool.awaitTermination(waitForThreadOnShutdownSeconds, TimeUnit.SECONDS);
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

				final LinkExtractor extractor = new StreamExtractor(this.configuration);
				final Crawler crawler = new CrawlerImpl(dbHelperFactory, extractor, httpClientFactory, this.configuration);
				threadPool.execute(new CrawlerRunner(crawler, next));
			}
		} finally {
			try {
				db.close();
			} catch (final Exception e) {
				LOG.warn("Failed to cloase database connection", e);
			}
		}
	}

	private static Configuration loadConfiguration() {
		try {
			return new PropertiesConfiguration("simple-web-spider.properties");
		} catch (final ConfigurationException e) {
			LOG.warn("Failed to load configuration: use only defaults", e);
		}
		return new BaseConfiguration();
	}

	public static void main(final String[] args) throws Exception {
		if (LOG.isInfoEnabled()) {
			LOG.info("Starting program...");
		}

		final Configuration configuration = loadConfiguration();

		try {
			// do sanity checks and startup actions
			daemonize();
		} catch (final Throwable e) {
			LOG.fatal("Startup failed", e);
		}

		final HostThrottler hostThrottler = new SimpleHostThrottler(configuration);
		final DbHelperFactory dbHelperFactory = new Db4oDbHelperFactory(configuration, hostThrottler);

		final HttpClientFactory httpClientFactory;
		if (args.length == 2) {
			httpClientFactory = new ApacheHttpClientFactory(configuration, args[0], Integer.parseInt(args[1]));
		} else {
			httpClientFactory = new ApacheHttpClientFactory(configuration);
		}

		final Main main = new Main(dbHelperFactory, httpClientFactory, configuration);
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
