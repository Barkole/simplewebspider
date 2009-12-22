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

package simplespider.simplespider.importing.simplefile;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.util.SimpleUrl;
import simplespider.simplespider.util.ValidityHelper;

public class SimpleFileImporter implements Iterator<String>, Iterable<String> {

	private static final String	BOOTSTRAP_SIMPLE_FILE_FILE_NAME			= "bootstrap.simple-file.file-name";
	private static final String	BOOTSTRAP_SIMPLE_FILE_FILE_NAME_DEFAULT	= "bootstrapping.txt";

	private static final Log	LOG										= LogFactory.getLog(SimpleFileImporter.class);

	private final Configuration	configuration;
	private String				filename;
	private FileReader			fstream;
	private BufferedReader		br;
	private String				nextLine;

	public SimpleFileImporter(final Configuration configuration) {
		this.configuration = configuration;

		init();
	}

	private void init() {
		this.filename = this.configuration.getString(BOOTSTRAP_SIMPLE_FILE_FILE_NAME, BOOTSTRAP_SIMPLE_FILE_FILE_NAME_DEFAULT);
		if (ValidityHelper.isEmpty(this.filename)) {
			LOG.warn("Configuration " + BOOTSTRAP_SIMPLE_FILE_FILE_NAME + " is invalid. Using default value: " + BOOTSTRAP_SIMPLE_FILE_FILE_NAME);
			this.filename = BOOTSTRAP_SIMPLE_FILE_FILE_NAME_DEFAULT;
		}
		// Open the file that is the first 
		// command line parameter
		try {
			this.fstream = new FileReader(this.filename);
			if (LOG.isInfoEnabled()) {
				LOG.info("Bootstraping from file \"" + this.filename + "\"");
			}
		} catch (final FileNotFoundException e) {
			LOG.warn("Import links fails: Failed to open file \"" + this.filename + "\"", e);
			this.fstream = null;
		}

		this.br = new BufferedReader(this.fstream);

		loadNext();
	}

	private void loadNext() {
		boolean readNextLine = true;
		while (readNextLine) {
			readNextLine = false;

			String strLine;
			try {
				strLine = this.br.readLine();
			} catch (final IOException e) {
				LOG.warn("Failed to read line from file " + this.filename, e);
				strLine = null;
			}

			if (strLine == null) {
				this.nextLine = null;
				if (this.br != null) {
					try {
						this.br.close();
					} catch (final IOException e) {
						LOG.error("Failed to close buffered reader", e);
					}
					this.br = null;
				}
				if (this.fstream != null) {
					try {
						this.fstream.close();
					} catch (final IOException e) {
						LOG.error("Failed to close file " + this.filename, e);
					}
					this.fstream = null;
				}
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Try to import link \"" + strLine + "\"");
				}
				final SimpleUrl simpleUrl;
				try {
					simpleUrl = new SimpleUrl(strLine);
					this.nextLine = simpleUrl.toNormalform(false, true);
				} catch (final Exception e) {
					LOG.warn("Skipping link \"" + strLine + "\": Not valid", e);
					readNextLine = true;
				}
			}
		}
	}

	@Override
	public boolean hasNext() {
		return this.nextLine != null;
	}

	@Override
	public String next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		final String result = this.nextLine;
		loadNext();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<String> iterator() {
		return this;
	}
}
