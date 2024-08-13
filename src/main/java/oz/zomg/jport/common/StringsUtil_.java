package oz.zomg.jport.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;


/**
 * Utilities for Strings.
 * <H3><I><FONT color="#770000">Subset of original source.</FONT></I></H3>
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class StringsUtil_ {
    static final public String[] NO_STRINGS = new String[0];

    private StringsUtil_() {
    }

    /**
     * Avoids String.split RegEx compiler overhead with a single char delimiter.
     * Non-regex.
     *
     * @param input          needs splitting
     * @param delimitingChar Non-RegEx char, ex. '/'
     * @return tokens in an array
     */
    static public String[] fastSplits(final String input, final char delimitingChar) {
        return fastSplits(input, String.valueOf(delimitingChar));
    }

    /**
     * Avoids String.split RegEx compiler overhead with a simple array of unique char delimiters.
     *
     * @param input           needs splitting
     * @param delimitingChars Non-RegEx one or more char cases ex. {',','\n'} or {'/'}
     * @return tokens an an array
     * @link "http://www.javamex.com/tutorials/regular_expressions/splitting_tokenisation_performance.shtml"
     * Non-regex.
     */
    static public String[] fastSplits(final String input, final char... delimitingChars) {
        final String delimitingCharsString = new String(delimitingChars); // String.valueOf(char[]) still copies the array
        return fastSplits(input, delimitingCharsString);
    }

    /**
     * Defensively encapsulated as 'private' so as to block misuse of the API.
     * Pass single char or char[]{...} to above methods -OR- use String.split() regex instead.
     *
     * @param input
     * @param delimitingCharsString
     * @return
     */
    static private String[] fastSplits(final String input, final String delimitingCharsString) {
        final StringTokenizer tokenizer = new StringTokenizer(input, delimitingCharsString, false); // outperforms String.split() by 50% even with static Pattern.compile("[/]")
        final int tokenCount = tokenizer.countTokens(); // path = SLASH_PATTERN.split( name, 0 ); // 0=no limit
        final String[] splits = new String[tokenCount]; // nulls
        for (int i = 0; i < tokenCount; i++) {
            splits[i] = tokenizer.nextToken();
        }
        return splits;
    }

    /**
     * @param epochDateTimeMillisec
     * @return date only in local TZ, ex. "2012-08-01"
     */
    @SuppressWarnings("deprecation")
    static public String getDateString(final long epochDateTimeMillisec) {
        if (epochDateTimeMillisec == 0L) {
            return "Unknown";
        }

        final java.util.Date jud = new java.util.Date(epochDateTimeMillisec);
        final int yer = jud.getYear() + 1900;
        final int mon = jud.getMonth() + 1; // required as 0=jan
        final int day = jud.getDate();

        String zmon = ((mon < 10) ? "0" : "") + mon; // prefix zero
        String zday = ((day < 10) ? "0" : "") + day; // prefix zero

        return yer + "-" + zmon + "-" + zday;
    }

    /**
     * Not called .doesEqual() because declared like String.equals().
     * Consider using a HashSet.contains() if a constant [] and is checked often (saw ~10x speed with 100 Strings).
     *
     * @param examine           this String, not 'null'
     * @param anyOfTheseStrings look for at least one exact match.  Could be more efficient with .bsearch() but var-args should be very short.
     * @return
     */
    static public boolean equals(final String examine, final String... anyOfTheseStrings) {
        return switch (anyOfTheseStrings.length) {
            case 0 -> false;
            case 1 -> examine.equals(anyOfTheseStrings[0]);
            default ->
                // linear search faster than hashing until more than ~20 non-.intern() elements
                    Arrays.asList(anyOfTheseStrings).contains(examine);
        };
    }

    /**
     * Not called .doesContain() because declared like String.contains().
     *
     * @param examine                   this String, not 'null'
     * @param needContainsAllSubStrings when 'true' the string to examine must contain ALL substrings, when 'false' ANY single substring will predicate
     * @param subStrings                subordinate strings to be found in the string being Examined.  Elements that are 'null' or {""} are ignored
     * @return 'true' if String contains any/all of the substrings
     */
    static public boolean contains
    (final String examine
            , final boolean needContainsAllSubStrings
            , final String... subStrings
    ) {
        for (final String sub : subStrings) {
            if (sub != null && !sub.isEmpty()) {   // elements that are 'null' or {""} are ignored
                if (examine.contains(sub)) {
                    if (!needContainsAllSubStrings) return true;
                } else if (needContainsAllSubStrings) {
                    return false; // one failed
                }
            }
        }

        // looks insane but is actually correct as could not have gotten here when needAll=true and any substring failed to match
        return needContainsAllSubStrings;
    }

    //BUG FIX

    /**
     * Concatenate Strings with separators between.
     *
     * @param separator       "" is acceptable
     * @param iterableStrings
     * @return has no separator at the very end as one would expect
     */
    static public String concatenate(final String separator, final Iterable<String> iterableStrings) {
        final StringBuilder sb = new StringBuilder();
        for (final String string : iterableStrings) {
            sb.append(string);
            sb.append(separator);
        }

        if (sb.isEmpty()) return ""; //BUG FIX when Iterable is empty

        sb.setLength(sb.length() - separator.length()); // remove last separator
        return sb.toString();
    }

    static public String concatenate(final String separator, final String... strings) {
        return concatenate(separator, Arrays.asList(strings)); // fast wrapper
    }

    //ENHANCE -> StringsUtil
    static public String[] toStrings(final Collection<String> stringCollection) {
        switch (stringCollection.size()) {
            case 0 -> {
                return NO_STRINGS;
            }
            case 1 -> {
                return new String[]{stringCollection.iterator().next()};
            }
            default -> {
                return stringCollection.toArray(new String[stringCollection.size()]);
            }
        }
    }

    //ENHANCE -> StringsUtil

    /**
     * Non-deep scan, just flat transform to new String[].
     *
     * @param objs can contain 'null' elements
     * @return 'null' elements are assigned ""
     */
    static public String[] toStrings(final Object[] objs) {
        switch (objs.length) {
            case 0:
                return NO_STRINGS;
            case 1:
                return new String[]{objs[0].toString()};
            default: {
                final String[] strings = new String[objs.length];
                int i = 0;
                for (final Object obj : objs) {
                    strings[i] = (obj != null) ? obj.toString() : "";
                    i++;
                }
                return strings;
            }
        }
    }

    //ENHANCE StringUtils?

    /**
     * @param isCaseInsensitive
     * @param strings
     * @return in-place sorted
     */
    static public String[] sort(final boolean isCaseInsensitive, final String... strings) {
        switch (strings.length) {
            case 0 -> {
                return NO_STRINGS;
            }
            case 1 -> {
                return strings;
            }
            default -> {
                if (!isCaseInsensitive) {
                    Arrays.sort(strings);
                } else {
                    Arrays.sort(strings, String.CASE_INSENSITIVE_ORDER);
                }
                return strings;
            }
        }
    }

    //ENHANCE StringUtils?

    /**
     * Columnizes an array via .toString().
     *
     * @param columnCount
     * @param prepend     can be ""
     * @param append      can be ""
     * @param objs
     * @return
     */
    static public String htmlTabularize
    (final int columnCount
            , final String prepend
            , final String append
            , final Object[] objs
    ) {
        return htmlTabularize(columnCount, prepend, append, Arrays.asList(objs)); // fast wrapper
    }

    //ENHANCE StringUtils?

    /**
     * Columnizes a linear list via .toString().
     *
     * @param columnCount
     * @param prepend     to table data, can be ""
     * @param append      to table data, can be ""
     * @param collection
     * @return starts with <CODE> [HTML] </CODE> and ends with <CODE> [/TABLE] </CODE>
     */
    static public String htmlTabularize
    (final int columnCount
            , final String prepend
            , final String append
            , final Collection<?> collection
    ) {
        if (collection.isEmpty()) return "";

        final StringBuilder sb = new StringBuilder("<HTML><TABLE><TR>");

        int i = 1; // don't start with a <BR>
        for (final Object obj : collection) {
            sb.append("<TD>").append(prepend).append(obj.toString()).append(append).append("</TD>");
            sb.append(((i % columnCount) == 0) ? "</TR><BR>" : "");
            i++;
        }

        sb.append("</TR></TABLE>");
        return sb.toString();
    }

    /**
     * @param hasBorder
     * @param string    with columns tab delimited and rows NEWLINE separated
     * @return
     */
    static public String toHtmlTable(final boolean hasBorder, final String string) {
        final String[] lines = fastSplits(string, '\n'); // literal newline
        final String[][] str2d = new String[lines.length][];
        int i = 0;
        for (final String line : lines) {
            str2d[i++] = fastSplits(line, '\t'); // literal tab
        }

        return toHtmlTable(hasBorder, str2d);
    }

    /**
     * JLabel will suffice with HTML tables, not required to use JTextPane
     *
     * @param hasBorder
     * @param strings2d java 2D arrays are sparsely allocated
     * @return
     */
    static public String toHtmlTable(final boolean hasBorder, final String[][] strings2d) {
        final StringBuilder sb = new StringBuilder("<HTML>");

        sb.append((hasBorder) ? "<TABLE border=1>" : "<TABLE>");

        Arrays.stream(strings2d).forEach(strings -> {
            sb.append("<TR>"); // table row
            if (strings.length == 0) {
                sb.append("<HR>");
            }
            if (strings.length == 1) {
                sb.append("<CAPTION>" + "<B>"); // Caption works to center across all TD
                sb.append(strings[0]);
                sb.append("</B>" + "</CAPTION>"); // HR does not work here
            } else {
                for (final String str : strings) {
                    sb.append("<TD>"); // table data
                    sb.append(str); // can be 'null'
                    sb.append("</TD>");
                }
            }
            sb.append("</TR>"); // BR assumed
        });

        sb.append("</TABLE>");
        return sb.toString();
    }
}
