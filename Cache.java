package com.sidewaysmilk;

import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;

/**
 * Put and get objects in a cache.
 * 
 * Runs a thread which prunes expired entries periodically to prevent memory
 * bloat.
 * 
 * Objects are added to the cache with a key and a value. Objects put into the
 * cache can be retrieved until they expire.
 * 
 * @author Justin Force - justin.force@gmail.com
 * 
 * @param <K>
 *            The Java class of the key
 * @param <V>
 *            The Java class of the values
 */
public class Cache<K, V> {

	/**
	 * The "time to live" of the cached object. After ttl milliseconds have
	 * passed, the object is to be expired.
	 */
	private final long ttl;

	/**
	 * The Java Hashtable which we will use to store our objects internally.
	 */
	private Hashtable<K, CacheValue<V>> table;

	/**
	 * The thread which prunes expired entries
	 */
	private Thread thread;

	/**
	 * Constructs a new, empty cache with "time to live" set to ttl
	 * milliseconds. Objects put into the cache will be expired after ttl has
	 * elapsed.
	 * 
	 * @param ttl
	 *            "time to live" in milliseconds
	 */
	public Cache(final long ttl) {

		this.ttl = ttl;

		/*
		 * Set up new Hashtable to hold all of our cache key/value pairs
		 */
		table = new Hashtable<K, CacheValue<V>>();

		/*
		 * Set up the thread and just run prune then sleep ttl milliseconds
		 * forever.
		 */
		thread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					prune();
					try {
						Thread.sleep(ttl);
					} catch (InterruptedException e) {
						// We really don't mind being interrupted. :)
					}
				}
			}
		});
		thread.start();
	}

	/**
	 * Put value into cache with key as an index.
	 * 
	 * @param key
	 *            the key for the value
	 * @param value
	 *            the value
	 */
	public void put(K key, V value) {

		/*
		 * Wrap value in a new CacheValue, setting the ttl to now + ttl
		 * milliseconds, then add it to the hashtable.
		 */
		table.put(key, new CacheValue<V>(new GregorianCalendar()
				.getTimeInMillis()
				+ ttl, value));
	}

	/**
	 * Get value corresponding to key from cache
	 * 
	 * @param key
	 *            the key corresponding to value
	 * @return the value
	 * @throws CacheKeyNotFoundException
	 *             if the value corresponding to key is not present, which can
	 *             happen if it has expired or never existed.
	 */
	public V get(K key) throws CacheKeyNotFoundException {
		CacheValue<V> o = table.get(key);
		if (o == null || o.expired()) {
			throw new CacheKeyNotFoundException();
		} else {
			return o.get();
		}
	}

	/**
	 * Remove value corresponding to key from cache.
	 * 
	 * @param key
	 *            the key corresponding to the value
	 * @return the value that is removed
	 */
	public V remove(K key) {
		return table.remove(key).get();
	}

	/**
	 * Return the thread which is managing the pruning operations
	 * 
	 * @return the thread
	 */
	public Thread getThread() {
		return thread;
	}

	/**
	 * Remove expired entries from cache. Meant to be done periodically, as in a
	 * thread, but can also be called manually if needed.
	 */
	public void prune() {
		Enumeration<K> keys = table.keys();
		while (keys.hasMoreElements()) {
			K key = keys.nextElement();
			if (table.get(key).expired()) {
				remove(key);
			}
		}
	}

	/**
	 * Wrapper for the cached value with its ttl
	 * 
	 * @author Justin Force - justin.force@gmail.com
	 * 
	 * @param <C>
	 *            the Java class of the value to be stored
	 */
	private class CacheValue<C> {

		/**
		 * The value stored
		 */
		private C value;

		/**
		 * The expiration date in milliseconds
		 */
		private long expiry;

		/**
		 * Construct new CacheValue with value and expiration date
		 * 
		 * @param expiry
		 *            the expiration date
		 * @param value
		 *            the value
		 */
		private CacheValue(long expiry, C value) {
			this.expiry = expiry;
			this.value = value;
		}

		/**
		 * Check whether this value is expired
		 * 
		 * @return true if this value is expired, false if it is not
		 */
		private boolean expired() {
			return (new GregorianCalendar().getTimeInMillis() > expiry);
		}

		/**
		 * Get the value
		 * 
		 * @return the value
		 */
		private C get() {
			return value;
		}
	}
}
