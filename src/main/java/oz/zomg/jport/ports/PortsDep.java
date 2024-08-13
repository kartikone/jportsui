package oz.zomg.jport.ports;

import oz.zomg.jport.PortConstants;
import oz.zomg.jport.type.Portable;

import java.util.*;


/**
 * Dependency caching.
 * Cross-cuts with PortsMark.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class PortsDep
//...    implements Dependable
{
    /**
     * Expected large set size optimization for unique dependencies.  Concurrency protection via 'synchronized'.
     */
    static final private Set<Portable> _SCRATCH_DEP_SET = new HashSet<Portable>(128);

    static {
    }

    /**
     * Cache non-sorted arrays.
     */
    final private Map<Portable, Portable[]>
            fPort_to_FullDependenciesOf = new HashMap<Portable, Portable[]>(), fPort_to_Dependants = new HashMap<Portable, Portable[]>();

    /**
     * Cache Sets for .contains()
     */
    final private Map<Portable, Set<Portable>>
            fPort_to_FullDependenciesOfSet = new HashMap<Portable, Set<Portable>>(), fPort_to_DependantSet = new HashMap<Portable, Set<Portable>>();

    final private PortsCatalog fPortsCatalog;

    /**
     * @param portsCatalog of this particular parsing of the ports catalog for future lazy referencing
     */
    PortsDep(final PortsCatalog portsCatalog) {
        fPortsCatalog = portsCatalog;
    }

    /**
     * @param aPort
     * @return has build or runtime dependencies on these ports
     */
    synchronized public Portable[] getFullDependenciesOf(final Portable aPort) {
        if (!fPort_to_FullDependenciesOf.containsKey(aPort)) {
            final Portable[] ports = buildFullDependenciesOf(aPort);
            fPort_to_FullDependenciesOf.put(aPort, ports);
            return ports;
        } else {
            return fPort_to_FullDependenciesOf.get(aPort);
        }
    }

    /**
     * @param ofPort
     * @return is a dependant of
     */
    synchronized public Portable[] getDependants(final Portable ofPort) {
        if (!fPort_to_Dependants.containsKey(ofPort)) {
            final Portable[] ports = buildDependants(ofPort);
            fPort_to_Dependants.put(ofPort, ports);
            return ports;
        } else {
            return fPort_to_Dependants.get(ofPort);
        }
    }

    synchronized public boolean hasDependency(final Portable aPort, final Portable onPort) {
        if (!fPort_to_FullDependenciesOfSet.containsKey(aPort)) {
            final Set<Portable> set = new HashSet<Portable>(Arrays.asList(getFullDependenciesOf(aPort))); // fast wrapper
            fPort_to_FullDependenciesOfSet.put(aPort, set);
        }

        return fPort_to_FullDependenciesOfSet.get(aPort).contains(onPort);
    }

    synchronized public boolean isADependant(final Portable aDependant, final Portable ofPort) {
        if (!fPort_to_DependantSet.containsKey(ofPort)) {
            final Set<Portable> set = new HashSet<Portable>(Arrays.asList(getDependants(ofPort))); // fast wrapper
            fPort_to_DependantSet.put(ofPort, set);
        }

        return fPort_to_DependantSet.get(ofPort).contains(aDependant);
    }

    /**
     * @param aPort
     * @return from Set
     */
    private Portable[] buildFullDependenciesOf(final Portable aPort) {
        recursiveDepSet(aPort);
        _SCRATCH_DEP_SET.remove(aPort); // include only dependicies of a MacPort, not the port itself

        final Portable[] depPorts = (!_SCRATCH_DEP_SET.isEmpty())
                ? _SCRATCH_DEP_SET.toArray(new Portable[_SCRATCH_DEP_SET.size()])
                : PortConstants.NO_PORTS;
        _SCRATCH_DEP_SET.clear(); // no leak and prepare for next time
        return depPorts;
    }

    /**
     * Very CPU expensive call.
     *
     * @param ofPort
     * @return from Set
     */
    private Portable[] buildDependants(final Portable ofPort) {
        final Set<Portable> dependantsSet = new HashSet<Portable>();

        final Portable[] allPorts = fPortsCatalog.getPortsInventory().getAllPorts();
        for (final Portable port : allPorts) {
            _SCRATCH_DEP_SET.clear(); // needed inside for() loop
            recursiveDepSet(port);

            if (_SCRATCH_DEP_SET.contains(ofPort)) {
                dependantsSet.add(port);
            }
        }

        dependantsSet.remove(ofPort); // do not include self
        final Portable[] depPorts = (!dependantsSet.isEmpty())
                ? dependantsSet.toArray(new Portable[dependantsSet.size()])
                : PortConstants.NO_PORTS;

        _SCRATCH_DEP_SET.clear(); // no leak and prepare for next time
        return depPorts;
    }

    private void recursiveDepSet(final Portable port) {
        _SCRATCH_DEP_SET.add(port);

        for (final Portable subPort : port.getDeps()) // port.hasDeps() hinders performance +25%
        {
            if (!_SCRATCH_DEP_SET.contains(subPort)) {   // optimized 225x by checking if .contains() first, went from 54 sec. down to 240 millisec
                recursiveDepSet(subPort);
            }
        }
    }

    @Deprecated
    private void recursiveDepSetDebug(final Portable port, final Set<Portable> recursDepSet, final int depth) {
        System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t".substring(0, depth) + port);

        for (final Portable subPort : port.getDeps()) {
            recursiveDepSetDebug(subPort, recursDepSet, depth + 1);
        }

        recursDepSet.add(port);
    }
}
