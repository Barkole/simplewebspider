package simplespider.simplespider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
	private static final int	WAIT_FOR_THREAD_ON_SHUTDOWN	= 1;
	private static final int		MAX_CURRENT_THREADS		= 4;
	private static final int		MAX_THREADS_PER_MINUTE	= 10;
	private static final Log		LOG						= LogFactory.getLog(Main.class);

	private volatile boolean		cancled					= false;
	private final DbHelperFactory	dbHelperFactory;
	final HttpClientFactory			httpClientFactory;

	private Main(final DbHelperFactory dbHelperFactory, final HttpClientFactory httpClientFactory) {
		this.dbHelperFactory = dbHelperFactory;
		this.httpClientFactory = httpClientFactory;
	}

	private void startCancleListener() {
		final Thread listener = new Thread() {
			@Override
			public void run() {
				LOG.info("Stop crawler with ENTER...");

				try {
					new BufferedReader(new InputStreamReader(System.in)).readLine();
				} catch (final IOException e) {
					LOG.fatal("Failed to listen to input stream", e);
				}
				Main.this.cancled = true;

				LOG.info("Invoke stopping crawler...");
			}

		};

		listener.setDaemon(true); // Ensure, that this thread will be terminated, if main is not running
		listener.start();
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
			LOG.debug("Start crawling URL: \"" + baseUrl + "\"");

			final LinkExtractor extractor = new LinkExtractorImpl();
			final Crawler crawler = new CrawlerImpl(this.dbHelperFactory, extractor, this.httpClientFactory);
			threadPool.execute(new CrawlerRunner(crawler, baseUrl));
		}

		LOG.info("Invoke shutting down threads...");
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
		System.exit(0);
	}
}
