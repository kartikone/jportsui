package jport;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jport.PortsConstants.EPortMark;
import jport.PortsConstants.EPortStatus;
import jport.common.Elemental.EElemental;
import jport.common.Reset.Resetable;
import jport.common.StringsUtil_;
import jport.type.Portable;


/**
 * Manages installing with different variant names than what
 * the default "Portfile" would recommend.
 *
 * @author sbaber
 */
public class PortsVariants
{
    static
    {}

    /** Sparsely populated. Applies mutable information to an immutable object. */
    final private Map<Portable,Set<String>> fPort_to_ApplyVariantSet_Map = new HashMap<Portable,Set<String>>();

    PortsVariants()
    {
        if( TheApplication.INSTANCE != null )
        {
            TheApplication.INSTANCE.getResetNotifier().addListener( new Resetable() // anonymous class
                    {   @Override public void reset()
                        {   fPort_to_ApplyVariantSet_Map.clear();
                        }
                    } );
        }
    }

    /**
     * Using the '@' syntax when any difference is detected between installed variants and applying variants.
     * Note: Variant names never contain a hyphen but port names can.
     * I'm assuming that is how ambiguities which avoid '@' occur.
     * Ex. "vim" or "vim@+ruby+x11-universal"
     *<BR>
     * Port man page
     *<BLOCKQUOTE>
     * Port variants can specified as +name, which indicates the variant is desired, or -name, indicating the contrary.
     * In case of ambiguities, a port can be fully specified with the <code>@version_revision+variants</code> format.
     *</BLOCKQUOTE>
     *
     * @param ofPort
     * @return if no desired variant changes, then will not return any variant modifiers
     */
    public String getNameVariant( final Portable ofPort )
    {
        final String portName = ofPort.getName().trim();

        // does not know the Mark but needs to be included for versioned Activate / Inactivate
        final String versionRevision = ( ofPort.isInstalled() == true )
                ? ofPort.getVersionInstalled() +'_'+ ofPort.getRevisionInstalled()
                : "";

        // variant changes
        final String variant;
        if( fPort_to_ApplyVariantSet_Map.containsKey( ofPort ) == true )
        {
            final Set<String> installedVariantSet = getInstalledVariantSet( ofPort, false );
            final Set<String> applyVariantSet = fPort_to_ApplyVariantSet_Map.get( ofPort );

            if( applyVariantSet.equals( installedVariantSet ) == false )
            {   // sanity check
                final StringBuilder sb = new StringBuilder();

                final Set<String> includeVariantSet = new TreeSet<String>( applyVariantSet );
                for( final String includeVariant : includeVariantSet )
                {
                    sb.append( '+' ).append( includeVariant.trim() );
                }
                //... might need to return to exclusionSet and '-'

                variant = sb.toString();
            }
            else
            {   // no change to variants
                variant = "";
            }
        }
        else
        {
            variant = "";
        }

        // required to apply variants or version number
        final String at = ( variant.isEmpty() && versionRevision.isEmpty() ) ? "" : "@";

        return portName + at + versionRevision + variant;
    }

    /**
     *
     * @param ofPort
     * @param aVariant
     * @return 'true' if user desired or currently installed variant of the port
     */
    public boolean isApplicableVariant( final Portable ofPort, final String aVariant )
    {
        final Set<String> applyVariantSet = fPort_to_ApplyVariantSet_Map.get( ofPort );
        if( applyVariantSet != null )
        {   // variants have been user edited
            return applyVariantSet.contains( aVariant );
        }
        else if( ofPort.isInstalled() == true )
        {   // Port is installed
            return StringsUtil_.equals( aVariant, ofPort.getVariantsInstalled() );
        }
        else
        {   // Port not installed
            return false;
        }
    }

    private Set<String> getInstalledVariantSet( final Portable port, final boolean isMutableNeeded )
    {
        if( port.isInstalled() == true )
        {   // Port is installed
            return new HashSet<String>( Arrays.asList( port.getVariantsInstalled() ) ); // fast wrapper
        }
        else
        {   // Port not installed
            if( isMutableNeeded == false )
            {   // NB7.2 generics compiler bug errs with ()?:
                return Collections.emptySet();
            }
            else
            {   // mutable set
                return new HashSet<String>();
            }
                   
            //... need default variants from each port?
            // also man page sez "Global variants used when a port is installed ->  ${prefix}/etc/macports/variants.conf"
        }
    }

    private boolean isChanged( final Portable onPort ) // , final Set<String> desiredVariantSet )
    {
        final Set<String> applyVariantSet = fPort_to_ApplyVariantSet_Map.get( onPort );
        if( applyVariantSet != null )
        {
            final Set<String> installedVariantSet = getInstalledVariantSet( onPort, false );
            return applyVariantSet.equals( installedVariantSet ) == false; // changed if sets are not equal
        }
        else
        {   // no user edits
            return false;
        }
    }

    /**
     *
     * @param onPort
     * @param aVariant
     * @param isInstalling 'false' to uninstall variant
     */
    public void setVariant( final Portable onPort, final String aVariant, final boolean isInstalling )
    {
        if( fPort_to_ApplyVariantSet_Map.containsKey( onPort ) == false )
        {   // sparsely added
            final Set<String> applyVariantSet = getInstalledVariantSet( onPort, true );
            fPort_to_ApplyVariantSet_Map.put( onPort, applyVariantSet );
        }

        final Set<String> applyVariantSet = fPort_to_ApplyVariantSet_Map.get( onPort );
        if( isInstalling == true )
        {   // install variant
            applyVariantSet.add( aVariant );
        }
        else
        {   // uninstall variant
            applyVariantSet.remove( aVariant );
        }

        if( isChanged( onPort ) == false )
        {   // enforce sparse population
            fPort_to_ApplyVariantSet_Map.remove( onPort );
        }
        else
        {   // do something to an unmarked port
            if( onPort.isUnmarked() == true )
            {   //... probably will have to mark any dependencies with equivalent variant also
                final EPortMark mark = ( onPort.hasStatus( EPortStatus.OUTDATED ) == true )
                        ? EPortMark.Upgrade
                        : EPortMark.Install;
                onPort.setMark( mark );
            }
        }

        TheApplication.INSTANCE.causeCrudNotification( EElemental.UPDATED, onPort );
    }

//    private String[] getApplyVariants( final Portable ofPort )
//    {
//        final Set<String> applyVariantSet = fPort_to_ApplyVariantSet_Map.get( ofPort );
//        if( applyVariantSet != null )
//        {   // variants have been user edited
//            return StringsUtil_.toStrings( applyVariantSet );
//        }
//        else if( ofPort instanceof CliPort )
//        {   // Port is installed
//            final CliPort cliPort = (CliPort)ofPort;
//            return cliPort.getVariantsInstalled();
//        }
//        else
//        {   // Port not installed
//            return StringsUtil_.NO_STRINGS;
//        }
//    }
}
