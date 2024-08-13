package oz.zomg.jport.gui.panel;

import oz.zomg.jport.PortConstants;
import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.Elemental;
import oz.zomg.jport.common.Elemental.EElemental;
import oz.zomg.jport.common.HttpUtil;
import oz.zomg.jport.common.ImageUtil_;
import oz.zomg.jport.common.Interfacing_.Targetable;
import oz.zomg.jport.common.StringsUtil_;
import oz.zomg.jport.common.gui.GuiUtil;
import oz.zomg.jport.gui.component.AJLabel_PortInfo;
import oz.zomg.jport.type.EPortStatus;
import oz.zomg.jport.type.Portable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Shows general information about the Port in four HTML tabled JLabels.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JPanel_CommonInfo extends JPanel
        implements
        Elemental.Listenable<Portable>
        , ActionListener {
    static final private boolean _IS_SHOWING_VARIANT = true;
    static final private String FONT_GRAY = "<FONT color=gray>";
    static final private String FONT_OFF = "</FONT>";

    static {
    }

    final private boolean fIsAssignmentLocked;

    final private JLabel[] jLabels = new JLabel[4]; // nulls

    final private AbstractButton abHomepage = new JButton("web");

    /**
     * Mutable for .actionPerformed() and follows table selection via .notify().  Must begin with 'null'
     */
    transient private Portable mAssignedPort = null;

    /**
     * @param assignedPort Use Portable.NONE to signal dynamically driven by user's table selection.
     */
    public JPanel_CommonInfo(final Portable assignedPort) {
        super(new BorderLayout());

        if (assignedPort == null) throw new NullPointerException();

        fIsAssignmentLocked = assignedPort != Portable.NONE;

        this.setOpaque(false); // otherwise messes up Mac-PLAF tab pit darkening

        abHomepage.setFocusable(false);

        for (int i = 0; i < jLabels.length; i++) {   // init array, lambda expressions would be nicer
            final JLabel jLabel = new JLabel();
            jLabel.setBorder(BorderFactory.createEmptyBorder(GuiUtil.GAP_PIXEL, GuiUtil.GAP_PIXEL, GuiUtil.GAP_PIXEL, GuiUtil.GAP_PIXEL)); // T L B R
            jLabel.setVerticalAlignment(SwingConstants.TOP);
            jLabel.setHorizontalAlignment(SwingConstants.LEFT);
            jLabel.setOpaque(false); // otherwise messes up Mac-PLAF tab pit darkening

            jLabels[i] = jLabel;
        }

        jLabels[3].setVisible(_IS_SHOWING_VARIANT);

        // needed to keep button small
        final JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        centerPanel.setOpaque(false);
        centerPanel.add(abHomepage);

        // assemble
        this.add(jLabels[0], BorderLayout.NORTH);
        this.add(jLabels[1], BorderLayout.WEST);
        this.add(jLabels[2], BorderLayout.EAST);
        this.add(jLabels[3], BorderLayout.SOUTH);
        this.add(centerPanel, BorderLayout.CENTER); // fixes tooltip dead-zone

        // listener
        abHomepage.addActionListener(this);
        TheApplication.INSTANCE.getCrudNotifier().addListenerWeakly(this); // automatically calls .notify() and updates mAssignedPort conforming the view
    }

    /**
     * @param port
     * @return status and version
     */
    private String getStatusVersText(final Portable port) {
        if (!port.hasStatus(EPortStatus.INSTALLED)) {   // Uninstalled
            return FONT_GRAY + "Status" + FONT_OFF + " \t Uninstalled" + '\n'
                    + FONT_GRAY + "Latest Version" + FONT_OFF + " \t " + port.getLatestVersion() + '\n';
        } else {   // Installed
            final String obsolete = (!port.hasStatus(EPortStatus.OBSOLETE)) ? " " : "OBSOLETE ";

            final String installed = (port.hasStatus(EPortStatus.ACTIVE))
                    ? "<FONT color=blue>Installed & Active" + FONT_OFF
                    : "<FONT color=purple>Installed & Inactive" + FONT_OFF;

            final String installedVerRev = port.getVersionInstalled() + ("0".equals(port.getRevisionInstalled())
                    ? ""
                    : " <U>" + FONT_GRAY + port.getRevisionInstalled() + FONT_OFF + "</U>");
            final String latestVerRev = port.getLatestVersion() + ("0".equals(port.getLatestRevision())
                    ? ""
                    : " <SMALL><U>" + port.getLatestRevision() + "</U></SMALL>");

            final boolean isOutdated = port.hasStatus(EPortStatus.OUTDATED);
            return FONT_GRAY + "Status" + FONT_OFF + " \t " + obsolete + installed + '\n'
                    + ((!isOutdated)
                    ? FONT_GRAY + "Installed Version" + FONT_OFF + " \t " + installedVerRev + '\n'
                    : FONT_GRAY + "Installed Version" + FONT_OFF + " \t " + "<FONT color=red><B>OUTDATED</B><BR>" + installedVerRev + FONT_OFF + '\n')
                    + ((!isOutdated)
                    ? FONT_GRAY + "Latest Version" + FONT_OFF + " \t " + latestVerRev + '\n'
                    : FONT_GRAY + "Latest Version" + FONT_OFF + " \t <B>" + latestVerRev + "</B>" + '\n');
        }
    }

    /**
     * @param port
     * @return tabbed for HTML table data and new line for HTML table rows
     */
    private String getCaptionText(final Portable port) {
        final String mark = (port.isUnmarked())
                ? ""
                : "<FONT color=#444444>" + port.getMark() + "\u2192 " + FONT_OFF; // dark-gray, right arrow

        return " \t <BIG>" + mark + "<B>" + port.getName() + "</B></BIG>\n"
                + " \t <FONT size=+1><I>" + port.getShortDescription() + "</I>" + FONT_OFF + '\n';
    }

    /**
     * @param port
     * @return distribution and support
     */
    private String getDistroText(final Portable port) {
        final String categories = StringsUtil_.concatenate(", ", port.getCategories()); // always at least one
        final String licenses = (port.getLicenses().length == 0) ? "Unknown" : StringsUtil_.concatenate(", ", port.getLicenses());
        final String maintainers = (port.getMaintainers().length == 0) ? "Unknown" : StringsUtil_.concatenate(", ", port.getMaintainers());
        final String homepage = (port.getHomepage().isEmpty()) ? "Unknown" : port.getHomepage();

        return FONT_GRAY + "Homepage</PRE> \t <U>" + homepage + "</U>" + '\n'
                + FONT_GRAY + "Maintainer</PRE> \t " + maintainers + '\n'
                + FONT_GRAY + "License</PRE> \t " + licenses + '\n'
                + FONT_GRAY + "Category</PRE> \t " + categories + '\n';
    }

    /**
     * @param port
     * @return build variants
     */
    private String getVariantText(final Portable port) {
        final String[] variants = port.getVariants(); // alias

        if (variants.length == 0) return " \t \n"; // no variants

        String modVariants = " " + StringsUtil_.concatenate(", ", StringsUtil_.sort(false, port.getVariants())); // .replace() required special casing first char

        if (!port.hasStatus(EPortStatus.INSTALLED) || (variants.length == 1 && port.getVariantsInstalled().length == 0)) {   // port NOT installed or installed but the one variant is not active
            return FONT_GRAY + "Variants" + FONT_OFF + " \t " + modVariants + '\n';
        } else {   // IS installed
            if (variants.length == 1) {   // has only one variant and it must be installed because of the above check
                return FONT_GRAY + "Variants" + FONT_OFF + " \t <FONT color=green><B> <U>" + variants[0] + "</U></B>," + FONT_OFF + " \n";
            } else {   // has many variants
                for (final String installedVariants : port.getVariantsInstalled()) {   // hilite installed, the prefix space + postfix comma avoids the partial match bug, ex. "x11" vs "no_x11"
                    modVariants = modVariants.replace(" " + installedVariants + ',', "<FONT color=green><B> <U>" + installedVariants + "</U></B>," + FONT_OFF);
                }

                return FONT_GRAY + "Variants" + FONT_OFF + " \t " + modVariants + '\n';
            }
        }
    }

    private void setPort(final Portable port) {
        if (port == Portable.NONE) {
            for (final JLabel jLabel : jLabels) {   // no text or tip
                jLabel.setText(null);
                jLabel.setToolTipText(null);
            }

            jLabels[0].setText(AJLabel_PortInfo.SELECT_PORT_TEXT);
        } else {   // HTML tables
            jLabels[0].setText(StringsUtil_.toHtmlTable(false, getCaptionText(port)));
            jLabels[1].setText(StringsUtil_.toHtmlTable(false, getStatusVersText(port)));
            jLabels[2].setText(StringsUtil_.toHtmlTable(false, getDistroText(port)));
            jLabels[3].setText(StringsUtil_.toHtmlTable(false, getVariantText(port)));

            // same tips
            final String tip = (!port.getShortDescription().equals(port.getLongDescription()))
                    ? port.provideTipText() // must be different else clutter
                    : null;
            for (final JLabel jLabel : jLabels) {
                jLabel.setToolTipText(tip);
            }

            abHomepage.setToolTipText("<HTML>Browse to<BR>" + port.getHomepage());
        }

        final boolean hasHomepage = !port.getHomepage().isEmpty();
        abHomepage.setVisible(hasHomepage);
        abHomepage.setIcon(null);

        if (PortConstants.IS_SHOWING_FAVICON) {
            jLabels[0].setIcon(null);

            if (hasHomepage) {
                HttpUtil.retrieveLogoConcurrently
                        (port.getDomain()
                                , new Targetable<Image>() // anonymous class
                                {
                                    @Override
                                    public void target(final Image image) {   // ignore any lag behind selection
                                        if (image != null && port.equals(mAssignedPort)) {
                                            setFavicon(image);
                                        }
                                    }
                                }
                        );
            }
        }
    }

    /**
     * EDT safe as may change parent container layout.
     * Swing thread safe.
     *
     * @param image can be 'null'
     */
    private void setFavicon(final Image image) {
        if (SwingUtilities.isEventDispatchThread()) {   // Mac-PLAF not happy, changes to square button -> abHomepage.setIcon( new ImageIcon( image ) )
            final ImageIcon imageIcon = (image != null)
                    ? new ImageIcon(ImageUtil_.reduceImage(image, 96))
                    : null;
            jLabels[0].setIcon(imageIcon);
        } else {
            SwingUtilities.invokeLater(new Runnable() // anonymous class
            {
                @Override
                public void run() {
                    setFavicon(image);
                }
            });
        }
    }

    /**
     * Follows main table selection or initializes the locked port assignment.
     *
     * @param elemental action
     * @param port      of marking view
     */
    @Override
    public void notify(final EElemental elemental, final Portable port) {
        switch (elemental) {
            case RETRIEVED: {
                if (!fIsAssignmentLocked || mAssignedPort == null) {
                    mAssignedPort = port;
                    setPort(port);
                }
            }
            break;

            case UPDATED: {
                if (mAssignedPort == port) {   // filtered out non-related updates
                    setPort(port);
                    break; // Mark may have changed
                }
            }
            break;
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == abHomepage) {   // could have used CLI -> port gohome $NAME
            HttpUtil.browseTo(mAssignedPort.getHomepage());
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
