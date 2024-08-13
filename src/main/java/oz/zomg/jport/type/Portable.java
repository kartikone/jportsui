package oz.zomg.jport.type;

import oz.zomg.jport.common.Providers_.ColorProvidable;
import oz.zomg.jport.common.Providers_.TipProvidable;


/**
 * Methods required of a Port implementation.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public interface Portable
        extends
        Comparable<Portable>
        , ColorProvidable
        , TipProvidable {
    Portable NONE = BsdPort.BSD_NONE;

    String getName();

    String getShortDescription();

    String getLongDescription();

    String getLatestVersion();

    String getLatestRevision();

    String getHomepage();

    String getDomain();

    // multi
    String[] getCategories();

    String[] getLicenses();

    String[] getMaintainers();

    String[] getVariants();

    /**
     * @return Used for comparisons
     */
    String getCaseInsensitiveName();

    String getPortDirectory();

    long getModificationEpoch();

    // deps
    boolean hasDependency(final Portable onPort);

    Portable[] getFullDependencies();

    Portable[] getDeps();

    Portable[] getDependants();

    // status change request marks
    boolean isUnmarked();

    EPortMark getMark();

    void setMark(final EPortMark markEnum);

    void unmark();

    // only applies to installed ports
    boolean isInstalled();

    boolean hasStatus(final EPortStatus statusEnum);

    void setStatus(final EPortStatus statusEnum);

    String[] getVariantsInstalled();

    String getVersionInstalled();

    String getRevisionInstalled();


    // ================================================================================

    /**
     *
     */
    interface Providable {
        Portable providePort();
    }


    // ================================================================================

    /**
     * Reduce interface for a map-reduce para-lambda expression.
     */
    interface Predicatable {
        /**
         * No intended narrowing, wide open.
         */
        Predicatable ANY = new Predicatable() {
            @Override
            public boolean evaluate(Portable port) {
                return true;
            }
        };

        boolean evaluate(final Portable port);
    }
}

@Deprecated // not worth converting from type erased []s
interface Dependable {
    boolean hasDependency(final Portable aPort, final Portable onPort);

    Portable[] getFullDependenciesOf(final Portable aPort);

    boolean isADependant(final Portable aDependant, final Portable ofPort);

    /**
     * @param ofPort
     * @return is a dependant of
     */
    Portable[] getDependants(final Portable ofPort);
}
