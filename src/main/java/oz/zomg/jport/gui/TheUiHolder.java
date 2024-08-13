package oz.zomg.jport.gui;

import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.Notification.Notifiable;
import oz.zomg.jport.common.Reset.Resetable;
import oz.zomg.jport.common.Reset.Reseter;
import oz.zomg.jport.gui.panel.JPanel_CommandBar;
import oz.zomg.jport.gui.table.TableModel_Port;
import oz.zomg.jport.gui.window.JFrame_Main;
import oz.zomg.jport.type.Portable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Singleton that manages GUI components, several of which must have their references retained.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class TheUiHolder
        implements
        ListSelectionListener
        , ActionListener {
    /**
     * Wait short period of time before requesting of EDT thread.
     */
    static final private int SELECTION_DEFER_MILLISEC = 175;

    /**
     * Static because doesn't require the INSTANCE to be finished constructing.
     */
    static final private Reseter RESET_FILTER_NOTIFIER = new Reseter();

    /**
     * Singleton that manages GUI components.
     */
    static final public TheUiHolder INSTANCE = new TheUiHolder();

    static {
        ToolTipManager.sharedInstance().setDismissDelay(60 * 1000); // 60 secs.
    }

    final private Commander fCommander = new Commander();

    /**
     * Let selection stabilize before updating UI. This reduces CPU thrashing of the Dependants check.
     */
    final private javax.swing.Timer fSelectionHoldingTimer = new Timer(SELECTION_DEFER_MILLISEC, this);

    final private TableModel_Port fTableModel_Port = new TableModel_Port();
    final private PortFilterPredicates fPortFilterPredicate = new PortFilterPredicates(fTableModel_Port);
    final private JFrame jFrame_Main;

    private TheUiHolder() {
        fSelectionHoldingTimer.setRepeats(false);

        final Component commandBarComponent = new JPanel_CommandBar(fCommander, fPortFilterPredicate.getHitTotalComponent());

        jFrame_Main = new JFrame_Main
                (fCommander
                        , fTableModel_Port
                        , commandBarComponent
                );

        // listener
        final ListSelectionModel tableLsm = fTableModel_Port.getJTable().getSelectionModel();
        tableLsm.addListSelectionListener(this);
    }

    /**
     * Shows the main GUI but with the Ports table not accepting Port selection clicks.
     */
    public void init() {
        goDark();
        jFrame_Main.setVisible(true);
        TheApplication.INSTANCE.causeReset(); // fills in table
    }

    /**
     * GUI now inactive to selections.
     */
    public void goDark() {
        fTableModel_Port.getJTable().setEnabled(false);
    }

    /**
     * GUI now active for selections.
     */
    public void goLive() {
        fTableModel_Port.getJTable().setEnabled(true);
    }

    public Commander getCommander() {
        return fCommander;
    }

    public Window getMainFrame() {
        return jFrame_Main;
    }

    public PortFilterPredicates getPortFilterPredicate() {
        return fPortFilterPredicate;
    }

    /**
     * @return a non-null value based on table's lead selection, NONE if empty
     */
    synchronized Portable getSelectedPort() {
        final Portable port = fTableModel_Port.getSingleSelection();
        return (port != null) ? port : Portable.NONE;
    }

    /**
     * @param port can be 'null' or Portable.NONE for clear selection
     */
    @Deprecated
    synchronized private void setSelectedPort(final Portable port) {
        fTableModel_Port.setSelection((port != Portable.NONE) ? port : null, true);
        fCommander.selectPort(port);
    }

    /**
     * BUG: when row sorter conformed and table resync'd, seems to cause column pixel widths to get defaulted.
     */
    void setTableSortByMark() {
        fTableModel_Port.setTableSortByMark();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == fSelectionHoldingTimer) {
            final Portable selectedPort = TheUiHolder.INSTANCE.getSelectedPort();
            TheUiHolder.INSTANCE.getCommander().selectPort(selectedPort);
        }
    }

    /**
     * User changed table selection, wait to see if it holds.
     * Profiler proved that table port selection was too slow when just
     * reacting to Port selection changes as several CLI were being running
     * and the Dependants graph was being determined at EDT time so
     * a Timer has been introduced to defer the work.
     *
     * @param e table selection to be translated into CRUD notification
     */
    @Override
    public void valueChanged(final ListSelectionEvent e) {   // start over previously pending selection change as mouse is still moving or arrow / page keys may be down
        fSelectionHoldingTimer.restart();
    }

    /**
     * Constructing UI in a separate thread.
     *
     * @return 'true' if the UI is completely constructed
     */
    static public boolean isReady() {
        return INSTANCE != null && INSTANCE.jFrame_Main.isVisible();
    }

    /**
     * Clear status selector AND histogram totals AND close all details windows.
     */
    static public void causeReset() {
        RESET_FILTER_NOTIFIER.causeReset();
    }

    /**
     * Subscribe to Filter reset notifications.
     *
     * @return just the listener .add() & .remove() aspect of the interface
     */
    static public Notifiable<Resetable> getResetNotifier() {
        return RESET_FILTER_NOTIFIER;
    }
}
