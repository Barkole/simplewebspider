package simplespider.simplespider.bot;

import simplespider.simplespider.util.ValidityHelper;

public class CrawlerRunner implements Runnable {

	private final Crawler	crawler;
	private final String	url;

	public CrawlerRunner(final Crawler crawler, final String url) {
		ValidityHelper.checkNotNull("crawler", crawler);
		ValidityHelper.checkNotEmpty("url", url);

		this.crawler = crawler;
		this.url = url;
	}

	@Override
	public void run() {
		this.crawler.crawl(this.url);
	}

}
