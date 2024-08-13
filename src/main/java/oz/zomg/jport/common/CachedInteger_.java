package oz.zomg.jport.common;

/**
 * This class looks stupid but Integer.valueOf() only caches 256 numbers
 * and the profiler indicates much time is being spent making new Integer wrappers.
 * For all possible ints with best performance, could be a Map custom coded to accept
 * the 'int' primitive as the key and SoftReference the Integer value.
 * <H3><I><FONT color="#770000">Subset of original source.</FONT></I></H3>
 * <p>
 * Note: can not extend Integer wrapper as it is 'final'.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class CachedInteger_ {
    static final public Integer ZERO = Integer.valueOf(0); // someday the JVM might do this for us?
    static final public Integer ONE = Integer.valueOf(1);

    static final private int _MIN_CACHED = -1;
    static final private int _MAX_CACHED = 32768;

    /**
     * There simply is not enough time to perform a hash.get() so an array is employed.
     */
    static final private Integer[] _INTEGERS = new Integer[1 + _MAX_CACHED - _MIN_CACHED];

    static // initializer block
    {
    }

    private CachedInteger_() {
    }

    static public Integer valueOf(final int i) {
        if (i == 0) return ZERO;

        if (i < _MIN_CACHED || i > _MAX_CACHED) {   // cache miss but perhaps some day zero-cost from JVM
            return Integer.valueOf(i); // access the negative number cache of Integer
        }

        Integer integer = _INTEGERS[i - _MIN_CACHED];
        if (integer == null) {   // lazy cache hit
            integer = Integer.valueOf(i);
            _INTEGERS[i - _MIN_CACHED] = integer;
        }
        return integer;
    }

    /**
     * Copy out a contiguous range.
     *
     * @param from
     * @param to
     * @return will be in reverse order if from > to
     */
    static public Integer[] slice(final int from, final int to) {
        final int begin = Math.min(from, to);
        final int end = Math.max(from, to);
        final int size = 1 + end - begin;
        final Integer[] integers = new Integer[size];

        // Can not use System.arraycopy() even if in range of begin >= _MIN_CACHED && end <= _MAX_CACHED
        // because _INTEGERS may contain 'nulls'.
        for (int j = begin, i = 0; i < size; i++, j++) {
            integers[i] = CachedInteger_.valueOf(j);
        }

        if (to < from) {   // descending
            Util.reverseOrderInPlace(integers);
        }

        return integers;
    }
}
