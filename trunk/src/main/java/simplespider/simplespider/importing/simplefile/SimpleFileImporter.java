package simplespider.simplespider.importing.simplefile;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.dao.LinkDao;
import simplespider.simplespider.enity.Link;
import simplespider.simplespider.importing.EntityImporter;
import simplespider.simplespider.util.SimpleUrl;

public class SimpleFileImporter implements EntityImporter {

	private static final Log	LOG	= LogFactory.getLog(SimpleFileImporter.class);

	private final String		filename;

	public SimpleFileImporter(final String filename) {
		this.filename = filename;
	}

	@Override
	public long importLink(final LinkDao linkDao) {
		// Open the file that is the first 
		// command line parameter
		final FileReader fstream;
		try {
			fstream = new FileReader(this.filename);
		} catch (final FileNotFoundException e) {
			LOG.warn("Import links fails: Failed to open file \"" + this.filename + "\"", e);
			return 0;
		}

		long count = 0;

		try {
			final BufferedReader br = new BufferedReader(fstream);
			try {
				String strLine;
				//Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Try to import link \"" + strLine + "\"");
					}

					final SimpleUrl simpleUrl;
					try {
						simpleUrl = new SimpleUrl(strLine);
					} catch (final RuntimeException e) {
						LOG.warn("Skipping link \"" + strLine + "\": Not valid", e);
						continue;
					}

					final String normalizedUrl = simpleUrl.toNormalform(false, true);

					final boolean available;
					try {
						available = linkDao.isAvailable(normalizedUrl);
					} catch (final RuntimeException e) {
						LOG.warn("Failed to import \"" + strLine + "\" (normalized: \"" + normalizedUrl + "\"): Failed to check if aready available",
								e);
						continue;
					}

					if (available) {
						if (LOG.isInfoEnabled()) {
							LOG.info("Skipping link \"" + strLine + "\" (normalized: \"" + normalizedUrl + "\"): Already available");
						}
						continue;
					}

					final Link link = new Link(normalizedUrl);
					link.setBootstrap(true);

					try {
						linkDao.save(link);
						count++;
						if (LOG.isInfoEnabled()) {
							LOG.info("Import link \"" + strLine + "\" (normalized: \"" + normalizedUrl + "\")");
						}
					} catch (final RuntimeException e) {
						LOG.warn("Failed to import \"" + strLine + "\" (normalized: \"" + normalizedUrl + "\")", e);
					}
				}
			} catch (final IOException e) {
				LOG.warn("Failure to read line of file \"" + this.filename + "\"", e);
			} finally {
				try {
					br.close();
				} catch (final IOException e) {
					LOG.warn("Failed to close buffer of file \"" + this.filename + "\"", e);
				}
			}
		} finally {
			try {
				fstream.close();
			} catch (final IOException e) {
				LOG.warn("Failed to close file \"" + this.filename + "\"", e);
			}
		}

		return count;
	}
}
