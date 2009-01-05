package simplespider.simplespider;

import java.io.File;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.bot.Crawler;
import simplespider.simplespider.bot.CrawlerImpl;
import simplespider.simplespider.bot.CrawlerRunner;
import simplespider.simplespider.bot.LinkExtractor;
import simplespider.simplespider.bot.LinkExtractorImpl;
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
		final LinkDao linkDao = db.getLinkDao();

		final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_CURRENT_THREADS);
		final LimitThroughPut limitThroughPut = new LimitThroughPut(Main.MAX_THREADS_PER_MINUTE);

		while (!this.cancled) {
			// Block while to much threads were working in last minute
			limitThroughPut.next();

			final Link next = linkDao.getNext();
			if (next == null) {
				LOG.fatal("No more links available...");
				break;
			}

			next.setDone(true);
			linkDao.save(next);
			db.commitTransaction();

			final String baseUrl = next.getUrl();
			LOG.info("Start crawling URL: \"" + baseUrl + "\"");

			final LinkExtractor extractor = new LinkExtractorImpl();
			final Crawler crawler = new CrawlerImpl(this.dbHelperFactory, extractor, this.httpClientFactory);
			threadPool.execute(new CrawlerRunner(crawler, baseUrl));
		}

		LOG.warn("Invoke shutting down threads...");
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(WAIT_FOR_THREAD_ON_SHUTDOWN, TimeUnit.MINUTES);
		} catch (final InterruptedException e) {
			LOG.warn("failed to wait for ending of al threads", e);
		}
		threadPool.shutdownNow();
		db.shutdown();
		LOG.info("Crawler stops");
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
