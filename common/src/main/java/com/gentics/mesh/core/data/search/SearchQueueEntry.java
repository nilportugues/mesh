package com.gentics.mesh.core.data.search;

import java.util.Map;

import rx.Completable;

/**
 * A search queue entry is contains the information that is needed to update the search index for the element that is specified in this entry. In order to
 * update the search index various information are needed.
 * 
 * This includes:
 * <ul>
 * <li>Element UUUID - dd5e85cebb7311e49640316caf57479f</li>
 * <li>Element Type - node, tag, role</li>
 * <li>Element Action - delete, update, create</li>
 * <li>Element Index Type - en, de (some search indices have different types for each document. We use types to separate language variations of nodes)</li>
 * </ul>
 *
 */
public interface SearchQueueEntry {

	/**
	 * Return the search queue entry element uuid which identifies the element that should be handled.
	 * 
	 * @return
	 */
	String getElementUuid();

	SearchQueueEntry setElementUuid(String uuid);

	/**
	 * Return the search element type.
	 * 
	 * @return
	 */
	String getElementType();

	/**
	 * Set the search element type (node, tag, role..)
	 * 
	 * @param type
	 * @return
	 */
	SearchQueueEntry setElementType(String type);

	/**
	 * Return the search queue entry action (eg. Update, delete..)
	 * 
	 * @return
	 */
	SearchQueueEntryAction getElementAction();

	/**
	 * Set the entry action (eg. update, delete, create)
	 * 
	 * @param action
	 * @return
	 */
	SearchQueueEntry setElementAction(SearchQueueEntryAction action);

	/**
	 * Process the entry.
	 * 
	 * @return
	 */
	Completable process();

	/**
	 * Get property with given key
	 * 
	 * @param key
	 *            property key
	 * @return property
	 */
	<T> T get(final String key);

	/**
	 * Set the property with given key
	 * 
	 * @param key
	 *            property key
	 * @param value
	 *            property value
	 * @return Fluent API
	 */
	SearchQueueEntry set(final String key, final Object value);

	/**
	 * Set custom properties.
	 * 
	 * @param properties
	 * @return Fluent API
	 */
	SearchQueueEntry set(Map<String, Object> properties);

	/**
	 * Set the timestamp for the entry.
	 * 
	 * @param currentTimeMillis
	 * @return
	 */
	SearchQueueEntry setTime(long currentTimeMillis);

	/**
	 * Return the timestamp on which the entry was created.
	 * 
	 * @return
	 */
	long getTime();

}
