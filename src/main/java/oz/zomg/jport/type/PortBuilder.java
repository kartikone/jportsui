package oz.zomg.jport.type;

import oz.zomg.jport.common.StringsUtil_;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Mutable Port builder that parses its info from a line (or lines) in the "PortIndex" file.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
class PortBuilder {
    /**
     * Do not refactor these, they need to match the lower-cased literal text field names from the "PortIndex" file.
     */
    private enum EPortField {
        name, variants(true), subports(true), depends_build(true), depends_fetch(true), depends_lib(true), depends_extract(true), depends_run(true), description, long_description, homepage, platforms(true), maintainers(true), license(true), epoch, version, revision, categories(true), portdir, replaced_by, installs_libs;

        EPortField() {
            this(false);
        }

        EPortField(final boolean isArray) {
            this.isArray = isArray;
        }

        final private boolean isArray;
    }

    static final private List<String> _SPLIT_LIST = new ArrayList<>(64);

    /**
     * Avoids exception throw of Enum.parse(...), would return 'null' instead.
     */
    static final private Map<String, EPortField> _FIELD_TO_ENUM_MAP = new HashMap<>();

    static {
        for (final EPortField e : EPortField.values()) {
            _FIELD_TO_ENUM_MAP.put(e.name(), e);
        }
    }

    String name = "", description = "", long_description = "", homepage = "", epoch = "", version = "", revision = "", portdir = "", replaced_by = "", installs_libs = "";

    String[] variants = StringsUtil_.NO_STRINGS, subports = StringsUtil_.NO_STRINGS, depends_fetch = StringsUtil_.NO_STRINGS, depends_extract = StringsUtil_.NO_STRINGS, depends_lib = StringsUtil_.NO_STRINGS, depends_build = StringsUtil_.NO_STRINGS, depends_run = StringsUtil_.NO_STRINGS, platforms = StringsUtil_.NO_STRINGS, maintainers = StringsUtil_.NO_STRINGS, license = StringsUtil_.NO_STRINGS, categories = StringsUtil_.NO_STRINGS;

    /**
     * @param line from the "PortIndex" file.
     */
    PortBuilder(final String line) {
        String parse = line;

        while (!parse.isEmpty()) {
            while (parse.charAt(0) == 0x20) {   // trim beginning
                parse = parse.substring(1); // allocates a new String obj but -not- a new backing char[]
            }

            // get key
            final int k = parse.indexOf(0x20); // space
            if (k != -1) {   // found
                final String key = parse.substring(0, k);

                if (_FIELD_TO_ENUM_MAP.containsKey(key)) {
                    final EPortField e = _FIELD_TO_ENUM_MAP.get(key);
                    parse = parse.substring(k + 1); // skip space

                    String value = "";

                    if (parse.charAt(0) == '{') {   // multi-value
                        int parenCount = 1;

                        final int parseLength = parse.length();
                        for (int p = 1; p < parseLength; p++) {
                            switch (parse.charAt(p)) {
                                case '{':
                                    parenCount++;
                                    break;
                                case '}':
                                    parenCount--;
                                    break;
                            }

                            if (parenCount == 0 || p == parseLength - 1) {
                                value = parse.substring(0, p);

                                parse = (p != parseLength - 1)
                                        ? parse.substring(p + 1) // skip space
                                        : ""; // done, actually happens

                                break; // for
                            }
                        }
                    } else {   // single value
                        final int p = parse.indexOf(0x20); // space
                        if (p != -1) {
                            value = parse.substring(0, p);
                            parse = parse.substring(p + 1); // skip space
                        } else {    // no more spaces, happens with 'revision'
                            value = parse;
                            parse = "";
                        }
                    }

                    setField(e, value);
                } else {   // no matching key
                    break; // while
                }
            }
            // else not expected
        }
    }

    private void setField(final EPortField e, final String rawValue) {
        if (rawValue.isEmpty()) return;

        final String value;
        final String[] values;

        if (rawValue.charAt(0) == '{') {   // multi value
            int p = 0; // begin
            while (p < rawValue.length() && rawValue.charAt(p) == '{') {   // find first non-'{'
                p++;
            }

            int q = rawValue.length() - 1; // end
            while (q > p && rawValue.charAt(q) == '}') {   // find last non-'}'
                q--;
            }

            if (e.isArray) {
                value = null;
                values = _splitOnSpace(rawValue.substring(p, q + 1));
            } else {   // do not .intern() these, all different
                value = rawValue.substring(p, q + 1); // source is a larger backing .substring() that needs to be GC'd
                values = null;
            }
        } else {   // single value may be .intern() later
            if (e.isArray) {
                value = null;
                values = new String[]{_extractAfterColon(rawValue).intern()}; // single element wrapper
            } else {   // break out a sub-string with 'new' for .intern()
                value = rawValue; // source is a larger backing .substring() that needs to be GC'd
                values = null;
            }
        }

//if( value != null && ( value.endsWith( " " ) || value.indexOf( '\t' ) != -1 ) ) System.out.println( "Single="+ value +'<' );
//if( values != null )
//{
//    for( String s : values )
//    {
//        if( s.endsWith( " " )  || s.indexOf( '\t' ) != -1 ) System.out.println( "Multi="+  s +'<' );
//    }
//}

        switch (e) {   // single
            case name:
                this.name = value;
                break;
            case description:
                this.description = value;
                break;
            case long_description:
                this.long_description = value;
                break;
            case homepage:
                this.homepage = (value.startsWith("ftp://ftp.")) ? value.replace("ftp://ftp.", "http://www.") : value;
                break; // special cased
            case epoch:
                this.epoch = value;
                break;
            case version:
                this.version = value;
                break;
            case revision:
                this.revision = value;
                break;
            case portdir:
                this.portdir = value;
                break;
            case replaced_by:
                this.replaced_by = value;
                break;
            case installs_libs:
                this.installs_libs = value;
                break;

            // multi
            case variants:
                this.variants = values;
                break;
            case subports:
                this.subports = values;
                break;
            case depends_fetch:
                this.depends_fetch = values;
                break;
            case depends_extract:
                this.depends_extract = values;
                break;
            case depends_lib:
                this.depends_lib = values;
                break;
            case depends_build:
                this.depends_build = values;
                break;
            case depends_run:
                this.depends_run = values;
                break;
            case platforms:
                this.platforms = values;
                break;
            case maintainers:
                this.maintainers = values;
                break;
            case license:
                this.license = values;
                break;
            case categories:
                this.categories = values;
                break;
        }
    }

    static private String _extractAfterColon(final String value) {
        final int colonPos = value.lastIndexOf(':');
        return (colonPos != -1)
                ? value.substring(colonPos + 1)
                : value;
    }

    /**
     * @param value
     * @return for use directly by class field
     */
    static private String[] _splitOnSpace(final String value) {
        final int stop = value.length() - 1;
        int p = 0;
        for (int q = 0; q < value.length(); q++) {
            if (value.charAt(q) == 0x20 || q == stop) {   // space or ending
                if (p != q) {   // but not a space run
                    final String spaceSplit = (q != stop)
                            ? value.substring(p, q) // normal
                            : value.substring(p); // special case lack of space char at end
                    final String colonSplit = _extractAfterColon(spaceSplit);
                    _SPLIT_LIST.add(colonSplit.intern());
                }

                p = q + 1;
            }
        }


        if (!_SPLIT_LIST.isEmpty()) {
            final String[] values = StringsUtil_.toStrings(_SPLIT_LIST);
            _SPLIT_LIST.clear(); // no leak and prepare for next time
            return values;
        } else {   // empty, nothing to clear
            return StringsUtil_.NO_STRINGS;
        }
    }

    boolean didParse() {
        return this.name != null && !this.name.isEmpty()
                && this.version != null && !this.version.isEmpty();
    }
}

// *** all on one line Example
// variants universal
// depends_build port:help2man
// portdir archivers/gnutar
// description {tar version of the GNU project}
// homepage http://www.gnu.org/software/tar/
// epoch 0
// platforms darwin
// name gnutar
// depends_lib {port:gettext port:libiconv}
// long_description {the gnutar program creates, adds files to, or extracts files from an archive file in gnutar format, called a tarfile. A tarfile is often a magnetic tape, but can be a floppy diskette or any regular disk file.}
// maintainers mww
// license GPL-3
// categories archivers
// version 1.26
// revision 0

// *** MacPort fields that have multiple terms or spaces in their String
// depends_fetch=1
// depends_extract=2
// homepage=15
// depends_run=267
// platforms=647
// depends_build=1091
// subports=1377
// variants=1723
// license=4112
// maintainers=5478
// categories=5525
// depends_lib=6803
// description=15331
// long_description=15348
