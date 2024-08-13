package oz.zomg.jport.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;


/**
 * Uniform Resource Identifier content holder.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class UriContent {
    final public URI fUri;
    final public byte[] fContentBytes; // 'null' if 404
    final public boolean fIs404; //... general 'wget' error code
    final public int fFetchDeltaMillisec;
    //... HeaderFields map and MIME types

    /**
     * HTTP Error 404, content not found @ uri
     *
     * @param uri
     */
    UriContent(final URI uri) {
        this(uri, null, true, 0);
    }

    UriContent(final URI uri, final byte[] contentBytes) {
        this(uri, contentBytes, false, 0);
    }

    UriContent(final URI uri, final byte[] contentBytes, final int fetchDeltaMillisec) {
        this(uri, contentBytes, false, fetchDeltaMillisec);
    }

    private UriContent
            (final URI uri
                    , final byte[] contentBytes
                    , final boolean is404
                    , final int deltaMillisec
            ) {
        fUri = uri;
        fContentBytes = contentBytes;
        fIs404 = is404;
        fFetchDeltaMillisec = deltaMillisec;
    }

    /**
     * Get the content of an URI as bytes during this Thread without caching.
     *
     * @param timeoutMillisec
     * @param uri
     * @return the content bytes @ URI if not 404
     * @throws IOException if FileNotFoundException or server reset connection
     */
    static UriContent create(final URI uri, final int timeoutMillisec) throws IOException {
        final URL youAreEl = uri.toURL(); // *BLOCKS* for DNS resolution

        final long startMillisec = System.currentTimeMillis();

        final URLConnection connection = youAreEl.openConnection();
        connection.setConnectTimeout(timeoutMillisec); // web servers get busy too

        // required to avoid err 403
        connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
        connection.connect();

        final int size = connection.getContentLength(); // *BLOCKS* connects to remote sever
        if (size > 0) {
            final InputStream is = connection.getInputStream();
            final DataInputStream dis = new DataInputStream(is);

            if (false) //... lame, need to recognize encoding and MIME types and add to URIContent
            {
                System.out.println("Header @ " + uri.toString());
                final Map<String, List<String>> map = connection.getHeaderFields();
                for (final Map.Entry<String, List<String>> entry : map.entrySet()) {
                    System.out.println("field=" + entry.getKey());
                    for (final String string : entry.getValue()) {
                        System.out.println("    ->" + string);
                    }
                }
            }

            final byte[] bytes = new byte[size];
            dis.readFully(bytes); // *BLOCKS* to GET from remote server

            dis.close();
            is.close();

            final int deltaMillisec = (int) (System.currentTimeMillis() - startMillisec);
            return new UriContent(uri, bytes, deltaMillisec);
        } else {
            return new UriContent(uri); // nothing to read
        }
    }
}
