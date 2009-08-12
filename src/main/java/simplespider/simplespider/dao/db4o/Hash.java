package simplespider.simplespider.dao.db4o;

/**
 * This entity will be used to avoid duplicate inserts of urls
 */
class Hash {
	public static final String	HASH	= "hash";
	private String				hash;

	Hash(final String hash) {
		this.hash = hash;
	}

	public String getHash() {
		return this.hash;
	}

	public void setHash(final String hash) {
		this.hash = hash;
	}
}
