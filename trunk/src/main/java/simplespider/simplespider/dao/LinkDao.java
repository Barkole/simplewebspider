package simplespider.simplespider.dao;

import simplespider.simplespider.enity.Link;

public interface LinkDao {

	public abstract Link getByUrl(String url);

	public abstract Link getNext();

	public abstract boolean isAvailable(String url);

	public abstract void save(Link link);
}