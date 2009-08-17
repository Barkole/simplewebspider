package simplespider.simplespider.importing;

import simplespider.simplespider.dao.DbHelperFactory;

public interface EntityImporter {
	long importLink(DbHelperFactory dbHelperFactory);
}
