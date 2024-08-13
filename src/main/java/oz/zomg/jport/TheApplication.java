package oz.zomg.jport;

import oz.zomg.jport.common.Elemental;
import oz.zomg.jport.common.Elemental.EElemental;
import oz.zomg.jport.common.Elemental.Listenable;
import oz.zomg.jport.common.Notification;
import oz.zomg.jport.common.Notification.Notifiable;
import oz.zomg.jport.common.Notification.OneArgumentListenable;
import oz.zomg.jport.common.Reset.Resetable;
import oz.zomg.jport.common.Reset.Reseter;
import oz.zomg.jport.gui.TheUiHolder;
import oz.zomg.jport.ports.PortsCatalog;
import oz.zomg.jport.ports.PortsCliUtil;
import oz.zomg.jport.ports.PortsMarker;
import oz.zomg.jport.type.CliPortInfo;
import oz.zomg.jport.type.EPortStatus;
import oz.zomg.jport.type.Portable;

import javax.swing.*;
import java.util.*;


/**
 * Manages the current PortsCatalog collection, specialized event notification,
 * and mutable status change request information as a singleton.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class TheApplication {
    /**
     * Manages the current PortsCatalog collection and mutable information as a singleton.
     */
    static final public TheApplication INSTANCE = new TheApplication();

    /**
     * Notifies when "PortIndex" file has been parsed and Port status queried.
     */
    final private Reseter fCatalogResetNotifier = new Reseter();

    /**
     * Facility for driving progress bar when ports CLI echo Ports status query is ongoing.
     */
    final private Notification.Notifier<EPortStatus> fEchoStatusNotifier = new Notification.Notifier<EPortStatus>();

    /**
     * Get notifications of individual Port C.R.U.D. (Create, Retrieve, Update, Delete).
     */
    final private Elemental.Notifier<Portable> fPortElementNotifier = new Elemental.Notifier<Portable>(Portable.NONE);

    final private PortsMarker fPortsMarker = new PortsMarker();

    volatile private PortsCatalog vCurrentPortsCatalog = PortsCatalog.NONE;

    /**
     * Singleton constructor.
     * Needs to happen quickly (i.e. be non-blocking) as many Threads are
     * waiting on TheApplication.INSTANCE being non-null!
     */
    private TheApplication() {
    }

    /**
     * Method broken out here as the constructor needed to be fast because
     * many Threads were waiting on TheApplication.INSTANCE being non-null!
     */
    public void init() {
        final Object MONITOR = new Object();

        // fork GUI building into Event Dispatch thread as required by Swing guidelines
        // anonymous class
        SwingUtilities.invokeLater(() -> {
            synchronized (MONITOR) {
                TheOsBinaries.INSTANCE.has("?"); // hey lazy Swing thread, do some extraneous work!
                TheUiHolder.INSTANCE.init(); // start Swing in EDT thread
                MONITOR.notifyAll();
            }
        });

        // if only one Processor Core, wait until GUI finishes constructing
        if (Runtime.getRuntime().availableProcessors() == 1) {
            try {
                synchronized (MONITOR) {
                    while (!TheUiHolder.isReady()) {
                        MONITOR.wait(2000); // *BLOCKS*
                    }
                }
            } catch (InterruptedException ignored) {
            }
        }

        // but start parsing "PortsIndex" in this thread
        TheApplication.INSTANCE.probeUpdate(); // *BLOCKS*

        // enable ports table in EDT even if GUI reported not ready
        // anonymous class
        SwingUtilities.invokeLater(TheUiHolder.INSTANCE::goLive);
    }

    /**
     * @return facility for letting the user indicate desired port status change requests
     */
    public PortsMarker getPortsMarker() {
        return fPortsMarker;
    }

    /**
     * @return instance managing the current contents of the "PortIndex" file
     */
    public PortsCatalog getPortsCatalog() {
        return vCurrentPortsCatalog;
    }

    /**
     * Reload catalog index, status, and modification dates.
     * The time stamp of the current catalog is the refresh time.
     * Completely refreshes the GUI in regards to the status of all known Ports.
     * Enables the main table for selections.
     */
    synchronized public void probeUpdate() {
        // complete parsing "PortsIndex" in this thread
        vCurrentPortsCatalog = new PortsCatalog(vCurrentPortsCatalog);

        //  since I/O bound, fetch the port folder modification dates from the file system in a seperate thread
        // anonymous class
        new Thread
                (() -> vCurrentPortsCatalog.scanDates()
                        , TheApplication.class.getCanonicalName()
                ).start();

        // from the Swing thread, reload port table, clear selection etc.
        if (TheUiHolder.isReady()) {
            // anonymous class
            SwingUtilities.invokeLater(() -> {
                TheApplication.INSTANCE.causeReset();
                deprang();
            });
        }
    }

    /**
     * Unmark any applied changes from CLI or JPortsUI.
     * Turn the JTable back on.
     */
    public void deprang() {
        // anonymous class
        SwingUtilities.invokeLater(() -> {
            getPortsMarker().exchangeAudit(vCurrentPortsCatalog);
            TheUiHolder.INSTANCE.goLive();
        });
    }

    /**
     * Subscribe to Ports Catalog reset notifications.
     *
     * @return just the listener .add() & .remove() aspect of the interface
     */
    public Notifiable<Resetable> getResetNotifier() {
        return fCatalogResetNotifier;
    }

    /**
     * Subscribe to Ports CLI status notifications.
     *
     * @return just the listener .add() & .remove() aspect of the interface
     */
    public Notifiable<OneArgumentListenable<EPortStatus>> getEchoStatusNotifier() {
        return fEchoStatusNotifier;
    }

    /**
     * Subscribe to Port elemental C.R.U.D. (Create, Retrieve, Update, Delete) notifications.
     *
     * @return just the listener .add() & .remove() aspect of the interface
     */
    public Notifiable<Listenable<Portable>> getCrudNotifier() {
        return fPortElementNotifier;
    }

    /**
     * Sends message to all Port elemental C.R.U.D. (Create, Retrieve, Update, Delete) listeners.
     *
     * @param elemental
     * @param port
     */
    public void causeCrudNotification(final EElemental elemental, final Portable port) {
        fPortElementNotifier.causeNotification(elemental, port);
    }

    public void causeReset() {
        fCatalogResetNotifier.causeReset();
    }

    /**
     * Full accounting avoids asking for All ports or Uninstalled ports as
     * these are assumed from the "PortIndex" parsing.
     * <p>
     * Note: Inefficient but I do not know a way to get all status attributes
     * for each installed port, see "man port"
     *
     * @return as reported by the CLI "port echo installed" all of which are type InstalledPort
     */
    public Map<EPortStatus, Set<CliPortInfo>> cliEchoAllPortStatus() {
        final Map<EPortStatus, Set<CliPortInfo>> status_to_InfoSet_Map = new EnumMap<>(EPortStatus.class);

        Arrays.stream(EPortStatus.VALUES).forEach(statusEnum -> {
            switch (statusEnum) { // fall-thru
                case ALL, UNINSTALLED -> {   // do not run CLI on these, too large/slow for a sanity check
                    final Set<CliPortInfo> emptySet = Collections.emptySet();
                    status_to_InfoSet_Map.put(statusEnum, emptySet);
                }
                default -> {
                    fEchoStatusNotifier.causeNotification(statusEnum);
                    final Set<CliPortInfo> cpiSet = PortsCliUtil.cliEcho(statusEnum);
                    status_to_InfoSet_Map.put(statusEnum, cpiSet);
                }
            }
        });

        return status_to_InfoSet_Map;
    }
}
