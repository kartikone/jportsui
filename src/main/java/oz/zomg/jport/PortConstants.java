package oz.zomg.jport;

import oz.zomg.jport.common.Util;
import oz.zomg.jport.type.Portable;

import java.io.File;
import java.io.IOException;


/**
 * Constants specific to the JPortsUI application.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class PortConstants {
    static final public String
            APP_NAME = "JPortsUI", PROJ_HOSTING = "https://code.google.com/p/jportsui/downloads", PORTS_PATH;

    static final public boolean
            DEBUG = false, OPTIMIZATION = true // prematurely, it is the root of all evil -- D. Knuth
            , IS_SHOWING_FAVICON = true // Java not so good with ".ico" image format and transparency, bummer!
            , HAS_MAC_PORTS;

    static final public Portable[] NO_PORTS = new Portable[0];

    static {
        if (Util.isOnMac()) {

            File portsPath = new File("/opt/local/var/macports/sources/rsync.macports.org/macports/release/ports/");
            if (portsPath.exists()) {   // try source code distro
                PORTS_PATH = portsPath.getAbsolutePath();
                HAS_MAC_PORTS = true;
            } else {   // try the non-source code distro
                portsPath = new File("/opt/local/var/macports/sources/rsync.macports.org/macports/release/tarballs/ports/");
                PORTS_PATH = portsPath.getAbsolutePath();
                HAS_MAC_PORTS = portsPath.exists();
            }
        } else {   // BSD unix
            PORTS_PATH = "/usr/sbin/";
            HAS_MAC_PORTS = false;
        }
    }

    /**
     * @return .JAR creation date
     */
    static public String getVersion() {
        try {
            final byte[] bytes = Util.retrieveResourceBytes("/build-date.txt");
            return new String(bytes);
        } catch (IOException ex) {
            return "(unversioned)"; // was not built from run script
        }
    }

    private PortConstants() {
    }
}
