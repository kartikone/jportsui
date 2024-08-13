package oz.zomg.jport.gui.panel;

import oz.zomg.jport.PortConstants;
import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.Elemental;
import oz.zomg.jport.common.Elemental.EElemental;
import oz.zomg.jport.common.HttpUtil;
import oz.zomg.jport.common.gui.GuiUtil;
import oz.zomg.jport.gui.Commander;
import oz.zomg.jport.gui.TheUiHolder;
import oz.zomg.jport.gui.window.JDialog_AboutApp;
import oz.zomg.jport.ports.PortsCliUtil;
import oz.zomg.jport.type.Portable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Application wide, user driven, main command functions.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JPanel_CommandBar extends JPanel
        implements
        ActionListener
        , Elemental.Listenable<Portable> {
    static {
    }

    final private Commander fCommander;

    // ignored on Mac-PLAF are .setBackground() and .setContentAreaFilled()
    final private AbstractButton
            ab_Sync = new JButton("\u21BB Sync") // unicode clockwise arrow
            , ab_MarkOutdated = new JButton("Mark \u2192 All Upgrades") // unicode right arrow
            , ab_ApplyMarks = new JButton("\u221A Apply...") // unicode square root
            , ab_MoreCommand = new JButton("More \u25BC") // unicode downward triangle, can not be HTML or will wreck BoxLayout
            ;
    final private JMenuItem
            jItem_PortDetail = new JMenuItem("Details..."), jItem_ResetMark = new JMenuItem("Reset Marks"), jItem_ResetFilter = new JMenuItem("Reset Filters"), jItem_ResetCache = new JMenuItem("<HTML><SMALL>Reset Logo Cache"), jItem_MarkInactive = new JMenuItem("<HTML><SMALL>Mark \u2192 All Inactive"), jItem_CleanInstalled = new JMenuItem("<HTML><SMALL>Clean Ports..."), jItem_UpgradeCli = new JMenuItem("Update MacPorts..."), jItem_AppUpdate = new JMenuItem("<HTML><SMALL>Check for Update..."), jItem_AppAbout = new JMenuItem("About " + PortConstants.APP_NAME + "...");
    final private JPopupMenu jPop_MoreCmd = new JPopupMenu();

    /**
     * @param commander
     * @param hitTotalComponent created elsewhere for updating
     */
    public JPanel_CommandBar(final Commander commander, final Component hitTotalComponent) {
        fCommander = commander;

        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS)); // btw- will not compress in FlowLayout -> .createHorizontalStrut( 2560 )
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // T L B R

        final String AB_SYNC_TIP = """
                <HTML>Refresh all loaded Ports information.<HR>\
                <I>Performs a sync operation only on the ports <BR>\
                tree of a MacPorts installation, pulling <BR>\
                in the latest revision available of the Portfiles <BR>\
                from the MacPorts rsync server.\
                """;
        ab_Sync.setToolTipText(AB_SYNC_TIP);
        ab_MarkOutdated.setToolTipText("Marks all outdated Ports for upgrading");
        ab_ApplyMarks.setToolTipText("Applies marked Port status change requests");
        ab_MoreCommand.setToolTipText("Show other commands");
        jItem_PortDetail.setToolTipText("Show Port details in a separate window");
        jItem_ResetMark.setToolTipText("Remove all Port status change request marks");
        jItem_ResetFilter.setToolTipText("Show all Ports without any filtering");
        jItem_ResetCache.setToolTipText("Remove JPortUI project logos from your computer");
        jItem_MarkInactive.setToolTipText("Marks all inactive Ports for removal");
        jItem_CleanInstalled.setToolTipText("<HTML>Clean installed Ports of any working,<BR>distribution, and log files");
        jItem_UpgradeCli.setToolTipText("Have MacPorts self-update its CLI tools");
        jItem_AppUpdate.setToolTipText("Browse to " + PortConstants.PROJ_HOSTING);
        jItem_AppAbout.setToolTipText("Credits");

        ab_Sync.setEnabled(PortsCliUtil.HAS_PORT_CLI); // only if ports bin file exists
        ab_ApplyMarks.setEnabled(false);
        jItem_PortDetail.setEnabled(false);
        jItem_UpgradeCli.setEnabled(PortsCliUtil.HAS_PORT_CLI); // only if ports bin file exists
        // leaving ab_MarkOutdated enabled because if any Ports are outdated, then we wont know until later in the Notifier Elemental from CLI recon

        // required for text field to gain focus at startup
        ab_Sync.setFocusable(false);
        ab_MarkOutdated.setFocusable(false);
        ab_ApplyMarks.setFocusable(false);
        ab_MoreCommand.setFocusable(false);

        JPanel searchPanel = new JPanel_Search(commander);
        searchPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, 1, 0)); // needed so that text box doesn't over-expand

        // assemble
        jPop_MoreCmd.add(jItem_PortDetail);
        jPop_MoreCmd.addSeparator();
        jPop_MoreCmd.add(jItem_ResetMark);
        jPop_MoreCmd.add(jItem_ResetFilter);
        jPop_MoreCmd.add(jItem_ResetCache);
        jPop_MoreCmd.addSeparator();
        jPop_MoreCmd.add(jItem_MarkInactive);
        jPop_MoreCmd.add(jItem_CleanInstalled);
        jPop_MoreCmd.add(jItem_UpgradeCli);
        jPop_MoreCmd.addSeparator();
        jPop_MoreCmd.add(jItem_AppUpdate);
        jPop_MoreCmd.add(jItem_AppAbout);

        this.add(ab_Sync);
        this.add(ab_MarkOutdated);
        this.add(ab_ApplyMarks);
        this.add(Box.createHorizontalStrut(20));
        this.add(ab_MoreCommand);
        this.add(Box.createHorizontalGlue());
        this.add(hitTotalComponent); // centered up
        this.add(Box.createHorizontalGlue());
        this.add(searchPanel);

        // listeners
        TheApplication.INSTANCE.getCrudNotifier().addListenerWeakly(this);

        // downside to this type of List Comprehension is that IDE's "Find Usages" is unavailable
        for (final AbstractButton ab : GuiUtil.getChildren(AbstractButton.class, jPop_MoreCmd, this)) {   // popup menu items and panel's other ABs
            ab.addActionListener(this);
        }
//        ab_Sync.addActionListener( this );
//        ab_MarkOutdated.addActionListener( this );
//        ab_Apply.addActionListener( this );
//        ab_Reset.addActionListener( this );
//        ab_Details.addActionListener( this );
//        ab_ClearSearch.addActionListener( this );
//        jItem_Detail.addActionListener( this );
//        jItem_ResetAll   .addActionListener( this );
//        jItem_ResetFilter.addActionListener( this );
//        jItem_ResetMark .addActionListener( this );
//        jItem_Upgrade   .addActionListener( this );
//        jItem_About   .addActionListener( this );
    }

    @Override
    public void notify(final EElemental elemental, final Portable port) {
        switch (elemental) {
            case RETRIEVED:
                jItem_PortDetail.setEnabled(port != Portable.NONE);
                break;

            case UPDATED:
                final boolean isMarked = TheApplication.INSTANCE.getPortsMarker().getMarkCount() > 0;
                ab_ApplyMarks.setEnabled(isMarked);
                break;
        }
    }

    /**
     * Needs more Enums.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        final Object obj = e.getSource();
        if (obj instanceof AbstractButton ab) {
            if (ab == ab_Sync) {
                fCommander.syncPorts();
            } else if (ab == ab_MarkOutdated) {
                fCommander.markOutdatedPorts();
            } else if (ab == ab_ApplyMarks) {
                fCommander.confirmApplyMarks();
            } else if (ab == ab_MoreCommand) {
                jPop_MoreCmd.show(ab_MoreCommand, 0, ab_MoreCommand.getHeight());
            } else if (ab == jItem_MarkInactive) {
                fCommander.markInactivePorts();
            } else if (ab == jItem_PortDetail) {
                fCommander.openSelectionDetails();
            } else if (ab == jItem_ResetMark) {
                fCommander.clearAllMarks();
            } else if (ab == jItem_ResetFilter) {
                TheUiHolder.causeReset();
            } else if (ab == jItem_ResetCache) {
                HttpUtil.clearCache();
            } else if (ab == jItem_CleanInstalled) {
                fCommander.cleanInstalled();
            } else if (ab == jItem_UpgradeCli) {
                fCommander.updateMacPortsItself();
            } else if (ab == jItem_AppUpdate) {
                HttpUtil.browseTo(PortConstants.PROJ_HOSTING);
            } else if (ab == jItem_AppAbout) {
                new JDialog_AboutApp().setVisible(true);
            }
        }
    }

    /**
     * Not needed as instance listens to CRUD weakly.
     */
    @Override
    public void removeNotify() {
        TheApplication.INSTANCE.getCrudNotifier().removeListener(this);
    }
}
