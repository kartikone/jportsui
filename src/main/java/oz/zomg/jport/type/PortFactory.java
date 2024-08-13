package oz.zomg.jport.type;


/**
 * Static factory methods for creating Portable instances.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class PortFactory {
    private PortFactory() {
    }

    /**
     * @param text plain-text entry for the port
     * @return NONE if was not able to parse the line
     */
    static public Portable createFromPortIndexFile(final String text) {
        final PortBuilder pb = new PortBuilder(text);
        return (pb.didParse())
                ? new BsdPort(pb)
                : Portable.NONE;
    }

    static public Portable createFromCli(final Portable port, final CliPortInfo cpi) {
        return new InstalledPort
                ((BsdPort) port
                        , cpi.getVersionInstalled()
                        , cpi.getRevisionInstalled()
                        , cpi.getVariants()
                );
    }
}
