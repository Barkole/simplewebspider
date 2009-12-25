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

package simplespider.simplespider.dao.db4o;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.dao.DbHelper;
import simplespider.simplespider.dao.DbHelperFactory;
import simplespider.simplespider.throttle.host.HostThrottler;
import simplespider.simplespider.util.ValidityHelper;

import com.db4o.ObjectContainer;
import com.db4o.ObjectServer;
import com.db4o.constraints.UniqueFieldValueConstraint;
import com.db4o.cs.Db4oClientServer;
import com.db4o.cs.config.ServerConfiguration;
import com.db4o.defragment.Defragment;
import com.db4o.diagnostic.ClassHasNoFields;
import com.db4o.diagnostic.Diagnostic;
import com.db4o.diagnostic.DiagnosticBase;
import com.db4o.diagnostic.DiagnosticListener;
import com.db4o.ext.ExtObjectContainer;
import com.db4o.ext.SystemInfo;
import com.db4o.reflect.ReflectClass;

public class Db4oDbHelperFactory implements DbHelperFactory {

	private static final Log		LOG												= LogFactory.getLog(Db4oDbHelperFactory.class);

	private static final String		DATABASE_DEFRAG_ON_STARTUP_URL_QUEUE			= "database.db4o.queue.defrag-on-startup";
	private static final boolean	DATABASE_DEFRAG_ON_STARTUP_URL_QUEUE_DEFAULT	= false;

	private static final String		DATABASE_DEFRAG_ON_STARTUP_URL_HASHES			= "database.db4o.hashes.defrag-on-startup";
	private static final boolean	DATABASE_DEFRAG_ON_STARTUP_URL_HASHES_DEFAULT	= false;

	private static final String		DATABASE_FILE_NAME_URL_QUEUE					= "database.db4o.queue.file-name";
	private static final String		DATABASE_FILE_NAME_URL_QUEUE_DEFAULT			= "sws.queue.db4o";

	private static final String		DATABASE_FILE_NAME_URL_HASHES					= "database.db4o.hashes.file-name";
	private static final String		DATABASE_FILE_NAME_URL_HASHES_DEFAULT			= "sws.hashes.db4o";

	private static final String		DATABASE_BLOCKSIZE_URL_QUEUE					= "database.db4o.queue.blocksize";
	private static final int		DATABASE_BLOCKSIZE_URL_QUEUE_DEFAULT			= 8;

	private static final String		DATABASE_BLOCKSIZE_URL_HASHES					= "database.db4o.hashes.blocksize";
	private static final int		DATABASE_BLOCKSIZE_URL_HASHES_DEFAULT			= 8;

	private ObjectServer			serverQueue;
	private ObjectServer			serverHashes;

	private final HostThrottler		hostThrottler;

	public Db4oDbHelperFactory(final Configuration configuration, final HostThrottler hostThrottler) {
		this.hostThrottler = hostThrottler;
		buildQueueServer(configuration);
		buildHashesServer(configuration);
	}

	private void buildQueueServer(final Configuration configuration) {
		String filename = configuration.getString(DATABASE_FILE_NAME_URL_QUEUE, DATABASE_FILE_NAME_URL_QUEUE_DEFAULT);
		if (ValidityHelper.isEmpty(filename)) {
			LOG.warn("Configuration " + DATABASE_FILE_NAME_URL_QUEUE + " is invalid. Using default value: " + DATABASE_FILE_NAME_URL_QUEUE_DEFAULT);
			filename = DATABASE_FILE_NAME_URL_QUEUE_DEFAULT;
		}
		int blockSizeBytes = configuration.getInt(DATABASE_BLOCKSIZE_URL_QUEUE, DATABASE_BLOCKSIZE_URL_QUEUE_DEFAULT);
		if (blockSizeBytes < 1 || blockSizeBytes > 127) {
			LOG.warn("Configuration " + DATABASE_BLOCKSIZE_URL_QUEUE + " is invalid. Using default value: " + DATABASE_BLOCKSIZE_URL_QUEUE_DEFAULT);
			blockSizeBytes = DATABASE_BLOCKSIZE_URL_QUEUE_DEFAULT;
		}
		final boolean defragDatabase = configuration.getBoolean(DATABASE_DEFRAG_ON_STARTUP_URL_QUEUE, DATABASE_DEFRAG_ON_STARTUP_URL_QUEUE_DEFAULT);

		final ServerConfiguration dbConfig = Db4oClientServer.newServerConfiguration();

		dbConfig.common().allowVersionUpdates(true);
		dbConfig.common().detectSchemaChanges(true);
		dbConfig.common().exceptionsOnNotStorable(true);
		dbConfig.common().optimizeNativeQueries(true);
		dbConfig.common().messageLevel(1);
		dbConfig.common().activationDepth(1);

		if (LOG.isDebugEnabled()) {
			dbConfig.common().diagnostic().addListener(new DiagnosticListener() {
				public void onDiagnostic(final Diagnostic arg0) {

					if (arg0 instanceof ClassHasNoFields) {
						return; // Ignore
					}
					if (arg0 instanceof DiagnosticBase) {
						final DiagnosticBase d = (DiagnosticBase) arg0;
						LOG.debug("Diagnostic: " + d.getClass() + " : " + d.problem() + " : " + d.solution() + " : " + d.reason(), new Exception(
								"debug"));
					} else {
						LOG.debug("Diagnostic: " + arg0 + " : " + arg0.getClass(), new Exception("debug"));
					}
				}
			});
		}

		/* TURN OFF SHUTDOWN HOOK.
		 * The shutdown hook does auto-commit. 
		 * And it close database after getting signal (or pressing CTRL-C) */
		dbConfig.common().automaticShutDown(false);

		// LAZY appears to cause ClassCastException's relating to db4o objects inside db4o code. :(
		// Also it causes duplicates if we activate immediately.
		// And the performance gain for e.g. RegisterMeRunner isn't that great.
		//		dbConfig.queries().evaluationMode(QueryEvaluationMode.LAZY);
		//		dbConfig.common().queries().evaluationMode(QueryEvaluationMode.SNAPSHOT);

		// Reduce memory usage. After running eight hours with an 350 MB DB (than 400 MB) freeslot requires 14 MB instead of 0.4 MB on application startup
		dbConfig.file().freespace().useBTreeSystem();

		/* Block size 8 should have minimal impact since pointers are this
		 * long, and allows databases of up to 16GB. */
		dbConfig.file().blockSize(blockSizeBytes);

		if (defragDatabase) {
			if (LOG.isInfoEnabled()) {
				LOG.info("Start defragmentation database file... This could took some while...");
			}
			try {
				Defragment.defrag(filename);
			} catch (final IOException e) {
				LOG.warn("Failed to defragment database file \"" + filename + "\"", e);
			}
		}

		if (LOG.isInfoEnabled()) {
			LOG.info("Opening database file... This could took some while...");
		}
		try {
			this.serverQueue = Db4oClientServer.openServer(dbConfig, filename, 0);
		} catch (final RuntimeException e) {
			throw new RuntimeException("Failed to open database file \"" + filename + "\"", e);
		}

		if (LOG.isInfoEnabled()) {
			final ObjectContainer container = getQueueConnection();
			try {
				final ExtObjectContainer ext = container.ext();

				final ReflectClass[] knownClasses = ext.knownClasses();
				final StringBuffer sb = new StringBuffer();
				sb.append("Known classes: ");
				for (final ReflectClass knownClass : knownClasses) {
					sb.append(knownClass.getName());
					sb.append(";");
				}
				LOG.info(sb.toString());

				final SystemInfo systemInfo = ext.systemInfo();
				LOG.info("Total size (byte): " + systemInfo.totalSize());
				LOG.info("Freespace size (byte): " + systemInfo.freespaceSize());
				LOG.info("Freespace entry count: " + systemInfo.freespaceEntryCount());

			} finally {
				container.close();
			}
		}
	}

	private void buildHashesServer(final Configuration configuration) {
		String filename = configuration.getString(DATABASE_FILE_NAME_URL_HASHES, DATABASE_FILE_NAME_URL_HASHES_DEFAULT);
		if (ValidityHelper.isEmpty(filename)) {
			LOG.warn("Configuration " + DATABASE_FILE_NAME_URL_HASHES + " is invalid. Using default value: " + DATABASE_FILE_NAME_URL_HASHES_DEFAULT);
			filename = DATABASE_FILE_NAME_URL_HASHES_DEFAULT;
		}
		int blockSizeBytes = configuration.getInt(DATABASE_BLOCKSIZE_URL_HASHES, DATABASE_BLOCKSIZE_URL_HASHES_DEFAULT);
		if (blockSizeBytes < 1 || blockSizeBytes > 127) {
			LOG.warn("Configuration " + DATABASE_BLOCKSIZE_URL_HASHES + " is invalid. Using default value: " + DATABASE_BLOCKSIZE_URL_HASHES_DEFAULT);
			blockSizeBytes = DATABASE_BLOCKSIZE_URL_HASHES_DEFAULT;
		}
		final boolean defragDatabase = configuration.getBoolean(DATABASE_DEFRAG_ON_STARTUP_URL_HASHES, DATABASE_DEFRAG_ON_STARTUP_URL_HASHES_DEFAULT);

		final ServerConfiguration dbConfig = Db4oClientServer.newServerConfiguration();
		dbConfig.common().objectClass(Hash.class).objectField(Hash.HASH).indexed(true);
		dbConfig.common().add(new UniqueFieldValueConstraint(Hash.class, Hash.HASH));

		dbConfig.common().allowVersionUpdates(true);
		dbConfig.common().detectSchemaChanges(true);
		dbConfig.common().exceptionsOnNotStorable(true);
		dbConfig.common().optimizeNativeQueries(true);
		dbConfig.common().messageLevel(1);
		dbConfig.common().activationDepth(1);

		if (LOG.isDebugEnabled()) {
			dbConfig.common().diagnostic().addListener(new DiagnosticListener() {
				public void onDiagnostic(final Diagnostic arg0) {

					if (arg0 instanceof ClassHasNoFields) {
						return; // Ignore
					}
					if (arg0 instanceof DiagnosticBase) {
						final DiagnosticBase d = (DiagnosticBase) arg0;
						LOG.debug("Diagnostic: " + d.getClass() + " : " + d.problem() + " : " + d.solution() + " : " + d.reason(), new Exception(
								"debug"));
					} else {
						LOG.debug("Diagnostic: " + arg0 + " : " + arg0.getClass(), new Exception("debug"));
					}
				}
			});
		}

		/* TURN OFF SHUTDOWN HOOK.
		 * The shutdown hook does auto-commit. 
		 * And it close database after getting signal (or pressing CTRL-C) */
		dbConfig.common().automaticShutDown(false);

		// LAZY appears to cause ClassCastException's relating to db4o objects inside db4o code. :(
		// Also it causes duplicates if we activate immediately.
		// And the performance gain for e.g. RegisterMeRunner isn't that great.
		//		dbConfig.queries().evaluationMode(QueryEvaluationMode.LAZY);
		//		dbConfig.common().queries().evaluationMode(QueryEvaluationMode.SNAPSHOT);

		// Reduce memory usage. After running eight hours with an 350 MB DB (than 400 MB) freeslot requires 14 MB instead of 0.4 MB on application startup
		dbConfig.file().freespace().useBTreeSystem();

		/* Block size 8 should have minimal impact since pointers are this
		 * long, and allows databases of up to 16GB. */
		dbConfig.file().blockSize(blockSizeBytes);

		if (defragDatabase) {
			if (LOG.isInfoEnabled()) {
				LOG.info("Start defragmentation database file... This could took some while...");
			}
			try {
				Defragment.defrag(filename);
			} catch (final IOException e) {
				LOG.warn("Failed to defragment database file \"" + filename + "\"", e);
			}
		}

		if (LOG.isInfoEnabled()) {
			LOG.info("Opening database file... This could took some while...");
		}
		try {
			this.serverHashes = Db4oClientServer.openServer(dbConfig, filename, 0);
		} catch (final RuntimeException e) {
			throw new RuntimeException("Failed to open database file \"" + filename + "\"", e);
		}

		if (LOG.isInfoEnabled()) {
			final ObjectContainer container = getHashesConnection();
			try {
				final ExtObjectContainer ext = container.ext();

				final ReflectClass[] knownClasses = ext.knownClasses();
				final StringBuffer sb = new StringBuffer();
				sb.append("Known classes: ");
				for (final ReflectClass knownClass : knownClasses) {
					sb.append(knownClass.getName());
					sb.append(";");
				}
				LOG.info(sb.toString());

				final SystemInfo systemInfo = ext.systemInfo();
				LOG.info("Total size (byte): " + systemInfo.totalSize());
				LOG.info("Freespace size (byte): " + systemInfo.freespaceSize());
				LOG.info("Freespace entry count: " + systemInfo.freespaceEntryCount());

			} finally {
				container.close();
			}
		}
	}

	@Override
	public DbHelper buildDbHelper() throws SQLException {
		final Db4oDbHelper dbHelper = new Db4oDbHelper(this.hostThrottler);

		dbHelper.createConnection(this);
		return dbHelper;
	}

	ObjectContainer getQueueConnection() {
		return this.serverQueue.openClient();
	}

	ObjectContainer getHashesConnection() {
		return this.serverHashes.openClient();
	}

	void shutdown() {
		this.serverHashes.close();
		this.serverHashes = null;

		this.serverQueue.close();
		this.serverQueue = null;
	}

}
