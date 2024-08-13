package oz.zomg.jport.ports;

import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.Elemental.EElemental;
import oz.zomg.jport.common.Reset.Resetable;
import oz.zomg.jport.common.StringsUtil_;
import oz.zomg.jport.type.EPortMark;
import oz.zomg.jport.type.EPortStatus;
import oz.zomg.jport.type.Portable;

import java.util.*;


/**
 * Manages installing with different variant names than what
 * the default "Portfile" would recommend.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class PortsVariants {
    static {
    }

    /**
     * Sparsely populated. Applies mutable information to an immutable object.
     */
    final private Map<Portable, Set<String>> fPort_to_ApplyVariantSet_Map = new HashMap<Portable, Set<String>>();

    PortsVariants() {
        if (TheApplication.INSTANCE != null) {
            TheApplication.INSTANCE.getResetNotifier().addListener(new Resetable() // anonymous class
            {
                @Override
                public void reset() {
                    fPort_to_ApplyVariantSet_Map.clear();
                }
            });
        }
    }

    /**
     * Using the '@' syntax when any difference is detected between installed variants and applying variants.
     * Note: Variant names never contain a hyphen but port names can.
     * I'm assuming that is how ambiguities which avoid '@' occur.
     * Ex. "vim" or "vim@+ruby+x11-universal"
     * <BR>
     * Port man page
     * <BLOCKQUOTE>
     * Port variants can specified as +name, which indicates the variant is desired, or -name, indicating the contrary.
     * In case of ambiguities, a port can be fully specified with the <CODE>@version_revision+variants</CODE> format.
     * </BLOCKQUOTE>
     *
     * @param ofPort
     * @return if no desired variant changes, then will not return any variant modifiers
     */
    public String getNameVariant(final Portable ofPort) {
        final String portName = ofPort.getName().trim();

        // does not know the Mark but needs to be included for versioned Activate / Inactivate
        final String versionRevision = (ofPort.isInstalled())
                ? ofPort.getVersionInstalled() + '_' + ofPort.getRevisionInstalled()
                : "";

        // variant changes
        final String variant;
        if (fPort_to_ApplyVariantSet_Map.containsKey(ofPort)) {
            final Set<String> installedVariantSet = getInstalledVariantSet(ofPort, false);
            final Set<String> applyVariantSet = fPort_to_ApplyVariantSet_Map.get(ofPort);

            if (!applyVariantSet.equals(installedVariantSet)) {   // sanity check
                final StringBuilder sb = new StringBuilder();

                final Set<String> includeVariantSet = new TreeSet<String>(applyVariantSet);
                for (final String includeVariant : includeVariantSet) {
                    sb.append('+').append(includeVariant.trim());
                }
                //... might need to return to exclusionSet and '-'

                variant = sb.toString();
            } else {   // no change to variants
                variant = "";
            }
        } else {
            variant = "";
        }

        // required to apply variants or version number
        final String at = (variant.isEmpty() && versionRevision.isEmpty()) ? "" : "@";

        return portName + at + versionRevision + variant;
    }

    /**
     * @param ofPort
     * @param aVariant
     * @return 'true' if user desired or currently installed variant of the port
     */
    public boolean isApplicableVariant(final Portable ofPort, final String aVariant) {
        final Set<String> applyVariantSet = fPort_to_ApplyVariantSet_Map.get(ofPort);
        if (applyVariantSet != null) {   // variants have been user edited
            return applyVariantSet.contains(aVariant);
        } else if (ofPort.isInstalled()) {   // Port is installed
            return StringsUtil_.equals(aVariant, ofPort.getVariantsInstalled());
        } else {   // Port not installed
            return false;
        }
    }

    private Set<String> getInstalledVariantSet(final Portable port, final boolean isMutableNeeded) {
        if (port.isInstalled()) {   // Port is installed
            return new HashSet<String>(Arrays.asList(port.getVariantsInstalled())); // fast wrapper
        } else {   // Port not installed
            if (!isMutableNeeded) {   // NB7.2 generics compiler bug errs with ()?:
                return Collections.emptySet();
            } else {   // mutable set
                return new HashSet<String>();
            }

            //... need default variants from each port?
            // also man page sez "Global variants used when a port is installed ->  ${prefix}/etc/macports/variants.conf"
        }
    }

    private boolean isChanged(final Portable onPort) // , final Set<String> desiredVariantSet )
    {
        final Set<String> applyVariantSet = fPort_to_ApplyVariantSet_Map.get(onPort);
        if (applyVariantSet != null) {
            final Set<String> installedVariantSet = getInstalledVariantSet(onPort, false);
            return !applyVariantSet.equals(installedVariantSet); // changed if sets are not equal
        } else {   // no user edits
            return false;
        }
    }

    /**
     * @param onPort
     * @param aVariant
     * @param isInstalling 'false' to uninstall variant
     */
    public void setVariant(final Portable onPort, final String aVariant, final boolean isInstalling) {
        if (!fPort_to_ApplyVariantSet_Map.containsKey(onPort)) {   // sparsely added
            final Set<String> applyVariantSet = getInstalledVariantSet(onPort, true);
            fPort_to_ApplyVariantSet_Map.put(onPort, applyVariantSet);
        }

        final Set<String> applyVariantSet = fPort_to_ApplyVariantSet_Map.get(onPort);
        if (isInstalling) {   // install variant
            applyVariantSet.add(aVariant);
        } else {   // uninstall variant
            applyVariantSet.remove(aVariant);
        }

        if (!isChanged(onPort)) {   // enforce sparse population
            fPort_to_ApplyVariantSet_Map.remove(onPort);
        } else {   // do something to an unmarked port
            if (onPort.isUnmarked()) {   //... probably will have to mark any dependencies with equivalent variant also
                final EPortMark mark = (onPort.hasStatus(EPortStatus.OUTDATED))
                        ? EPortMark.Upgrade
                        : EPortMark.Install;
                onPort.setMark(mark);
            }
        }

        TheApplication.INSTANCE.causeCrudNotification(EElemental.UPDATED, onPort);
    }
}
