package oz.zomg.jport.type;

import oz.zomg.jport.PortConstants;
import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.HttpUtil;
import oz.zomg.jport.common.StringsUtil_;
import oz.zomg.jport.ports.PortsCatalog;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Implementation of Port info originating from a PortBuilder
 * instance via the MacPorts built "PortIndex" file.
 * Mostly immutable Port with lazily instantiated,
 * non-recursive external Port dependencies as
 * the entire Port index has to be constructed first.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
class BsdPort //... refactor IndexPort?
        implements Portable {
    /**
     * Guard value.
     */
    static final Portable BSD_NONE = new BsdPort();

    /**
     * Expected large set size optimization for unique dependencies.
     */
    static final private Set<Portable> _SCRATCH_PORT_SET = new HashSet<>(64);

    static {
    }

    final private String
            ci_name // case-insensitive name
            , name, description, long_description, homepage, epoch, version, revision, portdir, replaced_by, installs_libs;

    final private String[]
            variants, subports, depends_fetch, depends_extract, depends_lib, depends_build, depends_run, platforms, maintainers, license, categories;

    /**
     * Mutable because lazily instantiated.  Not recursive, see .getFullDeps() for that.
     */
    private Portable[] mUniqueDependencies = null;

    private int mHashCode = 0;

    /**
     * Constructor for the NONE port, to avoid use of 'null'.
     */
    private BsdPort() {
        ci_name =
                name =
                        description =
                                long_description =
                                        homepage =
                                                epoch =
                                                        version =
                                                                revision =
                                                                        portdir =
                                                                                replaced_by =
                                                                                        installs_libs = "";

        variants =
                subports =
                        depends_fetch =
                                depends_extract =
                                        depends_lib =
                                                depends_build =
                                                        depends_run =
                                                                platforms =
                                                                        maintainers =
                                                                                license =
                                                                                        categories = StringsUtil_.NO_STRINGS;
    }

    /**
     * Copy constructor.
     *
     * @param cp
     */
    BsdPort(final BsdPort cp) {
        ci_name = cp.ci_name;

        name = cp.name;
        homepage = cp.homepage;
        epoch = cp.epoch;
        version = cp.version; // latest version
        revision = cp.revision;

        description = cp.description;
        long_description = cp.long_description;
        portdir = cp.portdir;
        replaced_by = cp.replaced_by;
        installs_libs = cp.installs_libs;

        variants = cp.variants;
        subports = cp.subports;
        depends_fetch = cp.depends_fetch;
        depends_extract = cp.depends_extract;
        depends_lib = cp.depends_lib;
        depends_build = cp.depends_build;
        depends_run = cp.depends_run;
        platforms = cp.platforms;
        maintainers = cp.maintainers;
        license = cp.license;
        categories = cp.categories;

        mUniqueDependencies = cp.mUniqueDependencies;
    }

    /**
     * Construct from builder.
     *
     * @param pb
     */
    BsdPort(final PortBuilder pb) {
        ci_name = pb.name.toLowerCase().intern(); // needed for dependency resolution as some are mis-cased

        name = pb.name.intern(); // Java6 puts these in PermGen memory which is limited, Java7 does not
        homepage = pb.homepage.intern();
        epoch = pb.epoch.intern();
        version = pb.version.intern();
        revision = pb.revision.intern();

        description = pb.description;
        long_description = (!pb.long_description.equals(pb.description)) ? pb.long_description : pb.description; // short and long need to be different else GC long
        portdir = pb.portdir;
        replaced_by = pb.replaced_by;
        installs_libs = pb.installs_libs;

        variants = pb.variants;
        subports = pb.subports;
        depends_fetch = pb.depends_fetch;
        depends_extract = pb.depends_extract;
        depends_lib = pb.depends_lib;
        depends_build = pb.depends_build;
        depends_run = pb.depends_run;
        platforms = pb.platforms;
        categories = pb.categories;

        maintainers = (pb.maintainers.length == 1 && "nomaintainer".equals(pb.maintainers[0])) ? StringsUtil_.NO_STRINGS : pb.maintainers; // common cased
        license = (pb.license.length == 1 && "unknown".equals(pb.license[0])) ? StringsUtil_.NO_STRINGS : pb.license; // common cased
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getShortDescription() {
        return this.description;
    }

    @Override
    public String getLongDescription() {
        return this.long_description;
    }

    @Override
    public String getLatestVersion() {
        return this.version;
    }

    @Override
    public String getLatestRevision() {
        return this.revision;
    }

    @Override
    public String getHomepage() {
        return this.homepage;
    }

    @Override
    public String[] getCategories() {
        return this.categories;
    }

    @Override
    public String[] getLicenses() {
        return this.license;
    }

    @Override
    public String[] getMaintainers() {
        return this.maintainers;
    }

    @Override
    public String[] getVariants() {
        return this.variants;
    } // possible variants that can be installed

    @Override
    public String getCaseInsensitiveName() {
        return ci_name;
    }

    @Override
    public String getPortDirectory() {
        return portdir;
    }

    @Override
    public String getDomain() {
        return HttpUtil.getDomain(this.getHomepage());
    }

    @Override
    public boolean isInstalled() {
        return false;
    }

    /**
     * The current, actual, on disk, status of the Port.
     * Not the user's "marked" action for the Port CLI.
     * Overridden by InstalledPort.
     *
     * @param statusEnum
     * @return 'true' if in the bloom filter
     */
    @Override
    public boolean hasStatus(final EPortStatus statusEnum) {
        if (statusEnum == null) return false;

        return switch (statusEnum) {
            case ALL, UNINSTALLED -> true;
            default -> false; // any other stati require installation of the Port
        };
    }

    @Override
    public void setStatus(final EPortStatus statusEnum) {
        throw new IllegalArgumentException("Only " + InstalledPort.class.getCanonicalName() + " supports requesting status changes");
    }

    @Override
    public String getVersionInstalled() {
        return "";
    }

    @Override
    public String getRevisionInstalled() {
        return "0";
    }

    @Override
    public String[] getVariantsInstalled() {
        return StringsUtil_.NO_STRINGS;
    }

    static private void _addDeps(final PortsCatalog portsCatalog, final String[] depNames) {
        Arrays.stream(depNames).map(portsCatalog::parse).forEach(port -> {
            if (port != null) {   // found it as expected
                _SCRATCH_PORT_SET.add(port);
            } else {   // * OEM error, these dependencies asked for but not found <- System.err.println( dependency );
                // MPlayer
                // MPlayer
                // py25-pylons
                // py26-h5py
                // py26-pylons
                // py26-pylons
                // py26-h5py
                // py26-h5py
                // py27-h5py
                // py27-pylons
                // py27-h5py
                // py27-h5py
                // py31-django
                // py32-django
                // rb19-fattr
            }
        });
    }

    /**
     * @return non-recursive, unique dependencies
     */
    @Override
    public Portable[] getDeps() {
        if (mUniqueDependencies == null) {   // lazy instantiated
            if (this.depends_lib.length == 0
                    && this.depends_build.length == 0
                    && this.depends_run.length == 0
                    && this.depends_fetch.length == 0
                    && this.depends_extract.length == 0
            ) {   // no dependencies
                mUniqueDependencies = PortConstants.NO_PORTS;
            } else {   // unroll the unique deps add
                final PortsCatalog portsCatalog = TheApplication.INSTANCE.getPortsCatalog(); // alias
                _addDeps(portsCatalog, this.depends_lib);
                _addDeps(portsCatalog, this.depends_build);
                _addDeps(portsCatalog, this.depends_run);
                _addDeps(portsCatalog, this.depends_fetch);
                _addDeps(portsCatalog, this.depends_extract);

                mUniqueDependencies = _SCRATCH_PORT_SET.toArray(new Portable[0]); // checked for 0 above
                _SCRATCH_PORT_SET.clear(); // no leak and prepare for next time
                // defer until detail view -> Arrays.sort( mUniqueDependencies );
            }
        }

        return mUniqueDependencies; // is not recursive
    }

    /**
     * has dependencies on
     *
     * @return other ports this port has dependencies of being installed first
     */
    @Override
    public Portable[] getFullDependencies() {
        return TheApplication.INSTANCE.getPortsCatalog().getDeps().getFullDependenciesOf(this);
    }

    /**
     * is dependant of
     *
     * @return other ports that are dependant on this port being installed and activated before them
     */
    @Override
    public Portable[] getDependants() {
        return TheApplication.INSTANCE.getPortsCatalog().getDeps().getDependants(this);
    }

    @Override
    public boolean hasDependency(final Portable onPort) {
        return TheApplication.INSTANCE.getPortsCatalog().getDeps().hasDependency(this, onPort);
    }

    private boolean isADependant(final Portable ofPort) {
        return TheApplication.INSTANCE.getPortsCatalog().getDeps().isADependant(this, ofPort);
    }

    /**
     * @return 'true' if no mark has been applied by the user.
     */
    @Override
    public boolean isUnmarked() {
        return TheApplication.INSTANCE.getPortsMarker().isUnmarked(this);
    }

    /**
     * User's desired action.
     *
     * @return 'null' if not marked
     */
    @Override
    public EPortMark getMark() {
        return TheApplication.INSTANCE.getPortsMarker().getMark(this);
    }

    /**
     * Overwrites user's previous value.
     *
     * @param markEnum
     */
    @Override
    public void setMark(final EPortMark markEnum) {
        TheApplication.INSTANCE.getPortsMarker().setMark(this, markEnum);
    }

    /**
     * Remove any user marking from this Port.
     */
    @Override
    public void unmark() {
        TheApplication.INSTANCE.getPortsMarker().unmark(this);
    }

    @Override
    public long getModificationEpoch() {
        return TheApplication.INSTANCE.getPortsCatalog().getModificationEpoch(this);
    }

    /**
     * @return the long description even if the same as the short because sometimes the entire text of the short description does not fit in the table column
     */
    @Override
    public String provideTipText() {
        final String mark = (isUnmarked())
                ? ""
                : "<FONT color=#444444>" + getMark() + "→</FONT> "; // dark-gray, right arrow

        final StringBuilder tip = new StringBuilder("<HTML><CENTER><B>" + mark + getName() + "</B></CENTER><BR>");

        final String[] strings = this.getLongDescription().split("\\s"); // break on whitespace

        int i = 0;
        while (i < strings.length) {
            if (i != 0 && i % 10 == 0) {   // word wrap
                tip.append("<BR>");
            }

            final String text = strings[i].replace("{", "<B><I>").replace("}", "</I></B>"); // some descriptions use '{','}' for emphasis
            tip.append(text).append(' ');
            i++;
        }

        return tip.toString();
    }

    @Override
    public Color provideColor() {   // sequential check logic
        return hasStatus(EPortStatus.UNINSTALLED) ? Color.DARK_GRAY
                : hasStatus(EPortStatus.OUTDATED) ? Color.RED
                : hasStatus(EPortStatus.ACTIVE) ? Color.BLUE
                : hasStatus(EPortStatus.INACTIVE) ? Color.MAGENTA
                : hasStatus(EPortStatus.INSTALLED) ? Color.MAGENTA
                : Color.YELLOW.darker(); // ?
    }

    /**
     * @param obj
     * @return works equally well with InstalledPort as method declared 'final'
     */
    @Override
    final public boolean equals(final Object obj) {
        if (obj == this) return true;

        if (obj instanceof BsdPort other) {
            if (!this.ci_name.equals(other.getCaseInsensitiveName())) return false;
            if (this.isInstalled() != other.isInstalled()) return false;
            if (!this.isInstalled()) return true; // other will also be 'false'

            // both installed
            return this.getVersionInstalled().equals(other.getVersionInstalled())
                    && this.getRevisionInstalled().equals(other.getRevisionInstalled());
        }

        return false;
    }

    @Override
    final public int hashCode() {
        if (mHashCode == 0) {   // lazy init
            if (!this.isInstalled()) {
                mHashCode = this.ci_name.hashCode();
            } else {
                int hash = 7;
                hash = 67 * hash + this.getRevisionInstalled().hashCode();
                hash = 67 * hash + this.getVersionInstalled().hashCode();
                hash = 67 * hash + this.ci_name.hashCode();
                mHashCode = hash;
            }
        }

        return mHashCode;
    }

    /**
     * @param another
     * @return by case-insensitive name + version + revision.
     */
    @Override
    final public int compareTo(final Portable another) {
        if (another == this) return 0;

        final int comparedCiNameCode = this.ci_name.compareTo(another.getCaseInsensitiveName());
        if (comparedCiNameCode != 0) {   // name differs
            return comparedCiNameCode;
        } else {   // reverse order of version numbers
            final int comparedVersionCode = -1 * this.getVersionInstalled().compareTo(another.getVersionInstalled());
            if (comparedVersionCode != 0) {
                return comparedVersionCode;
            } else {   // revision in reverse order
                return -1 * this.getRevisionInstalled().compareTo(another.getRevisionInstalled());
            }
        }
    }

    @Override
    final public String toString() {
        return this.name;
    }
}
