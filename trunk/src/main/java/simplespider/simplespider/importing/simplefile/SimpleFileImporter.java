package simplespider.simplespider.importing.simplefile;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.dao.DbHelper;
import simplespider.simplespider.dao.DbHelperFactory;
import simplespider.simplespider.dao.LinkDao;
import simplespider.simplespider.importing.EntityImporter;
import simplespider.simplespider.util.SimpleUrl;

public class SimpleFileImporter implements EntityImporter {

	private static final Log	LOG	= LogFactory.getLog(SimpleFileImporter.class);

	private final String		filename;

	public SimpleFileImporter(final String filename) {
		this.filename = filename;
	}

	@Override
	public long importLink(final DbHelperFactory dbHelperFactory) {
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
			final DbHelper dbHelper = dbHelperFactory.buildDbHelper();
			try {
				final BufferedReader br = new BufferedReader(fstream);
				try {

					final LinkDao linkDao = dbHelper.getLinkDao();

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

						try {
							linkDao.saveForced(normalizedUrl);
							count++;
							if (LOG.isInfoEnabled()) {
								LOG.info("Import link \"" + strLine + "\" (normalized: \"" + normalizedUrl + "\")");
							}
						} catch (final RuntimeException e) {
							LOG.warn("Failed to import \"" + strLine + "\" (normalized: \"" + normalizedUrl + "\")", e);
							try {
								dbHelper.rollbackTransaction();
							} catch (final Exception e2) {
								LOG.warn("Failed to rollback database transaction", e2);
							}
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
					dbHelper.close();
				} catch (final Exception e) {
					LOG.warn("Failed to close database connection", e);
				}
			}
		} catch (final SQLException e) {
			LOG.error("Failed to open conenction to database", e);
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
