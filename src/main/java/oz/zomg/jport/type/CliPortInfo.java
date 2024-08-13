package oz.zomg.jport.type;

import java.util.*;


/**
 * Information returned from any CLI 'port echo' status command.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class CliPortInfo {
    static {
    }

    final private String
            fCi_name, fName, fVersionInstalled, fRevisionInstalled;
    final private String[] fVariants;

    /**
     * @param name
     * @param versionInstalled
     * @param revisionInstalled
     * @param variants
     */
    public CliPortInfo
    (final String name
            , final String versionInstalled
            , final String revisionInstalled
            , final String[] variants
    ) {
        fCi_name = name.toLowerCase().intern();
        fName = name;
        fVersionInstalled = versionInstalled;
        fRevisionInstalled = revisionInstalled;
        fVariants = variants;
    }

    public String getCaseInsensitiveName() {
        return fCi_name;
    }

    public String getName() {
        return fName;
    }

    public String getVersionInstalled() {
        return fVersionInstalled;
    }

    public String getRevisionInstalled() {
        return fRevisionInstalled;
    }

    public String[] getVariants() {
        return fVariants;
    }

    @Override
    final public boolean equals(final Object obj) {
        if (obj == this) return true;

        if (obj instanceof CliPortInfo other) {
            return this.fName.equals(other.fName)
                    && this.fVersionInstalled.equals(other.fVersionInstalled)
                    && this.fRevisionInstalled.equals(other.fRevisionInstalled);
        }

        return false;
    }

    @Override
    final public int hashCode() {
        int hash = 7;
        hash = 11 * hash + fRevisionInstalled.hashCode();
        hash = 11 * hash + fVersionInstalled.hashCode();
        hash = 11 * hash + fName.hashCode();
        return hash;
    }

    /**
     * @return as displayed by the 'port' CLI tool
     */
    @Override
    public String toString() {
        return fName + '@' + fVersionInstalled + '_' + fRevisionInstalled;
    }

    /**
     * Reverse the CLI Port status utility mapping to a more usable form.
     *
     * @param kvMap
     * @return
     */
    static public Map<CliPortInfo, Set<EPortStatus>> createInverseMultiMapping(final Map<EPortStatus, ? extends Collection<CliPortInfo>> kvMap) {
        final Map<CliPortInfo, Set<EPortStatus>> invMap = new HashMap<CliPortInfo, Set<EPortStatus>>();

        for (final Map.Entry<EPortStatus, ? extends Collection<CliPortInfo>> entry : kvMap.entrySet()) {
            final Collection<CliPortInfo> cpiCollection = entry.getValue(); // alias
            final EPortStatus statusEnum = entry.getKey(); // alias

            if (cpiCollection != null) {   // values may be 'null', but keys can not be
                for (final CliPortInfo cpiKey : cpiCollection) {
                    if (!invMap.containsKey(cpiKey)) {   // new key
                        final Set<EPortStatus> valueSet = EnumSet.of(statusEnum); // mutable with single element
                        invMap.put(cpiKey, valueSet);
                    } else {   // seen the inverse key before
                        final Set<EPortStatus> valueSet = invMap.get(cpiKey);
                        valueSet.add(statusEnum);
                    }
                }
            }
        }

        // replacing 'null' value Sets with Collections.emptySet() does not have to be done because 'null' keys are prohibited
        return invMap;
    }
}
