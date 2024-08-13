package oz.zomg.jport.ports;

import oz.zomg.jport.PortConstants;
import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.Util;
import oz.zomg.jport.type.CliPortInfo;
import oz.zomg.jport.type.EPortStatus;
import oz.zomg.jport.type.PortFactory;
import oz.zomg.jport.type.Portable;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Aggregates all Ports from the "PortIndex" file.  Later actual Port status is queried by the Port CLI tool.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class PortsCatalog {
    static final private boolean _IS_PREV_MAP_REUSED = false;

    /**
     * This file created by MacPorts.
     */
    static final private String _PORTS_FILE_NAME = "PortIndex";

    /**
     * As of 2012-09-01, ~15,500 port entries.
     */
    static final private int _FORECAST_COUNT = 20000;

    /**
     * Last Catalog generated Identification #
     */
    static final private AtomicInteger _ID = new AtomicInteger();

    /**
     * Initial, empty catalog.
     */
    static final public PortsCatalog NONE = new PortsCatalog();

    static {
    }

    /**
     * Time of "PortIndex" file parse.
     */
    final private long fParse_Epoch = System.currentTimeMillis();

    final private PortsDep fPortsDep = new PortsDep(this);
    final private PortsVariants fPortsVariants = new PortsVariants();

    /**
     * Key is the case-insensitive port name.
     */
    final private Map<String, Portable> fCiName_to_PortMap;
    final private PortsInventory fPortsInventory;

    /**
     * Lazily instantiated in a different I/O bound thread.
     */
    volatile private PortsDate vPortsDate = null;

    /**
     * For initial NONE catalog.
     */
    private PortsCatalog() {
        fCiName_to_PortMap = Collections.emptyMap();
        fPortsInventory = new PortsInventory();
    }

    /**
     * Reread and parse the "PortsIndex" file into a new Map.
     *
     * @param prevCatalog was Initial
     */
    public PortsCatalog(final PortsCatalog prevCatalog) {
        final Map<String, Portable> ciName_to_PortMap = (!_IS_PREV_MAP_REUSED)
                ? _parsePortIndex(PortConstants.PORTS_PATH + File.separator + _PORTS_FILE_NAME) // *BLOCKS* for disk I/O
                : prevCatalog.fCiName_to_PortMap;

        final Set<Portable> allPortSet = new HashSet<>(_FORECAST_COUNT);

        // interrogate the CLI for user's installed Ports status
        // *BLOCKS* for CLI
        final Map<EPortStatus, Set<CliPortInfo>> status_to_CpiSet_Map = TheApplication.INSTANCE.cliEchoAllPortStatus();

        final Map<CliPortInfo, Set<EPortStatus>> cpi_to_StatusSet_Map = CliPortInfo.createInverseMultiMapping(status_to_CpiSet_Map);

        // alias
        // alias
        cpi_to_StatusSet_Map.forEach((cpi, statusSet) -> {
            final String ciName = cpi.getCaseInsensitiveName();
            final Portable prevPort = ciName_to_PortMap.get(ciName);

            if (prevPort != null) {   // always expected to be found
                final Portable cliPort = PortFactory.createFromCli(prevPort, cpi);

                // assign each status found in the CLI
                statusSet.forEach(cliPort::setStatus);

                // MacPorts guarantees only one Active version per Port
                if (statusSet.contains(EPortStatus.ACTIVE)) {   // Replace existing index Port with the ACTIVE installed port info version.
                    // That ensures that any ci_named dep Ports refer here-in.
                    ciName_to_PortMap.put(ciName, cliPort);
                } else {   // copy in other Inactive Port versions and also modify Active status with Outdated, etc.
                    allPortSet.add(cliPort);
                }
            }
            // else port now deprecated?
        });

        // augment with installed ports the included MULTIPLE versions with differing variants
        allPortSet.addAll(ciName_to_PortMap.values());

        fPortsInventory = new PortsInventory(allPortSet);

        fCiName_to_PortMap = ciName_to_PortMap;

        _ID.incrementAndGet();
    }

    static private Map<String, Portable> _parsePortIndex(final String filePathName) {
        final Map<String, Portable> map = new HashMap<>(_FORECAST_COUNT);

        final File filePath = (PortConstants.HAS_MAC_PORTS)
                ? new File(filePathName)
                : new File(_PORTS_FILE_NAME); // fall back to project folder for dev work

        if (!filePath.exists()) {   // Port index file not found
            JOptionPane.showMessageDialog(null, filePathName + "\n does not seem to exist.");
        } else {   // found Port index
            if (PortConstants.DEBUG)
                System.out.println(PortsCatalog.class.getSimpleName() + " OPTIMIZATION=" + PortConstants.OPTIMIZATION);
            final long startMillisec = System.currentTimeMillis();

            if (PortConstants.OPTIMIZATION) {   // Scanner uses regex, this is 2x faster on startup -AND- accommodates multi-line port info
                try {
                    final byte[] bytes = Util.retrieveFileBytes(filePath); //? assumes UTF-8 encoding when constructing the String from bytes?
                    int p = 0;
                    int q = 1;
                    while (q < bytes.length) {   // get port name and info length line indexes
                        while (bytes[q] != '\n') {
                            q++;
                        }

                        // extract info length
                        int size = getSize(q, bytes);

                        // offset from that short line
                        // final String shortLine = new String( bytes, p, q - p );
                        p = q + 1;
                        q = p + 1;

                        // jump ahead by the info length
                        q += size;
                        q -= 2;

                        // wait...what, sometimes the info length is too short!?
                        while (bytes[q] != '\n') {
                            q++;
                        }

                        // longer info line
                        final String text = new String(bytes, p, q - p); // convert from bytes
                        p = q + 1;
                        q = p + 1;

                        final Portable port = PortFactory.createFromPortIndexFile(text);
                        if (port != Portable.NONE) {   // text parsed ok
                            final String ci_portName = port.getCaseInsensitiveName();

                            // no name collisions occurred, this means we only get the lastest version from the file
                            if (PortConstants.DEBUG && map.containsKey(ci_portName)) {
                                System.out.println(port);
                            }

                            map.put(ci_portName, port);
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {   // 2x slower but 10x more maintainable
                // misses the case where the port info crosses multiple lines
                try {
                    final Scanner scanner = new Scanner(filePath, StandardCharsets.UTF_8); // *THROWS* FileNotFoundException
                    while (scanner.hasNext()) // scan.useDelimiter( "\\n" ) <- this regex works but is not needed
                    {   // Scanner default is to read file line-by-line
                        final String line = scanner.nextLine(); // some lines are empty ""
                        if (line.length() > 40) {
                            final Portable port = PortFactory.createFromPortIndexFile(line);
                            if (port != Portable.NONE) {   // line parsed ok
                                final String ci_portName = port.getCaseInsensitiveName();

                                // no name collisions occur, this means we only get the lastest version from the file
                                if (PortConstants.DEBUG && map.containsKey(ci_portName)) {
                                    System.out.println(port);
                                }

                                map.put(ci_portName, port);
                            }
                        } else {   // wrong, just junk '\n' inside a {} Needs to keep going
                            // System.out.println( line );
                        }
                    }
                    scanner.close();
                } catch (FileNotFoundException ignored) // handled above with File.exist()
                {
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (PortConstants.DEBUG)
                System.out.println(PortsCatalog.class.getSimpleName() + "._parsePortIndex() ms=" + (System.currentTimeMillis() - startMillisec));
        }

        return map;
    }

    private static int getSize(int q, byte[] bytes) {
        int size = 0;
        int magnitude = 1;
        int r = q - 1;
        do {   // parse positive integer
            final int digit = bytes[r] - (byte) '0';
            size += digit * magnitude;
            magnitude *= 10;
            r--;
        }
        while (bytes[r] != 0x20); // space
        return size;
    }

    public PortsDep getDeps() {
        return fPortsDep;
    }

    public PortsVariants getPortsVariants() {
        return fPortsVariants;
    }

    public PortsInventory getPortsInventory() {
        return fPortsInventory;
    }

    /**
     * Look up a port by case-insensitive name.
     *
     * @param portName treated as case-insensitive
     * @return Portable.NONE if not found, there are a couple dozen of these
     */
    public Portable parse(final String portName) {
        final Portable port = fCiName_to_PortMap.get(portName.toLowerCase());
        return (port != null)
                ? port
                : Portable.NONE;
    }

//    /* *
//     * Compares old port entries to freshly CLI interrogated ports.
//     * Called after "port echo $PSEUDO_NAME".
//     *
//     * @param fromCliChangeSet updated information from CLI is a InstalledPort
//     */
//    synchronized private Set<Portable> inform( final Set<Portable> fromCliChangeSet )
//    {
//        for( final Portable cliPort : fromCliChangeSet )
//        {
//            if( PortsConstants.DEBUG ) System.out.println( PortsCatalog.class.getName() +".inform("+  cliPort +')' );
//
//            final Portable prevPort = fCiName_to_PortMap.get( cliPort.getCaseInsensitiveName() );
//            fCiName_to_PortMap.put( cliPort.getCaseInsensitiveName(), cliPort );
//
//            final int i = Util.indexOfIdentity( prevPort, fAllPorts );
//            if( i != Util.INVALID_INDEX )
//            {   // valid index
//                fAllPorts[ i ] = cliPort;
//                //... port changed event for detail views?
//            }
//        }
//
//        return fromCliChangeSet;
//    }

    public long getModificationEpoch(final Portable port) {
        return (vPortsDate != null)
                ? vPortsDate.getModificationEpoch(port)
                : -1L;
    }

    /**
     * Lengthy constructor operation only called from TheApplication.
     */
    public void scanDates() {
        vPortsDate = new PortsDate(this); // atomic assignment
    }

    public long getLastSyncEpoch() {
        if (vPortsDate != null) {
            return vPortsDate.getLastSyncEpoch();
        } else {   // still scanning or no Port tool
            return -1L;
        }
    }

    /**
     * TESTING
     *
     * @param args
     */
    static public void main(String[] args) {
        // test
        final long startMillisec = System.currentTimeMillis();
        final PortsCatalog portsCatalog = new PortsCatalog(NONE);
//        final Portable[] dependPorts = portsCatalog.getDeps().getFullDependenciesOf( portsCatalog.parse( "graphviz" ) );
        // OR -> portsCatalog.parse( "graphviz" ).buildFullDependencies()
        System.out.println(PortsCatalog.class.getSimpleName() + ".main() ms=" + (System.currentTimeMillis() - startMillisec));
        //      System.out.println( dependPorts.length +"="+ Arrays.toString( dependPorts ) );
    }
}


// "graphviz" -> 87=[apr, apr-util, autoconf, automake, bzip2, cairo, cmake, curl-ca-bundle, cyrus-sasl2, db46, expat, fontconfig, freetype, gd2, gdbm, gettext, ghostscript, glib2, gobject-introspection, gperf, groff, gts, help2man, jasper, jbig2dec, jbigkit, jpeg, kerberos5, lcms2, libedit, libffi, libiconv, libidn, libLASi, libpaper, libpixman, libpng, libtool, libxml2, m4, ncurses, neon, netpbm, openssl, p5.12-locale-gettext, pango, perl5, perl5.12, pkgconfig, psutils, python27, python_select, serf1, sqlite3, subversion, texinfo, tiff, unzip, urw-fonts, Xft2, xorg-bigreqsproto, xorg-inputproto, xorg-kbproto, xorg-libice, xorg-libpthread-stubs, xorg-libsm, xorg-libX11, xorg-libXau, xorg-libXaw, xorg-libxcb, xorg-libXdmcp, xorg-libXext, xorg-libXmu, xorg-libXt, xorg-renderproto, xorg-util-macros, xorg-xcb-proto, xorg-xcb-util, xorg-xcmiscproto, xorg-xextproto, xorg-xf86bigfontproto, xorg-xproto, xorg-xtrans, xpm, xrender, xz, zlib]
// missing?
//
// CLI -> port echo rdepof:graphviz | wc -l
// 87
//    apr
//    apr-util
//    autoconf
//    automake
//    bzip2
//    cairo
//    cmake
//    curl-ca-bundle
//    cyrus-sasl2
//    db46
//    expat
//    fontconfig
//    freetype
//    gd2
//    gdbm
//    gettext
//    ghostscript
//    glib2
//    gobject-introspection
//    gperf
//    groff
//    gts
//    help2man
//    jasper
//    jbig2dec
//    jbigkit
//    jpeg
//    kerberos5
//    lcms2
//    libedit
//    libffi
//    libiconv
//    libidn
//    libLASi
//    libpaper
//    libpixman
//    libpng
//    libtool
//    libxml2
//    m4
//    ncurses
//    neon
//    netpbm
//    openssl
//    p5.12-locale-gettext
//    pango
//    perl5
//    perl5.12
//    pkgconfig
//    psutils
//    python27
//    python_select
//    serf1
//    sqlite3
//    subversion
//    texinfo
//    tiff
//    unzip
//    urw-fonts
//    Xft2
//    xorg-bigreqsproto
//    xorg-inputproto
//    xorg-kbproto
//    xorg-libice
//    xorg-libpthread-stubs
//    xorg-libsm
//    xorg-libX11
//    xorg-libXau
//    xorg-libXaw
//    xorg-libxcb
//    xorg-libXdmcp
//    xorg-libXext
//    xorg-libXmu
//    xorg-libXt
//    xorg-renderproto
//    xorg-util-macros
//    xorg-xcb-proto
//    xorg-xcb-util
//    xorg-xcmiscproto
//    xorg-xextproto
//    xorg-xf86bigfontproto
//    xorg-xproto
//    xorg-xtrans
//    xpm
//    xrender
//    xz
//    zlib
