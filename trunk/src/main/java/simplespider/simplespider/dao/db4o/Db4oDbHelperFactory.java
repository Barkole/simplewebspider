package simplespider.simplespider.dao.db4o;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.dao.DbHelper;
import simplespider.simplespider.dao.DbHelperFactory;

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

	private static final Log	LOG	= LogFactory.getLog(Db4oDbHelperFactory.class);

	private ObjectServer		server;

	public Db4oDbHelperFactory(final String filename) {

		final ServerConfiguration dbConfig = Db4oClientServer.newServerConfiguration();
		dbConfig.common().objectClass(Link.class).objectField(Link.RANDOMIZER).indexed(true);
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
		 * The shutdown hook does auto-commit. We do NOT want auto-commit: if a
		 * transaction hasn't commit()ed, it's not safe to commit it. 
		 * Add our own hook to rollback and close... */
		dbConfig.common().automaticShutDown(true);

		// LAZY appears to cause ClassCastException's relating to db4o objects inside db4o code. :(
		// Also it causes duplicates if we activate immediately.
		// And the performance gain for e.g. RegisterMeRunner isn't that great.
		//		dbConfig.queries().evaluationMode(QueryEvaluationMode.LAZY);
		//		dbConfig.common().queries().evaluationMode(QueryEvaluationMode.SNAPSHOT);

		//		dbConfig.file().blobPath("blob.db4o");
		//		dbConfig.file().databaseGrowthSize(1024);
		//		dbConfig.file().freespace().useBTreeSystem();

		//		dbConfig.cache().slotCacheSize(64);
		// Try to reduce cachig size
		//		final int cachePageCount = 4;
		//		final int cachePageSize = 1024;
		//		final CachingStorage storage = new CachingStorage(new FileStorage(), cachePageCount, cachePageSize) {
		//			// By overriding the newCache method you can plug in a different cache
		//			@Override
		//			protected Cache4<Object, Object> newCache() {
		//				return CacheFactory.new2QXCache(cachePageCount);
		//			}
		//		};
		//		final FileStorage storage = new FileStorage();
		//		final NonFlushingStorage storage = new NonFlushingStorage(new FileStorage());
		//		final FileConfiguration fileConfiguration = dbConfig.file();
		//		fileConfiguration.storage(storage);

		//		dbConfig.file().storage().

		/* Block size 8 should have minimal impact since pointers are this
		 * long, and allows databases of up to 16GB. 
		 * FIXME make configurable by user. */
		dbConfig.file().blockSize(8);

		if (LOG.isInfoEnabled()) {
			LOG.info("Start defragmentation database file... This could took some while...");
		}
		try {
			Defragment.defrag(filename);
		} catch (final IOException e) {
			LOG.warn("Failed to defragment database file \"" + filename + "\"", e);
		}

		if (LOG.isInfoEnabled()) {
			LOG.info("Opening database file... This could took some while...");
		}
		try {
			this.server = Db4oClientServer.openServer(dbConfig, filename, 0);
		} catch (final RuntimeException e) {
			throw new RuntimeException("Failed to open database file \"" + filename + "\"", e);
		}

		if (LOG.isInfoEnabled()) {
			final ObjectContainer container = getConnection();
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
				LOG.info("Total size: " + systemInfo.totalSize());
				LOG.info("Freespace size: " + systemInfo.freespaceSize());
				LOG.info("Freespace entry count: " + systemInfo.freespaceEntryCount());

			} finally {
				container.close();
			}
		}
	}

	@Override
	public DbHelper buildDbHelper() throws SQLException {
		final Db4oDbHelper dbHelper = new Db4oDbHelper();

		dbHelper.createConnection(this);
		return dbHelper;
	}

	ObjectContainer getConnection() {
		return this.server.openClient();
	}

	void shutdown() {
		this.server.close();
		this.server = null;
	}

}