package oz.zomg.jport.ports;

import oz.zomg.jport.PortConstants;
import oz.zomg.jport.common.Util;
import oz.zomg.jport.type.EPortStatus;
import oz.zomg.jport.type.Portable;
import oz.zomg.jport.type.Portable.Predicatable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * Ordered array of all installed with multiply versioned, local variants
 * and also, all not installed ports as described by the "PortIndex" file.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class PortsInventory {
    static {
    }

    /**
     * All values in case-insensitive alphabetical order with descending version number tie-breakers.
     */
    final private Portable[] fAllPorts;

    PortsInventory() {
        fAllPorts = PortConstants.NO_PORTS;
    }

    /**
     * @param allPortSet values to array and sorts by case-insensitive name + version + revision.
     */
    PortsInventory(final Set<Portable> allPortSet) {
        fAllPorts = allPortSet.toArray(new Portable[allPortSet.size()]);
        Arrays.sort(fAllPorts);
    }

    /**
     * Read only, do not modify any indexes of this array.
     * Java does not provide a non-wrapped immutable array type so we are on the honor system.
     * Note: Collections.unmodifiableList( ... ) requires 3 allocations,
     * throws an UnsupportedOperationException on insert, and does not use square bracket syntax.
     *
     * @return in ascending alphabetical order with descending version + revision number tie-breakers
     */
    public Portable[] getAllPorts() {
        return fAllPorts;
    }

    /**
     * @param byStatus
     * @return in ascending alphabetical order with descending version number tie-breakers
     */
    public Portable[] filter(final EPortStatus byStatus) {
        return filter(new Predicatable() // anonymous class
        {
            @Override
            public boolean evaluate(final Portable port) {
                return port.hasStatus(byStatus);
            }
        });
    }

    /**
     * @param byPredicate
     * @return is reduced and in ascending alphabetical order with descending version number tie-breakers
     */
    public Portable[] filter(final Predicatable byPredicate) {
        final List<Portable> list = new ArrayList<Portable>(32);
        for (final Portable port : fAllPorts) {
            if (byPredicate.evaluate(port)) {
                list.add(port);
            }
        }

        return Util.createArray(Portable.class, list);
    }

    /**
     * Look up a matching new port by case-insensitive name + version + revision.
     *
     * @param otherPort from a previous catalog
     * @return Portable.NONE if not found
     */
    Portable equate(final Portable otherPort) {
        final int index = Arrays.binarySearch(fAllPorts, otherPort);
        return (index >= 0)
                ? fAllPorts[index]
                : Portable.NONE;
    }
}
