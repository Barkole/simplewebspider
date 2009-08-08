package simplespider.simplespider.importing;

import simplespider.simplespider.dao.LinkDao;

public interface EntityImporter {
	long importLink(LinkDao linkDao);
}
