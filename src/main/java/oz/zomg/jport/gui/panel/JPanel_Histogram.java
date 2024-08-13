package oz.zomg.jport.gui.panel;

import oz.zomg.jport.common.Reset.Resetable;
import oz.zomg.jport.gui.HistogramUiFactory;
import oz.zomg.jport.gui.TheUiHolder;
import oz.zomg.jport.ports.PortsHistogramFactory.EHistogram;
import oz.zomg.jport.type.Portable.Predicatable;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Histogram table presentation similar to CardLayout except we do this by using .remove() and .add().
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JPanel_Histogram extends JPanel
        implements
        ActionListener
        , AncestorListener {
    static final boolean NEED_TO_BREAK_OUT_CATEGORIES = false;

    static {
    }

    final private JPanel jPanel_center = new JPanel(new GridLayout()); // for proper resize when frame changes
    final private JComboBox jCombo_Histo = new JComboBox(EHistogram.values());
    final private AbstractButton ab_Any = new JButton("Any");


    public JPanel_Histogram() {
        super(new BorderLayout(5, 5));

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // T L B R
        this.setOpaque(false); // needed for tab embed on Mac-PLAF

        jCombo_Histo.setToolTipText("Choose a different Histogram");

        ab_Any.setToolTipText("Clears selected histogram's filter");
        ab_Any.setEnabled(false);

        jCombo_Histo.setSelectedIndex(-1); // no selection
        if (NEED_TO_BREAK_OUT_CATEGORIES) jCombo_Histo.removeItem(EHistogram.Categories);

        this.add(jPanel_center, BorderLayout.CENTER);
        this.add(jCombo_Histo, BorderLayout.NORTH);
        this.add(ab_Any, BorderLayout.SOUTH);

        // listeners
        jCombo_Histo.addActionListener(this);
        jCombo_Histo.addAncestorListener(this); // FocusListener did not work

        TheUiHolder.getResetNotifier().addListener(new Resetable() // anonymous class
        {
            @Override
            public void reset() {
                ab_Any.doClick();
            }
        });
    }

    private void showHistogram(final EHistogram histogram) {
        ab_Any.setEnabled(false);
        TheUiHolder.INSTANCE.getPortFilterPredicate().setHistoFilter(Predicatable.ANY);

        // equivalent of a CardLayout except we do this by using .remove() and .add()
        jPanel_center.removeAll(); // discard previous view
        this.revalidate();

        final Component tableScroller = HistogramUiFactory.createComponent(ab_Any, histogram);
        jPanel_center.add(tableScroller);

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final Object obj = e.getSource();
        if (obj instanceof JComboBox<?> jcb) {
            final EHistogram selectedHisto = (EHistogram) jcb.getSelectedItem();
            showHistogram(selectedHisto);
        }
    }

    @Override
    public void ancestorRemoved(AncestorEvent e) {
    }

    @Override
    public void ancestorMoved(AncestorEvent e) {
    }

    @Override
    public void ancestorAdded(AncestorEvent e) {
        if (jCombo_Histo.getSelectedIndex() == -1) {
            jCombo_Histo.setSelectedIndex(0); // lazy build first item's histogram table when tab is shown
        }
    }
}
