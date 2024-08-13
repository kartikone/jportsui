package oz.zomg.jport.gui.window;

import oz.zomg.jport.PortConstants;
import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.HttpUtil;
import oz.zomg.jport.common.Reset.Resetable;
import oz.zomg.jport.common.Util;
import oz.zomg.jport.common.gui.FocusedButtonFactory;
import oz.zomg.jport.gui.Commander;
import oz.zomg.jport.gui.TheUiHolder;
import oz.zomg.jport.gui.component.JProgress_Enum;
import oz.zomg.jport.gui.component.JTabPane_Detail;
import oz.zomg.jport.gui.component.JTabPane_Filter;
import oz.zomg.jport.gui.panel.JPanel_Mark;
import oz.zomg.jport.gui.table.TableModel_Port;
import oz.zomg.jport.type.EPortStatus;
import oz.zomg.jport.type.Portable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * The primary, top-level window for the UI.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JFrame_Main extends JFrame
        implements ChangeListener {
    static {
    }

    public JFrame_Main
            (final Commander commander
                    , final TableModel_Port portsTable
                    , final Component commandBarComponent
            ) {
        super(PortConstants.APP_NAME + "  -  " + PortConstants.getVersion());

        final JTabbedPane jTab_Filter = new JTabPane_Filter();
        final JTabbedPane jTab_Detail = new JTabPane_Detail();

        final JPanel markPortPanel = new JPanel_Mark();

        // JPanel_Mark has no need to manage an unrelated progress bar but it does have a BorderLayout.SOUTH available
        final JProgress_Enum<EPortStatus> jProgress_EchoPortStatus = new JProgress_Enum<EPortStatus>(false, EPortStatus.VALUES);
        jProgress_EchoPortStatus.setPreferredSize(new Dimension(80, 10)); // default width is 100+ pixels, too wide
        markPortPanel.add(jProgress_EchoPortStatus, BorderLayout.SOUTH); // sneak a progress bar in here

        final JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.add(jTab_Detail, BorderLayout.CENTER);
        detailPanel.add(markPortPanel, BorderLayout.EAST);

        final JSplitPane jSplit_inventory_detail = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, portsTable.getJScrollPane(), detailPanel);
        final JSplitPane jSplit_view_inv_detail = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, jTab_Filter, jSplit_inventory_detail);

        jSplit_view_inv_detail.setDividerLocation(232);
        jSplit_view_inv_detail.setBorder(null); // declutters
        jSplit_inventory_detail.setBorder(null);

        // north
        final Component northComponent = commandBarComponent; // alias

        // center
        final Component centerComponent;
        if (PortConstants.HAS_MAC_PORTS || Util.isOnWindows()) {   //... allows testing on Windows but should not always
            centerComponent = jSplit_view_inv_detail;
        } else {   // no MacPorts installed
            northComponent.setVisible(false); // hide Command Bar to reduce confusion

            final AbstractButton ab_BrowseMacPorts = FocusedButtonFactory.create
                    ("<HTML><BIG><CENTER>JPortsUI requires MacPorts to be installed.<BR>Click to browse the MacPorts installer page."
                            , "http://www.macports.org/install.php"
                    );
            ab_BrowseMacPorts.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
            ab_BrowseMacPorts.addActionListener(new ActionListener() // anonymous class
            {
                @Override
                public void actionPerformed(ActionEvent e) {
                    HttpUtil.browseTo("http://www.macports.org/install.php");
                }
            });
            centerComponent = ab_BrowseMacPorts;
        }

        // south
        final Component southComponent = Box.createHorizontalStrut(1200); //... helps .pack(), but should come from a Preference file

        // west
        final Component westComponent = Box.createHorizontalStrut(10);

        // east
        final Component eastComponent = Box.createVerticalStrut(768); //... helps .pack(), but should come from a Preference file

        // assemble
        this.add(centerComponent, BorderLayout.CENTER);
        this.add(northComponent, BorderLayout.NORTH);
        this.add(southComponent, BorderLayout.SOUTH);
        this.add(westComponent, BorderLayout.WEST);
        this.add(eastComponent, BorderLayout.EAST);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();

        // listener
        jTab_Filter.addChangeListener(this); // user changed tabs

        TheApplication.INSTANCE.getResetNotifier().addListener(new Resetable() // anonymous class
        {
            @Override
            public void reset() {   // note: closing all Port Detail dialogs is automatic as they register themselves
                jTab_Filter.setSelectedIndex(0);
                jTab_Detail.setSelectedIndex(0);
                portsTable.clearSelection();
//                        portsTable.setTableSortByName();

                // THUNK, can't set table rows until port snapshot parsing completes
                portsTable.setRows(PortConstants.NO_PORTS);
                final Portable[] begunHaveTheCloneWars = TheApplication.INSTANCE.getPortsCatalog().getPortsInventory().getAllPorts().clone();
                portsTable.setRows(begunHaveTheCloneWars);
                jSplit_inventory_detail.setDividerLocation(0.66F); // needed the rows first
            }
        });

        TheApplication.INSTANCE.getEchoStatusNotifier().addListenerWeakly(jProgress_EchoPortStatus);
    }

    /**
     * Reset filters like Synaptic does when top level tabs/views changed.
     *
     * @param e status filter tab changed
     */
    @Override
    public void stateChanged(final ChangeEvent e) {
        TheUiHolder.causeReset();
    }
}
