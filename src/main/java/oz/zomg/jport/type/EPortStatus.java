package oz.zomg.jport.type;

import oz.zomg.jport.common.Providers_.ColorProvidable;
import oz.zomg.jport.common.Providers_.TipProvidable;

import java.awt.*;


/**
 * From CLI "port echo XXX".
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public enum EPortStatus implements TipProvidable, ColorProvidable {
    ALL("all Ports from each Ports tree<BR>listed in the sources.conf file")
    //
    , UNINSTALLED("Ports in the Ports trees that aren't installed", Color.DARK_GRAY), INSTALLED("set of all installed Ports", Color.GREEN), OUTDATED("installed Ports that are out of date with<BR>respect to their current version/revision in the Ports trees", Color.RED)
    // ============
    //
    , ACTIVE("set of installed and active Ports", Color.BLUE), INACTIVE("set of installed but inactive Ports", Color.MAGENTA), ACTINACT("set of installed Ports that have both an<BR>active version and one or more inactive versions", Color.CYAN.darker()), REQUESTED("installed Ports that were explicitly<BR>asked for", Color.GREEN.darker()), UNREQUESTED("installed Ports that were installed only<BR>to satisfy dependencies", Color.BLUE.darker()), LEAVES("installed Ports that are unrequested and<BR>have no dependents", Color.GRAY), OBSOLETE("set of Ports that are installed but no<BR>longer exist in any Port trees", Color.YELLOW.darker());

    EPortStatus(final String tip) {
        this(tip, null);
    }

    EPortStatus(final String tip, final Color color) {
        fTip = tip;
        fColor = color;
    }

    final private String fTip;
    final private Color fColor;

    @Override
    public String provideTipText() {
        return "<HTML>Show " + fTip;
    }

    @Override
    public Color provideColor() {
        return fColor;
    }

    @Override
    public String toString() {
        return this.name().charAt(0) + this.name().substring(1).toLowerCase();
    }

    /**
     * Avoid array allocation as Java does not have immutable []s
     */
    static final public EPortStatus[] VALUES = EPortStatus.values();
}
