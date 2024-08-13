package oz.zomg.jport.gui;

import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.Reset.Resetable;
import oz.zomg.jport.gui.table.TableModel_Port;
import oz.zomg.jport.ports.PortsCliUtil;
import oz.zomg.jport.type.Portable;
import oz.zomg.jport.type.Portable.Predicatable;

import javax.swing.*;
import java.awt.*;

import static oz.zomg.jport.type.Portable.Predicatable.ANY;


/**
 * The status and text filters for the main Ports table.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class PortFilterPredicates {
    static {
    }

    final private TableModel_Port fTableModel_Ports;

    final private JLabel_HitTotal jLabel_HitTotal = new JLabel_HitTotal();

    // mutable
    private Predicatable
            mStatusFilterPredicate = Predicatable.ANY, mHistoFilterPredicate = Predicatable.ANY, mTextSearchPredicate = Predicatable.ANY;

    PortFilterPredicates(final TableModel_Port tmp) {
        fTableModel_Ports = tmp;
    }

    public Component getHitTotalComponent() {
        return jLabel_HitTotal;
    }

    /**
     * Port install/activation status as one of the three filters.
     *
     * @param predicate
     */
    public void setStatusFilter(final Predicatable predicate) {
        if (predicate != mStatusFilterPredicate) {
            mStatusFilterPredicate = (predicate != null) ? predicate : Predicatable.ANY;
            setPortTableRowFilter();
        }
        // else no change
    }

    /**
     * Histogram field search as one of the three filters.
     *
     * @param predicate
     */
    public void setHistoFilter(final Predicatable predicate) {
        if (predicate != mHistoFilterPredicate) {
            mHistoFilterPredicate = (predicate != null) ? predicate : Predicatable.ANY;
            setPortTableRowFilter();
        }
        // else no change
    }

    /**
     * Directed text search as one of the three filters.
     *
     * @param predicate
     */
    public void setTextSearch(final Predicatable predicate) {
        if (predicate != mTextSearchPredicate) {
            mTextSearchPredicate = (predicate != null) ? predicate : Predicatable.ANY;
            setPortTableRowFilter();
        }
        // else no change
    }

    /**
     * Possible combinations of the 3 filters is 8 (2^3)
     *
     * @return count of visible rows post-filter
     */
    @SuppressWarnings("unchecked")
    private int setPortTableRowFilter() {
        // could have converted to bitfields and used a switch-case
        final Predicatable predicate;
        if (mStatusFilterPredicate == ANY
                && mHistoFilterPredicate == ANY
                && mTextSearchPredicate == ANY
        ) {   // no filtering or searching needed
            predicate = ANY;
        } else if (mStatusFilterPredicate != ANY && mHistoFilterPredicate == ANY && mTextSearchPredicate == ANY) {   // singular
            predicate = mStatusFilterPredicate;
        } else if (mStatusFilterPredicate == ANY && mHistoFilterPredicate != ANY && mTextSearchPredicate == ANY) {   // singular
            predicate = mHistoFilterPredicate;
        } else if (mStatusFilterPredicate == ANY && mHistoFilterPredicate == ANY && mTextSearchPredicate != ANY) {   // singular
            predicate = mTextSearchPredicate;
        } else {   // three or two combined with slight performance hit for relevancy check
            predicate = new Predicatable() // anonymous class
            {
                @Override
                public boolean evaluate(Portable port) {
                    return (mStatusFilterPredicate == ANY || mStatusFilterPredicate.evaluate(port))
                            && (mHistoFilterPredicate == ANY || mHistoFilterPredicate.evaluate(port))
                            && (mTextSearchPredicate == ANY || mTextSearchPredicate.evaluate(port));
                }
            };
        }

        // apply filter to Java6 table row sorter
        final JTable jTable = fTableModel_Ports.getJTable();
        final RowSorter<TableModel_Port> rowSorter = (RowSorter<TableModel_Port>) jTable.getRowSorter();
        if (rowSorter instanceof DefaultRowSorter<TableModel_Port, Integer> defaultRowSorter) {
            final RowFilter<TableModel_Port, Integer> rowFilter = (predicate != ANY)
                    ? new PortsRowFilter_TableModel(fTableModel_Ports, predicate)
                    : null; // no filter or search to be done
            defaultRowSorter.setRowFilter(rowFilter); // does filter & sort immediately
            return jLabel_HitTotal.setHit(defaultRowSorter.getViewRowCount());
        } else {   // something is odd with this TableModel
            return jLabel_HitTotal.setHit(-1);
        }
    }


    // ================================================================================

    /**
     * Applies a potentially compound predicate.
     */
    static private class PortsRowFilter_TableModel extends RowFilter<TableModel_Port, Integer> {
        final TableModel_Port fTableModel_Ports;
        final private Portable.Predicatable fPredicate;

        private PortsRowFilter_TableModel
                (final TableModel_Port tableModel_Ports
                        , final Portable.Predicatable predicate
                ) {
            fTableModel_Ports = tableModel_Ports;
            fPredicate = predicate;
        }

        @Override
        public boolean include(final RowFilter.Entry<? extends TableModel_Port, ? extends Integer> entry) //ENHANCE ATableModel_
        {
            final Integer modelRowIndex = entry.getIdentifier(); // guaranteed to be in model order
            final Portable rowObject = fTableModel_Ports.provideRow(modelRowIndex);
            return fPredicate.evaluate(rowObject);
        }
    }


    // ================================================================================

    /**
     * Embedded in Command Bar.
     */
    @SuppressWarnings("serial")
    static class JLabel_HitTotal extends JLabel
            implements Resetable {
        JLabel_HitTotal() {
            this.setForeground(Color.GRAY);

            // listener
            TheApplication.INSTANCE.getResetNotifier().addListenerWeakly(this);
        }

        int setHit(final int hitTotal) {
            final String text = (hitTotal > 0)
                    ? "Showing " + hitTotal + " Port" + (hitTotal > 1 ? 's' : ' ')
                    : null;
            this.setText(text);

            return hitTotal;
        }

        @Override
        public void reset() {
            this.setText("Using MacPorts " + PortsCliUtil.PORT_CLI_VERSION);
        }
    }
}
