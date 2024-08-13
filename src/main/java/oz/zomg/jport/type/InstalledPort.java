package oz.zomg.jport.type;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;


/**
 * These instances have more information from CLI status calls than from the "PortIndex" parse for BsdPort instances.
 * Usually locally installed, but possibly outdated.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
class InstalledPort extends BsdPort {
    final private String fVersionInstalled;
    final private String fRevisionInstalled;
    final private String[] fVariantsInstalled;

    final Set<EPortStatus> fStatusSet = EnumSet.noneOf(EPortStatus.class);

    InstalledPort
            (final BsdPort copyPort
                    , final String versionInstalled
                    , final String revisionInstalled
                    , final String[] variantsInstalled
            ) {
        super(copyPort);

        fStatusSet.add(EPortStatus.ALL);
        fStatusSet.add(EPortStatus.INSTALLED);

        fVersionInstalled = versionInstalled;
        fRevisionInstalled = revisionInstalled;
        fVariantsInstalled = variantsInstalled;

        Arrays.sort(variantsInstalled);

//.. did talk to the CLI
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void setStatus(final EPortStatus statusEnum) {
        fStatusSet.add(statusEnum);

//    if(false) //... each installed port may have seperate versions now
//        switch( statusEnum )
//        {
//            case UNINSTALLED :
//                    clearStatus( EPortStatus.INSTALLED );
//                    clearStatus( EPortStatus.ACTIVE );
//                    clearStatus( EPortStatus.INACTIVE );
//                    clearStatus( EPortStatus.ACTINACT );
//                    clearStatus( EPortStatus.OUTDATED );
//                    clearStatus( EPortStatus.LEAVES );
//                    break;
//
//            case INSTALLED :
//                    clearStatus( EPortStatus.UNINSTALLED );
//                    break;
//
//            case ACTIVE :
//                    clearStatus( EPortStatus.INACTIVE );
//                    break;
//
//            case INACTIVE :
//                    clearStatus( EPortStatus.ACTIVE );
//                    break;
//
//            case REQUESTED :
//                    clearStatus( EPortStatus.UNREQUESTED );
//                    break;
//
//            case UNREQUESTED :
//                    clearStatus( EPortStatus.REQUESTED );
//                    break;
//        }
    }

    private void clearStatus(final EPortStatus status) {
        fStatusSet.remove(status);
    }

    @Override
    public boolean hasStatus(final EPortStatus status) {
        return fStatusSet.contains(status);
    }

    @Override
    public String getVersionInstalled() {
        return fVersionInstalled;
    }

    @Override
    public String getRevisionInstalled() {
        return fRevisionInstalled;
    }

    @Override
    public String[] getVariantsInstalled() {
        return fVariantsInstalled;
    }
}
