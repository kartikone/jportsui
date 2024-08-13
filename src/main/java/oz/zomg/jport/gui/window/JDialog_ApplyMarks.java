package oz.zomg.jport.gui.window;

import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.StringsUtil_;
import oz.zomg.jport.common.gui.FocusedButtonFactory;
import oz.zomg.jport.common.gui.JScrollPaneFactory_;
import oz.zomg.jport.common.gui.JScrollPaneFactory_.EScrollPolicy;
import oz.zomg.jport.gui.TheUiHolder;
import oz.zomg.jport.ports.PortsVariants;
import oz.zomg.jport.type.EPortMark;
import oz.zomg.jport.type.Portable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Set;


/**
 * Shows a summary of all mark apply actions.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JDialog_ApplyMarks extends JDialog
        implements ActionListener {
    static {
    }

    final private Map<EPortMark, ? extends Set<Portable>> fResolvedMap = TheApplication.INSTANCE.getPortsMarker().createInverseMultiMapping();

    final private AbstractButton
            ab_Simulate = new JCheckBox("Simulate"), ab_Cancel = new JButton("Cancel"), ab_Apply = FocusedButtonFactory.create("Apply", this.getTitle());

    public JDialog_ApplyMarks() {
        super
                (TheUiHolder.INSTANCE.getMainFrame() // stay on top
                        , "Apply marks to " + TheApplication.INSTANCE.getPortsMarker().getMarkCount() + " Ports"
                        , ModalityType.APPLICATION_MODAL
                );

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // need window's maximize widget -> this.setUndecorated( true );

        // SOUTH -- [x]Simulate (Cancel) and (Apply) buttons
        final JPanel southPanel = new JPanel(new BorderLayout(20, 10));
        final JPanel sc = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sc.add(ab_Cancel);
        sc.add(Box.createHorizontalStrut(20));
        sc.add(ab_Apply);
        southPanel.add(sc, BorderLayout.CENTER);
        southPanel.add(ab_Simulate, BorderLayout.WEST);
        southPanel.add(Box.createHorizontalStrut(ab_Simulate.getPreferredSize().width), BorderLayout.EAST);

        // center
        final PortsVariants portsVariants = TheApplication.INSTANCE.getPortsCatalog().getPortsVariants(); // alias
        final StringBuilder sb = new StringBuilder("<HTML>");

        for (final Map.Entry<EPortMark, ? extends Set<Portable>> entry : fResolvedMap.entrySet()) {
            final EPortMark portMark = entry.getKey(); // alias
            final Set<Portable> portSet = entry.getValue(); // alias

            if (!portSet.isEmpty()) {
                sb.append("<BR><BIG>").append(portMark.toString()).append("</BIG>");

                // show applied variants
                final String[] portVariantName = new String[portSet.size()];
                int i = 0;
                for (final Portable port : portSet) {
                    portVariantName[i] = portsVariants.getNameVariant(port);
                    i++;
                }

                sb.append(StringsUtil_.htmlTabularize(6, "<FONT color=gray>\u2192</FONT>", "", portVariantName)).append("<BR>");
            }
        }

        final JLabel jLabel = new JLabel(sb.toString());
        jLabel.setVerticalAlignment(JLabel.CENTER);
        jLabel.setOpaque(false);
        final JScrollPane jsp = JScrollPaneFactory_.create(jLabel, EScrollPolicy.VERT_ALWAYS__HORIZ_NONE);

        // assemble
        this.add(Box.createHorizontalStrut(600), BorderLayout.NORTH);
        this.add(Box.createVerticalStrut(300), BorderLayout.WEST);
        this.add(jsp, BorderLayout.CENTER);
        this.add(southPanel, BorderLayout.SOUTH);
        this.pack();
        this.setLocationRelativeTo(TheUiHolder.INSTANCE.getMainFrame());

        ab_Apply.requestFocusInWindow(); // post assemble

        // listeners
        ab_Cancel.addActionListener(this);
        ab_Apply.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object obj = e.getSource();
        if (obj == ab_Cancel) {
            this.dispose();
        } else if (obj == ab_Apply) {
            this.dispose();
            final boolean isSimulated = ab_Simulate.isSelected();
            TheUiHolder.INSTANCE.getCommander().applyMarksToPorts(isSimulated, fResolvedMap);
        }
    }
}
