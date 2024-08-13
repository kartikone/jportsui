package oz.zomg.jport.common.gui;

import oz.zomg.jport.common.EmptyArrayFactory_;
import oz.zomg.jport.common.Interfacing_.Equatable;
import oz.zomg.jport.common.Interfacing_.Unleakable;

import javax.swing.*;
import java.awt.*;


/**
 * Facilitate the Pub/Sub Alterations to efficiently redraw the table by allowing the backing store to be external.
 *
 * @param <C> column of same Enum class type
 * @param <R> row of class type will be inferred
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
abstract public class AEnumTableModel_Array<R, C extends Enum<C>> extends AEnumTableModel<R, C>
        implements Unleakable {
    static // initializer block
    {
    }

    /**
     * Model on board.
     */
    private R[] mRows;

    /**
     * @param rowOfClassType  row is of class type R
     * @param anyCellEditable
     * @param rowSelectLogic
     * @param colorizeRow
     * @param columnEnums
     */
    public AEnumTableModel_Array
    (final Class<R> rowOfClassType
            , final EEditable anyCellEditable
            , final ERowSelection rowSelectLogic
            , final EColorize colorizeRow
            , final C... columnEnums
    ) {
        this
                (rowOfClassType
                        , null
                        , anyCellEditable
                        , rowSelectLogic
                        , EScrolling.VERTICALLY_ONLY
                        , EShowHeader.ENABLE
                        , EAutoSorter.ENABLE
                        , EShowGrid.DISABLE
                        , colorizeRow
                        , null
                        , columnEnums
                );
    }

    /**
     * @param rowOfClassType             row is of class type R
     * @param tableMaxPreferredDimension GuiFactory.DO_NOT_SET_DIMENSION or 'null' for none
     * @param anyCellEditable
     * @param rowSelectLogic
     * @param scrolling
     * @param columnHeaderShown
     * @param autoSorter
     * @param grid
     * @param colorizeRow
     * @param font                       use GuiFactory.DO_NOT_SET_FONT or 'null' for default
     * @param columnEnums
     */
    public AEnumTableModel_Array
    (final Class<R> rowOfClassType
            , final Dimension tableMaxPreferredDimension
            , final EEditable anyCellEditable
            , final ERowSelection rowSelectLogic
            , final EScrolling scrolling
            , final EShowHeader columnHeaderShown
            , final EAutoSorter autoSorter
            , final EShowGrid grid
            , final EColorize colorizeRow
            , final Font font
            , final C... columnEnums
    ) {
        super
                (rowOfClassType
                        , tableMaxPreferredDimension
                        , anyCellEditable
                        , rowSelectLogic
                        , scrolling
                        , columnHeaderShown
                        , autoSorter
                        , grid
                        , colorizeRow
                        , font
                        , columnEnums
                );

        mRows = EmptyArrayFactory_.get(rowOfClassType);
    }

    /**
     * Normally, simply use .provideRow()
     *
     * @return is never 'null' but may be an empty reified array []{}.  If using the [] for occasional content modification, you will need to call <CODE>this.fireTableDataChanged();</CODE>
     */
    public R[] getRowArray() {
        return mRows;
    }

    /**
     * Set model contents.
     * Handles firing table row alterations as it compares the differences.
     * 'final' because called from derived class constructor.
     * Swing thread safe.
     *
     * @param rows
     */
    @SuppressWarnings("unchecked")
    final public void setRows(final R[] rows) {
        if (rows == null) throw new NullPointerException();
        if (rows.length != 0 && rows == mRows)
            throw new IllegalArgumentException("Should not be the TableModel backing array.  Modifications are only firable by comparison against another array.");

        if (SwingUtilities.isEventDispatchThread()) {
            final R[] prevRows = mRows;
            mRows = rows; // keep RowSorter from IndexOutOfBoundsException when .fireTableRowsXXX() calls .getRowCount()

            if (rows.length == 0 || prevRows == null || prevRows.length == 0) {   // all filtered or all dropped on init, or startup case etc
                fireTableDataChanged();
            } else {   // compare against what is presently displayed
                final int minSize = Math.min(prevRows.length, rows.length);
                for (int i = 0; i < minSize; i++) {   // ignore equivalent/nonchanging elements
                    final R prevRow = prevRows[i];
                    final R row = rows[i];
                    if (!prevRow.equals(row) || (isRowEquatable() && !((Equatable<R>) prevRow).isEquivalent(row))) {   // some column data different
                        fireTableRowsUpdated(i, i); //... SLOOOOW! could be optimized to handle contiguous cases, ex. i thru j
                    }
                    // else no display change as elements are the same
                }

                if (prevRows.length != rows.length) {   // handle straggler rows
                    if (prevRows.length > rows.length) {   // removing as the prev list is longer
                        fireTableRowsDeleted(minSize, prevRows.length - 1);
                    } else {   // inserting as new list is longer
                        fireTableRowsInserted(minSize, rows.length - 1);
                    }
                }
            }
        } else {
            SwingUtilities.invokeLater(new Runnable() // anonymous class
            {
                @Override
                public void run() {
                    AEnumTableModel_Array.this.setRows(rows);
                }
            });
        }
    }

    @Override
    public int getRowCount() {   // check for null because ATabelModel_ BASE constructor provokes this call but has not established the value of mRows yet
        return (mRows != null) ? mRows.length : 0; // is 'null' when the base class constructor is doing JTable.super ops
    }

    /**
     * @param index is zero based
     * @return the element or null to the RowSorter class in certain base constructor cases
     */
    @Override
    public R provideRow(final int index) {
        return (mRows != null) ? mRows[index] : null;
    }

    /**
     * Blow out the array.
     */
    @Override
    public void unleak() {
        mRows = EmptyArrayFactory_.get(getRowOfClassType());
    }
}
