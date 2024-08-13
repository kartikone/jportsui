package oz.zomg.jport.ports;

import oz.zomg.jport.PortConstants;
import oz.zomg.jport.common.StringsUtil_;
import oz.zomg.jport.common.Util;
import oz.zomg.jport.type.Portable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;


/**
 * Tracks mutable rsync directory dates of individual Ports.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
class PortsDate {
    /**
     * Applies mutable information to an immutable object.
     */
    final private Map<Portable, Long> fPort_to_EpochMap = new HashMap<Portable, Long>();

    /**
     * Bi-directional mapping.
     */
    private Map<Long, Set<Portable>> mEpoch_to_PortSet_Map = null;

    static {
    }

    /**
     * Generally, a slow operation but only exposed through an atomic assignment
     * so no need for concurrent Map.
     *
     * @param portCatalog
     */
    PortsDate(final PortsCatalog portCatalog) {
        if (!PortConstants.HAS_MAC_PORTS) return; // devel

        final long startMillisec = System.currentTimeMillis();
        final File portsPath = new File(PortConstants.PORTS_PATH);

// putting ports array in directory order only 20ms (10%) faster but array sorting takes 100ms making it 25% slower overall
//            final Portable[] copy = ports.clone();
//            Arrays.sort( copy, new Comparator<Portable>() // anonymous class
//                    {   @Override public int compare( Portable o1, Portable o2 )
//                        {   return o1.getPortDirectory().compareTo( o2.getPortDirectory() );
//                        }
//                    } );

        //... ends up double checking some of the inodes
        final Portable[] allPorts = portCatalog.getPortsInventory().getAllPorts();
        for (final Portable port : allPorts) {   // none of the mod dates are = 0L
            final File filePath = new File(portsPath, port.getPortDirectory() + "/Portfile");
            fPort_to_EpochMap.put(port, filePath.lastModified()); // auto-box

// different date with the "files" dir modification, not sure what it implies
//            File filePath = new File( portsPath, port.getPortDirectory() +"/files" );
//            filePath = ( filePath.exists() == true )
//                    ? filePath
//                    : new File( portsPath, port.getPortDirectory() +"/Portfile" );
//            if( filePath.exists() == false ) System.err.println( "FNF="+ filePath );
        }

        if (PortConstants.DEBUG)
            System.out.println(PortsDate.class.getSimpleName() + "<init> ms=" + (System.currentTimeMillis() - startMillisec));
    }

    long getModificationEpoch(final Portable port) {
        final Long epochLong = fPort_to_EpochMap.get(port);
        return (epochLong != null) ? epochLong.longValue() : -1L;
    }

    /**
     * @param port
     * @return
     */
    String getModificationDate(final Portable port) {
        return StringsUtil_.getDateString(getModificationEpoch(port));
    }

    /**
     * @return sequential epochs to Port Sets where keys are a NavigableSet
     */
    private Map<Long, Set<Portable>> getInverseMultiMapping() {
        if (mEpoch_to_PortSet_Map == null) {   // lazy instantiate
            mEpoch_to_PortSet_Map = Util.createInverseMultiMapping(true, false, fPort_to_EpochMap);
        }

        return mEpoch_to_PortSet_Map; // Collections.unmodifiableMap( ... ) fubars (NavigableSet<Long>) cast
    }

    /**
     * @return epoch of last "PortFile" modification time
     */
    public long getLastSyncEpoch() {
        final Map<Long, Set<Portable>> map = getInverseMultiMapping();
        final NavigableSet<Long> navSet = (NavigableSet<Long>) map.keySet();
        return navSet.getLast().longValue();
    }
}
