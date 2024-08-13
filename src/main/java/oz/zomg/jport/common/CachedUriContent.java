package oz.zomg.jport.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Name space class.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class CachedUriContent {
    /**
     * Singleton-esque as not using a real Service Registry.
     */
    static volatile private UriContentCacheable sUriCacheable = null;

    /**
     * @return no caching in case feature is dropped.
     */
    static UriContentCacheable getInstance() {
        return (sUriCacheable = new NoCache());
    }

    /**
     * Static factory method.
     *
     * @param isNonVolatile 'true' for on disk persistence
     * @return
     */
    static UriContentCacheable getInstance(final boolean isNonVolatile) {
        if (sUriCacheable == null) {
            sUriCacheable = (isNonVolatile)
                    ? new NonVolatileCache()
                    : new VolatileCache();
        }

        return sUriCacheable;
    }

    /**
     * @return non-404 and non-GC'd contents of the cache
     */
    static public UriContent[] dumpContent() {
        return (sUriCacheable != null)
                ? sUriCacheable.dump(true)
                : new UriContent[0];
    }

    static private UriContent[] _toArray(final boolean isContentOnly, final Collection<UriContent> collection) {
        final List<UriContent> list = new ArrayList<UriContent>(collection.size());
        for (final UriContent uc : collection) {
            if (!isContentOnly || (uc.fContentBytes != null && uc.fContentBytes.length > 0)) {
                list.add(uc);
            }
        }

        return Util.createArray(UriContent.class, list);
    }

    static {
    }

    private CachedUriContent() {
    }


    // ================================================================================

    /**
     * Similar in spirit to <CODE>Map<K,V></CODE>
     */
    interface UriContentCacheable {
        void clearAll();

        /**
         * @param uri
         * @return 'true' if cached even if 404
         */
        boolean has(URI uri);

        /**
         * @param uri
         * @return content bytes @ URI else 'null' if not available
         */
        UriContent get(URI uri);

        /**
         * @param uriContent to put into the cache, can be 'null' for 404
         */
        void put(UriContent uriContent);

        /**
         * @param isContentOnly 'true' excludes 404 URL Not Found
         * @return current cache
         */
        UriContent[] dump(final boolean isContentOnly);
    }


    // ================================================================================

    /**
     * Do not use any caching
     */
    static private class NoCache
            implements UriContentCacheable {
        @Override
        public void clearAll() {
        }

        @Override
        public boolean has(URI uri) {
            return false;
        }

        @Override
        public UriContent get(URI uri) {
            return null;
        }

        @Override
        public void put(UriContent uriContent) {
        }

        @Override
        public UriContent[] dump(final boolean isContentOnly) {
            return new UriContent[0];
        }
    }


    // ================================================================================

    /**
     * In memory URI cache.  Provides run-time recall.
     */
    static private class VolatileCache
            implements UriContentCacheable {
        /**
         * Note: ConcurrentHashMap does not permit 'null' values, HashMap does.
         */
        final private Map<URI, UriContent> fUriToContentMap = new HashMap<URI, UriContent>();

        @Override
        synchronized public void clearAll() {
            fUriToContentMap.clear();
        }

        @Override
        synchronized public boolean has(final URI uri) {
            return fUriToContentMap.containsKey(uri);
        }

        @Override
        synchronized public UriContent get(final URI uri) {
            return fUriToContentMap.get(uri);
        }

        @Override
        synchronized public void put(final UriContent uriContent) {
            fUriToContentMap.put(uriContent.fUri, uriContent);
        }

        @Override
        synchronized public UriContent[] dump(final boolean isContentOnly) {
            return _toArray(isContentOnly, fUriToContentMap.values());
        }
    }


    // ================================================================================

    /**
     * Reduces web server bandwidth usage by using home user's disk to persist from memory.
     * Retains in HashMap softly.  When out of memory and content is GC'd, will restore from
     * local media.
     * <p>
     * A Soft Reference is not disposed until it is the only referent to an Object and the JVM has
     * consumed the maximum heap memory (or in some JVMs simply that a major GC was performed).
     */
    static private class NonVolatileCache
            implements UriContentCacheable {
        static final private Thread_Worker _FILE_WORKER_THREAD = new Thread_Worker(NonVolatileCache.class.getSimpleName());

        static {
            _FILE_WORKER_THREAD.start();
        }

        final private Map<URI, Reference<UriContent>> fUriToContentRefMap = new HashMap<URI, Reference<UriContent>>();
        final private boolean fIsNonVolatileAvailable;
        final private File fCacheDirPath;

        /**
         * Default hidden directory.
         */
        NonVolatileCache() {
            this(System.getProperty("user.home") + File.separatorChar + ".zomg" + File.separatorChar + "uri-cache");
        }

        /**
         * @param cacheDirectory probably ~/.application-name/uri-cache/ but can be 'null' for none
         */
        private NonVolatileCache(final String cacheDirectory) {
            boolean isNonVolatileAvailable = cacheDirectory != null;

            fCacheDirPath = new File(cacheDirectory);
            if (isNonVolatileAvailable && !fCacheDirPath.exists()) {   // create the folder if possible
                isNonVolatileAvailable = fCacheDirPath.mkdirs();
            }

            // had write permissions
            fIsNonVolatileAvailable = isNonVolatileAvailable;
        }

        /**
         * @param uri
         * @return compatible with Unix/Windows file names
         */
        static private String _sanitizeUri(final URI uri) {
            return uri.toString().replace('/', '.').replace(':', '-'); // File.separatorChar
        }

        @Override
        synchronized public void clearAll() {
            if (fIsNonVolatileAvailable) {
                Arrays.stream(fCacheDirPath.listFiles()).forEach(File::delete);
            }

            fUriToContentRefMap.clear();
        }

        @Override
        synchronized public boolean has(final URI uri) {
            return fUriToContentRefMap.containsKey(uri) || this.get(uri) != null; // 1st run needs to check on disk also
        }

        @Override
        synchronized public UriContent get(final URI uri) {
            if (fIsNonVolatileAvailable && (!fUriToContentRefMap.containsKey(uri) || fUriToContentRefMap.get(uri).get() == null)) {   // check local disk
                final File filepath = new File(fCacheDirPath, _sanitizeUri(uri));
                if (filepath.exists()) {   // read contents
                    try {
                        byte[] contentBytes = Util.retrieveFileBytes(filepath);

                        final UriContent uriContent = (contentBytes.length != 0)
                                ? new UriContent(uri, contentBytes)
                                : new UriContent(uri); // 404

                        final Reference<UriContent> uriContentRef = new SoftReference<>(uriContent);
                        fUriToContentRefMap.put(uri, uriContentRef);
                    } catch (IOException ignored) {
                    }
                }
            }

            final Reference<UriContent> uriContentRef = fUriToContentRefMap.get(uri);
            return (uriContentRef != null)
                    ? uriContentRef.get() // make available as hard ref
                    : null; // not present
        }

        /**
         * One could make sure the cache file will be completely written to disk by
         * calling through SwingUtilities.invokeLater().
         * Rather perverse as the UI thread is the only one guaranteed to keep the
         * JVM live without having to build a real ServiceProvider architecture.
         *
         * @param uriContent
         */
        @Override
        synchronized public void put(final UriContent uriContent) {
            final URI uri = uriContent.fUri; // alias

            if (!fUriToContentRefMap.containsKey(uri)) {   // new content
                final Reference<UriContent> uriContentRef = new SoftReference<>(uriContent);
                fUriToContentRefMap.put(uri, uriContentRef);

                if (fIsNonVolatileAvailable) {
                    _FILE_WORKER_THREAD.offer(new Runnable() // anonymous class
                    {
                        @Override
                        public void run() // maintains a hard ref to uriContent
                        {
                            File filePath = new File(fCacheDirPath, _sanitizeUri(uri));

                            if (filePath.exists()) {   // tried again
                                if (!filePath.delete()) return;
                            }

                            FileOutputStream fileOutputStream = null;
                            try {
                                if (filePath.createNewFile()) {
                                    fileOutputStream = new FileOutputStream(filePath); // THROWS
                                    if (!uriContent.fIs404) {   // content was not 404
                                        fileOutputStream.write(uriContent.fContentBytes); // THROWS
                                    }
                                    // else just a zero length file
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            } finally {
                                Util.close(fileOutputStream);
                            }
                        }
                    });
                }
            }
        }

        @Override
        synchronized public UriContent[] dump(final boolean isContentOnly) {
            final List<UriContent> hardRefList =
                    fUriToContentRefMap
                            .values().stream()
                            .map(Reference::get)
                            .filter(Objects::nonNull)
                            .collect(
                                    Collectors.toCollection(() ->
                                            new ArrayList<>(fUriToContentRefMap.size())));
            // break out soft/weak refs
            // hold onto hard reference before GC'd

            return _toArray(isContentOnly, hardRefList);
        }
    }
}
