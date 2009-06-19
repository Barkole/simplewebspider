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
package simplespider.simplespider.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import simplespider.simplespider.dao.DbHelper;
import simplespider.simplespider.dao.LinkDao;
import simplespider.simplespider.enity.Link;

public class JdbcLinkDao implements LinkDao {

	private static final String	UPDATE_LINK					= "UPDATE" //
																	+ " link" // 
																	+ " SET url = ?, done = ?, errors = ?" //
																	+ " WHERE id = ?";

	private static final String	INSERT_LINK					= "INSERT" //
																	+ " INTO link (url, done, errors)" //
																	+ " VALUES (?, ?, ?)";

	private static final String	SELECT_LINK_COUNT_BY_URL	= "SELECT count(ID) AS link_count" //
																	+ " FROM link" //
																	+ " WHERE url = ?" //
																	+ " LIMIT 1";

	private static final String	SELECT_LINK_BY_URL			= "SELECT *" //
																	+ " FROM link" //
																	+ " WHERE url = ?";

	private static final String	SELECT_LINK_WITH_MAX_ID		= "SELECT *" //
																	+ " FROM link" //
																	+ " WHERE done = false" //
																	+ "    AND id < ? " //
																	+ " ORDER BY rand()" //
																	+ " LIMIT 1";

	private static final String	SELECT_LINK					= "SELECT *" //
																	+ " FROM link" //
																	+ " WHERE done = false" //
																	+ " ORDER BY rand()" //
																	+ " LIMIT 1";

	private final DbHelper		db;

	public JdbcLinkDao(final DbHelper db) {
		this.db = db;
	}

	private Link createLink(final ResultSet selectResult) throws SQLException {
		final Link link = new Link();

		try {
			link.setId(Long.valueOf(selectResult.getLong("ID")));
		} catch (final SQLException e) {
			throw new SQLException("Failed to load link.id as Long", e);
		}

		try {
			if (selectResult.wasNull()) {
				link.setId(null);
			}
		} catch (final SQLException e) {
			throw new SQLException("Failed to load link.id as Long", e);
		}

		try {
			link.setUrl(selectResult.getString("URL"));
		} catch (final SQLException e) {
			throw new SQLException("Failed to load link.url as String", e);
		}

		try {
			link.setDone(selectResult.getBoolean("DONE"));
		} catch (final SQLException e) {
			throw new SQLException("Failed to load link.done as boolean", e);
		}

		try {
			link.setErrors(selectResult.getInt("ERRORS"));
		} catch (final SQLException e) {
			throw new SQLException("Failed to load link.errors as int", e);
		}

		return link;
	}

	@Override
	public Link getByUrl(final String url) {
		try {
			final PreparedStatement select = this.db.prepareStatement(SELECT_LINK_BY_URL);
			select.setString(1, url);

			return getLink(select, true);
		} catch (final SQLException e) {
			throw new RuntimeException("Failed to get link for url: " + url, e);
		}
	}

	@Override
	public Link getNext() {
		try {
			final PreparedStatement select = this.db.prepareStatement(SELECT_LINK);
			return getLink(select, false);
		} catch (final SQLException e) {
			throw new RuntimeException("Failed to get next link", e);
		}
	}

	@Override
	public Link getNextUpToId(final long maxId) {
		try {
			final PreparedStatement select = this.db.prepareStatement(SELECT_LINK_WITH_MAX_ID);
			select.setLong(1, maxId);
			return getLink(select, false);
		} catch (final SQLException e) {
			throw new RuntimeException("Failed to get next link", e);
		}
	}

	private Link getLink(final PreparedStatement selectLink, final boolean mustUnique) throws SQLException {
		final ResultSet selectResult = selectLink.executeQuery();
		if (!selectResult.next()) {
			return null;
		}

		final Link link = createLink(selectResult);

		if (mustUnique // 
				&& selectResult.next()) {
			throw new SQLException("Unique constrain voilation: More than one Link available");
		}

		selectResult.close();
		selectLink.close();

		return link;
	}

	@Override
	public boolean isAvailable(final String url) {
		try {
			final PreparedStatement select = this.db.prepareStatement(SELECT_LINK_COUNT_BY_URL);
			select.setString(1, url);
			final ResultSet selectResult = select.executeQuery();
			if (!selectResult.next()) {
				throw new SQLException("Failed to count entities for url " + url + ": No count result");
			}

			final int entityCount = selectResult.getInt("link_count");

			selectResult.close();
			select.close();

			return entityCount != 0;
		} catch (final SQLException e) {
			throw new RuntimeException("Failed to check url: " + url, e);
		}
	}

	@Override
	public void save(final Link link) {
		try {
			if (link.getId() == null) {
				// Save new link
				final String url = link.getUrl();
				final PreparedStatement insert = this.db.prepareStatement(INSERT_LINK);
				insert.setString(1, url);
				insert.setBoolean(2, link.isDone());
				insert.setInt(3, link.getErrors());
				insert.executeUpdate();
				insert.close();

				// get id of new link
				final Link insertedLink = getByUrl(url);
				if (insertedLink == null) {
					throw new SQLException("Inserted link is not in database: " + link);
				}
				link.setId(insertedLink.getId());
			} else {
				final PreparedStatement update = this.db.prepareStatement(UPDATE_LINK);
				update.setString(1, link.getUrl());
				update.setBoolean(2, link.isDone());
				update.setInt(3, link.getErrors());
				update.setLong(4, link.getId().longValue());
				update.executeUpdate();
				update.close();
			}
		} catch (final SQLException e) {
			throw new RuntimeException("Failed to save link " + link, e);
		}
	}
}
