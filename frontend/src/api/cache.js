// Simple in-memory cache with TTL + in-flight request de-duplication, so
// remounting a component (e.g. navigating away from the dashboard and back)
// within the TTL window reuses the last result instead of re-fetching/
// re-rendering everything from scratch. Cache is a plain module-level Map,
// so it lives for the lifetime of the page (cleared on a full reload).
const store = new Map();

export function cachedFetch(key, fetchFn, ttlMs = 5 * 60 * 1000) {
  const entry = store.get(key);
  const now = Date.now();
  if (entry && now < entry.expiresAt) {
    return entry.promise;
  }
  const promise = fetchFn().catch((err) => {
    store.delete(key); // don't cache failures
    throw err;
  });
  store.set(key, { promise, expiresAt: now + ttlMs });
  return promise;
}

export function invalidateCache(key) {
  if (key) {
    store.delete(key);
  } else {
    store.clear();
  }
}
