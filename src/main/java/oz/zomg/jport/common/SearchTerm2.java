package oz.zomg.jport.common;

import java.util.Arrays;

//  see ENHANCE

/**
 * Encapsulates multiple, partial string seeking in a target String.
 *
 * @param <T> is the type implementing the .toString() method to search against.
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 * @see SearchTerm2#doesMatch
 */
public class SearchTerm2<T> {
    /**
     * Use '+' to match against all words.  Could also use radio buttons like (+)All (_)Any and pass in -> final boolean isAllTerms
     */
    static final private String _ALL_STRING = "+";

    static // initializer block
    {
    }

    final private boolean fIgnoreCase;
    final private boolean fNeedSearchAll;
    final private String[] fSearchStrings;

    private int mHitCount = 0;

    /**
     * Default constructor is always 'false' with .doesMatch().
     */
    public SearchTerm2() {
        this("", true);
    }

    /**
     * Constructor.
     *
     * @param searchTermsString multiple terms and may include one or more '+' for signifying ALL as opposed to ANY.  Can be 'null'
     * @param ignoreCase        when set 'true'
     */
    public SearchTerm2(final String searchTermsString, final boolean ignoreCase) {
        fIgnoreCase = ignoreCase;

        final String search = (searchTermsString != null) ? searchTermsString.trim() : "";
        fNeedSearchAll = search.contains(_ALL_STRING); // one or more '+' present

        fSearchStrings = (!search.isEmpty())
                ? StringsUtil_.fastSplits(search, ' ', '+')
                : StringsUtil_.NO_STRINGS; // an empty String[]{} here makes .isEmptyTerm()==true

        if (ignoreCase) {
            int i = 0;
            for (final String searchette : fSearchStrings) {   // make search case insensitive
                fSearchStrings[i++] = searchette.toLowerCase();
            }
        }
    }

    public boolean isEmptyTerm() {
        return fSearchStrings.length == 0;
    }

    /**
     * Does not increment hit count when matching so that rendering a JTree, for example, does not change the total count.
     *
     * @param obj with a sensible .toString() method for content to search
     * @return Ex. true when constructed with "hover + craft" and obj="My Hovercraft Has Eels"
     */
    public boolean doesMatch(final T obj) {
        final String content = (fIgnoreCase) // obj's String
                ? obj.toString().toLowerCase() // makes search case insensitive
                : obj.toString();

        if (content.isEmpty()) return isEmptyTerm(); //ENHANCE after researching -> "".contains( "" )

        final boolean match = StringsUtil_.contains
                (content
                        , fNeedSearchAll
                        , fSearchStrings
                );
        return match;
    }

    public boolean doesMatch(final T[] objs) //ENHANCE to handle array transforms
    {
        final StringBuilder sb = new StringBuilder();
        for (final T obj : objs) {
            final String string = (fIgnoreCase) // obj's String
                    ? obj.toString().toLowerCase() // makes search case insensitive
                    : obj.toString();

            sb.append(string).append(' ');
        }

        if (sb.length() == objs.length) return isEmptyTerm(); // just appended spaces

        final String content = sb.toString();
        final boolean match = StringsUtil_.contains
                (content
                        , fNeedSearchAll
                        , fSearchStrings
                );
        return match;
    }

    /**
     * Call when .doesMatch() returns true in your search Visitor.
     * Is re-entrant for multi-thread by returning an 'int' of the accumulating hits.
     * To reset the counter, create a fresh instance of SearchTerm.
     */
    public void incrementHitCount() {
        synchronized (this) {
            mHitCount += 1;
        } // was java.util.concurrent.atomic.AtomicInteger
    }

    public int getHitCount() {
        return mHitCount;
    }

    /**
     * @return 'true' if .incrementHitCount() was called indicating full / partial matches
     */
    public boolean hasAnyHits() {
        return mHitCount > 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj instanceof SearchTerm2<?> other) {
            return this.fIgnoreCase == other.fIgnoreCase
                    && this.fNeedSearchAll == other.fNeedSearchAll
                    && Arrays.deepEquals(this.fSearchStrings, other.fSearchStrings);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.fIgnoreCase ? 1 : 0);
        hash = 59 * hash + (this.fNeedSearchAll ? 1 : 0);
        hash = 59 * hash + Arrays.deepHashCode(this.fSearchStrings);
        return hash;
    }
}
