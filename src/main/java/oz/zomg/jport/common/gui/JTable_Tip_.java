package oz.zomg.jport.common.gui;

import oz.zomg.jport.common.Interfacing_.Unleakable;
import oz.zomg.jport.common.Providers_.RowProvidable;
import oz.zomg.jport.common.Providers_.TipProvidable;
import oz.zomg.jport.common.Util;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.event.MouseEvent;


/**
 * Part of the mechanism for extracting tool tips out of a JTable's row.
 * If table table model is Unleakable then that is handled on removed from Container.
 * <H3><I><FONT color="#770000">Subset of original source.</FONT></I></H3>
 *
 * @param <T> is inferred
 * @param <R> rows are of class type
 * @param <C> columns are from the same Enum definition
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JTable_Tip_<R, T extends TableModel & RowProvidable<R>, C extends Enum<C>> extends JTable {
    static // initializer block
    {
    }

    final private RowProvidable<R> fRowProvider;
    final private C[] fColumnEnums;

    public JTable_Tip_(final T tableModel, final C... columnEnums) {
        super(tableModel);

        fRowProvider = tableModel;
        fColumnEnums = columnEnums;
    }

    /**
     * Contextually examine the object under the current mouse Row.
     *
     * @param event
     * @return TipProvidable if the object has that behavior
     */
    @Override
    public String getToolTipText(final MouseEvent event) {
        final TableModel tableModel = this.getModel();
        if (tableModel instanceof RowProvidable<?>) // conforming .setModel to guaranteed this is not worth the hassle
        {
            final int mouseRowIndex = rowAtPoint(event.getPoint());
            if (mouseRowIndex != Util.INVALID_INDEX) {   // not in bottom blank area of table or header
                final int modelRowIndex = convertRowIndexToModel(mouseRowIndex);
                final R obj = fRowProvider.provideRow(modelRowIndex); // might be 'null'
                if (obj instanceof TipProvidable providable) // instanceof on 'null' is always false
                {
                    return providable.provideTipText(); // got it
                }
            }
        }

        return super.getToolTipText(); // fall-thru to JTable's base tooltip method
    }

    /**
     * Columns can provide header tooltip text.
     *
     * @return table header with tip text override
     */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(this.columnModel) // anonymous class
        {
            @Override
            public String getToolTipText(final MouseEvent e) {
                int viewIndex = columnModel.getColumnIndexAtX(e.getPoint().x);
                int modelIndex = columnModel.getColumn(viewIndex).getModelIndex();

                final C columnEnum = fColumnEnums[modelIndex];
                if (columnEnum instanceof TipProvidable providable) {
                    final String tipText = providable.provideTipText();
                    if (tipText != null && !tipText.isEmpty()) {
                        return "<HTML>Column shows " + tipText;
                    }
                }

                return null; // no tip
            }
        };
    }

    /**
     * Called by the parent container chain when this component is removed.
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        final TableModel tableModel = this.getModel();
        if (tableModel instanceof Unleakable unleakable) {
            unleakable.unleak();
        }
    }
}
