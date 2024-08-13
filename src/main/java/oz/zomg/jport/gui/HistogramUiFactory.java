package oz.zomg.jport.gui;

import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.Histogram2;
import oz.zomg.jport.gui.table.TableModel_Histogram;
import oz.zomg.jport.ports.PortsHistogramFactory;
import oz.zomg.jport.ports.PortsHistogramFactory.EHistogram;
import oz.zomg.jport.type.Portable;
import oz.zomg.jport.type.Portable.Predicatable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;


/**
 * Create Histogram tables that filter the displayed Ports when a row is selected.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class HistogramUiFactory {
    /**
     * Not a JPanel because certain Components are needed elsewhere, ex. Categories table.
     */
    private HistogramUiFactory() {
    }

    /**
     * Formerly, Categories was a "top-level" tab.
     *
     * @return index [0]=scroll pane of histo, [1]=Any button
     */
    static private Component[] createCategoriesComponent() {
        final AbstractButton ab_Any = new JButton("Any");
        return new Component[]{createComponent(ab_Any, EHistogram.Categories), ab_Any};
    }

    /**
     * @param ab_Any    the "Any" button clears the table selection
     * @param histoEnum
     * @return JScrollPane containing a JTable of instances and their frequency
     */
    static public Component createComponent(final AbstractButton ab_Any, final EHistogram histoEnum) {
        final Histogram2<String> histogram = new Histogram2<String>(String.class);
        final Portable[] allPorts = TheApplication.INSTANCE.getPortsCatalog().getPortsInventory().getAllPorts();
        for (final Portable port : allPorts) {
            for (final String key : histoEnum.transform(port)) {
                histogram.increment(key);
            }
        }

        final Map.Entry<Integer, String>[] entries = (histoEnum == EHistogram.Categories)
                ? histogram.getFrequencyKeyEntries(3, Integer.MAX_VALUE) // each Category will need at least three Ports in it
                : histogram.getFrequencyKeyEntries();

        final TableModel_Histogram tmh = new TableModel_Histogram();
        tmh.setRows(entries);

        // listener
        final PrivateListener privateListener = new PrivateListener(tmh, histoEnum);
        ab_Any.addActionListener(privateListener); // leaks

        final ListSelectionModel lsm = tmh.getJTable().getSelectionModel();
        lsm.addListSelectionListener(privateListener); //ENHANCE ATableModel.this.addListSelectionListener(...)
        lsm.addListSelectionListener(new ListSelectionListener() // anonymous class
        {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {   // waits until selection stops changing via arrow keys or mouse stabilizes
                    ab_Any.setEnabled(!lsm.isSelectionEmpty());
                }
            }
        });

        return tmh.getJScrollPane();
    }


    // ================================================================================

    /**
     * handles Histogram instantiations table selections.
     */
    static private class PrivateListener
            implements
            ListSelectionListener
            , ActionListener {
        final private TableModel_Histogram fTableModel_Histogram;
        final private EHistogram fHistoEnum;

        private PrivateListener(final TableModel_Histogram tmh, final EHistogram histoEnum) {
            fTableModel_Histogram = tmh;
            fHistoEnum = histoEnum;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final Object obj = e.getSource();
            if (obj instanceof AbstractButton) {
                fTableModel_Histogram.clearSelection();
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {   // user stopped doing mouse drag changes
                final Predicatable predicate;

                final ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (!lsm.isSelectionEmpty()) {
                    final Map.Entry<?, ?> entry = fTableModel_Histogram.getSingleSelection();
                    final String search = entry.getValue().toString(); // context
                    predicate = PortsHistogramFactory.createPredicate(fHistoEnum, search);
                } else {   // no narrowing, wide open
                    predicate = Predicatable.ANY;
                }

                TheUiHolder.INSTANCE.getPortFilterPredicate().setHistoFilter(predicate);
            }
            // else user still dragging
        }
    }
}
