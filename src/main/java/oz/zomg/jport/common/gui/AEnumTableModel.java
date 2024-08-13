package oz.zomg.jport.common.gui;

import oz.zomg.jport.common.Interfacing_.Equatable;
import oz.zomg.jport.common.Providers_.*;
import oz.zomg.jport.common.SearchTerm2;
import oz.zomg.jport.common.Util;
import oz.zomg.jport.common.gui.CellRenderers_.CellRenderer_AlternatingBar;
import oz.zomg.jport.common.gui.CellRenderers_.CellRenderer_Color;
import oz.zomg.jport.common.gui.CellRenderers_.CellRenderer_ForeBackColor;
import oz.zomg.jport.common.gui.JScrollPaneFactory_.EHorizontalScroller;
import oz.zomg.jport.common.gui.JScrollPaneFactory_.EVerticalScroller;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;


/**
 * A Row driven table model.  Columns can carry some static information
 * (Header name, pixel width, CellRender class) via variable argument Enums.
 * Normally all the JTable cells are non-editable to the user.  To change this
 * behavior construct with EEditable.ENABLE and have your column Enum implement
 * Providers.EnabledProvidable to return a 'true' value.
 * <p>
 * Extending AbstractTableModel to gain its Listener event firing methods.
 *
 * @param <C> column of same Enum class type
 * @param <R> row of class type will be inferred
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
abstract public class AEnumTableModel<R, C extends Enum<C>> extends AbstractTableModel
        implements
        RowProvidable<R>
        , TableModel {
    /**
     * Allows the table to have editing mode.
     * Use Providers.EnabledProvidable in the EColumn to signal whether a column is editable.
     * EEditable can override this by being set to EEditable.DISABLE.
     * If editable per cell only, then override the .isCellEditable(...) method in your derived Table.
     */
    public enum EEditable {ENABLE, DISABLE}

    /**
     * Show column header text Components.
     */
    public enum EShowHeader {ENABLE, DISABLE}

    /**
     * Show Horizontal / vertical grid lines.
     */
    public enum EShowGrid {ENABLE, DISABLE}

    /**
     * Feature only available with Java6+.
     */
    public enum EAutoSorter {ENABLE, DISABLE}

    /**
     * Via Row Object and ColorProvidable. ALT_BARs alternate ever X rows to another back color.
     */
    public enum EColorize {ENABLE, DISABLE, ALT_BAR_1, ALT_BAR_2, ALT_BAR_3}

    /**
     * Do -NOT- change ordering as .ordinal() used for first three with JTable.setSelectionMode().
     */
    public enum ERowSelection {
        SINGLE_SELECTION            // =0
        , SINGLE_INTERVAL_SELECTION   // =1
        , MULTIPLE_INTERVAL_SELECTION // =2
        , NO_SELECTION
    }

    /**
     * Create a wrapper JScrollPane or not and its behavior.
     */
    public enum EScrolling {
        VERTICALLY_ONLY               // Scrolls up-down.
        , HORIZONTALLY_ONLY             // Scrolls left-right.
        , VERTICALLY_AND_HORIZONTALLY   // Scrolls up-down and also scrolls left-right.
        /** No wrapper JScrollPane will be created for scrolling.  Side-effect is no JTableHeader will be visible until separately added to the Container. */
        , DISABLE
    }

    /**
     * Ignore setting preferred and max pixel size.
     */
    static final public Dimension NO_DIMENSION = new Dimension(-1, -1);

    static // initializer block
    {
    }

    final private Class<R> fRowOfClassType;
    final private boolean fIsAnyCellEditable;
    final private boolean fIsRowEquatable;
    final private boolean fIsRowComparable;

    final private Class<C> fColumnEnumType; // did not want to commit to making all derived classes specify <C>
    final private C[] fColumnEnums;
    final private Class<?>[] fColumnClasses; // needed for proper sorting and/or rendering

    final private JScrollPane jScrollPane; // provides a reference so the parent Container.add() as the JTable may be embedded in a JScrollPane
    final private JTable jTable; //... when possible, replace with SwingX sortable, filterable, highlightable JXTable

    /**
     * Row's Class type must implement the ContextualVisibilityProvidable<SearchTerm> interface method to use this.
     */
    private SearchTerm2<String> mFilteringSearchTerm = null;

    /**
     * Simplest constructor.
     *
     * @param rowOfClassType
     * @param columnEnums    non-'null'
     */
    public AEnumTableModel
    (final Class<R> rowOfClassType
            , final C... columnEnums
    ) {
        this
                (rowOfClassType
                        , null
                        , EEditable.DISABLE
                        , ERowSelection.NO_SELECTION
                        , EScrolling.VERTICALLY_ONLY
                        , EShowHeader.ENABLE
                        , EAutoSorter.ENABLE
                        , EShowGrid.ENABLE
                        , EColorize.DISABLE
                        , null
                        , columnEnums
                );
    }

    /**
     * Varg techniques may not be of much help here as this is sub-classed in several places.
     *
     * @param rowOfClassType
     * @param tableMaxPreferredDimension GuiFactory.DO_NOT_SET_DIMENSION or 'null' for none
     * @param anyCellEditable
     * @param rowSelectLogic
     * @param scrolling
     * @param columnHeaderShown          also not shown if EScrolling=DISABLE
     * @param autoSorter
     * @param grid
     * @param colorizeRow
     * @param font                       GuiFactory.DO_NOT_SET_FONT or 'null' for default
     * @param columnEnums                non-'null'
     */
    @SuppressWarnings("unchecked")
    public AEnumTableModel
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
        if (rowOfClassType == null) throw new NullPointerException();
        if (columnEnums.length == 0)
            throw new IllegalArgumentException("AT LEAST ONE COLUMN ENUM/NAME NEEDED FOR .getColumnCount()");

        // determine Column class type closest to Enum.class as it is possible to internally anonymously extend an enum and get the wrong answer!
        // For example <CODE>enum EExamp { E {toString()}, F, G{hashcode()} }</CODE> should not return <CODE>EExamp$1</CODE>
        fColumnEnumType = columnEnums[0].getDeclaringClass(); // gets the Enum's correct type even if it is an anonymous Enum like EExamp$1

        @SuppressWarnings("unchecked") final Class<C>[] columnClasses = new Class[columnEnums.length]; // nulls
        Arrays.fill(columnClasses, Object.class);

        // jTable needs some info for construction
        fColumnEnums = columnEnums;
        fColumnClasses = columnClasses;
        jTable = new JTable_Tip_(this, columnEnums); // one-to-one compositional relationship, auto registers itself as a TableModelListener

        // row colorization if <R> has the correct Provider
        final TableCellRenderer tableCellRenderer;
        switch (colorizeRow) {
            case ENABLE: {
                if (ForeBackColorProvidable.class.isAssignableFrom(rowOfClassType)) {
                    tableCellRenderer = new CellRenderer_ForeBackColor((RowProvidable<ForeBackColorProvidable>) this);
                } else if (ColorProvidable.class.isAssignableFrom(rowOfClassType)) {
                    tableCellRenderer = new CellRenderer_Color((RowProvidable<ColorProvidable>) this);
                } else {
                    tableCellRenderer = null;
                }
            }
            break;

            case ALT_BAR_1:
            case ALT_BAR_2:
            case ALT_BAR_3: {
                tableCellRenderer = new CellRenderer_AlternatingBar(colorizeRow, Color.BLACK, Color.WHITE);
            }
            break;

            default:
                tableCellRenderer = null;
                break;
        }

        // when we pick a Colorizer, the common column usage case is to cell render an Object.toString()
        if (tableCellRenderer != null) {
            jTable.setDefaultRenderer(Object.class, tableCellRenderer);
        }

        // conform table columns gui
        boolean isAnyColumnEditable = false; // double check that at least one column implements Providers.EnabledProvidable and is true
        for (int i = 0; i < columnEnums.length; i++) {
            final C columnEnum = columnEnums[i];

            if (columnEnum instanceof ClassProvidable providable) {
                final Class<?> columnClassType = providable.provideClass();
                fColumnClasses[i] = columnClassType;

                if (tableCellRenderer != null
                        && columnClassType != Boolean.class
                        && columnClassType != Integer.class
                        && columnClassType != Double.class
                        && columnClassType != Float.class
                        && columnClassType != Icon.class
                ) {   // special case the cell renderer
                    jTable.setDefaultRenderer(columnClassType, tableCellRenderer);
                }
            }
            // else leave as Object.class

            if (columnEnum instanceof WidthProvidable providable) {   // set column width
                final int pixelWidth = providable.provideWidth();
                if (pixelWidth != -1) // '-1' is flex-space
                {   // lock column size
                    final TableColumn column = jTable.getColumnModel().getColumn(i);
                    column.setMinWidth(pixelWidth);
                    column.setMaxWidth(pixelWidth);
                    column.setPreferredWidth(pixelWidth);
                }
                // else flex space
            }

            if (anyCellEditable != EEditable.DISABLE
                    && !isAnyColumnEditable
                    && columnEnum instanceof EnabledProvidable providable
                    && providable.provideIsEnabled()
            ) {   // found an editable column
                isAnyColumnEditable = true;
            }
        }

        // determined column Editicity
        fIsAnyCellEditable = isAnyColumnEditable;

        // examine Row class capabilities
        fRowOfClassType = rowOfClassType;
        fIsRowComparable = Comparable.class.isAssignableFrom(rowOfClassType);
        fIsRowEquatable = Equatable.class.isAssignableFrom(rowOfClassType);

        final boolean isTableRowSelectionEnabled = rowSelectLogic != ERowSelection.NO_SELECTION;

        // conform the JTable gui
        jTable.getTableHeader().setReorderingAllowed(false);
        jTable.setColumnSelectionAllowed(false);
        jTable.setRowSelectionAllowed(isTableRowSelectionEnabled);
        jTable.setShowGrid(grid == EShowGrid.ENABLE);
        jTable.setIntercellSpacing((grid == EShowGrid.ENABLE) ? new Dimension(2, 2) : new Dimension(0, 0));
//        jTable.setRowMargin( 4 ); gets set by above

        if (scrolling == EScrolling.DISABLE || scrolling == EScrolling.VERTICALLY_ONLY) {   // else HORIZONTAL_SCROLLBAR_AS_NEEDED would fail because the table horizontally fits the view port
            jTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        }

        if (columnHeaderShown != EShowHeader.ENABLE && scrolling != EScrolling.DISABLE) {   // remove header, if no JScrollPane, you would have to .add() anyway to show the getJTable.getJTableHeader(), however JTableHeader.setVisible(false) just gives a hole at top
            jTable.setTableHeader(null);
        }

        if (font != null) {   // not 'null'
            jTable.setFont(font);
        }

        if (isTableRowSelectionEnabled) {   // when SINGLE_SELECTION, you -can- use table.getSelectedRow() instead of table.getSelectionModel().isSelectedIndex( row ) and getMinSelectionIndex() & getMinSelectionIndex
            jTable.setSelectionMode(rowSelectLogic.ordinal());
        }

        if (scrolling == EScrolling.DISABLE) {   // no scrollpane
            jScrollPane = null;
            jTable.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, Color.LIGHT_GRAY)); // otherwise no upper-left boundary line
            jTable.getTableHeader().setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        } else {   // wrap with a scroll pane
            jScrollPane = JScrollPaneFactory_.create
                    (jTable
                            , (scrolling == EScrolling.VERTICALLY_AND_HORIZONTALLY || scrolling == EScrolling.VERTICALLY_ONLY) ? EVerticalScroller.AS_NEEDED : EVerticalScroller.NONE
                            , (scrolling == EScrolling.VERTICALLY_AND_HORIZONTALLY || scrolling == EScrolling.HORIZONTALLY_ONLY) ? EHorizontalScroller.AS_NEEDED : EHorizontalScroller.NONE
                            , JScrollPaneFactory_.EUsingInternalBorder.NONE // internally jScrollPane.setBorder( null )
                            , JScrollPaneFactory_.SCROLL_INCREMENT_PIXEL
                    );
        }

        if (tableMaxPreferredDimension != null && !tableMaxPreferredDimension.equals(NO_DIMENSION)) {   // not 'null' or -1
            final Component parentComponent = (scrolling == EScrolling.DISABLE) ? jTable : jScrollPane;
            parentComponent.setMaximumSize(tableMaxPreferredDimension); // constrains size in any Layout except Grid
            parentComponent.setPreferredSize(tableMaxPreferredDimension);
        }

        //... table.setFillsViewportHeight( true );

        // auto-sorting calls some of the overrided methods that require an initialized 'kColumnClasses'
        final boolean isAutoSorter = autoSorter != EAutoSorter.DISABLE;
        jTable.setAutoCreateRowSorter(isAutoSorter); // will call derived class's .getRowCount() so check against null fields there, watch for nulls here in super.ctor
        if (isAutoSorter) {
            @SuppressWarnings("unchecked") final RowSorter<AEnumTableModel<R, C>> rowSorter = (RowSorter<AEnumTableModel<R, C>>) jTable.getRowSorter();
            rowSorter.toggleSortOrder(0); // this appears to be the correct call to pick the sort column

            // visibility filter
            if (rowSorter instanceof @SuppressWarnings("unchecked")DefaultRowSorter<AEnumTableModel<R, C>, Integer> defaultRowSorter) {

                if (ContextualVisibilityProvidable.class.isAssignableFrom(rowOfClassType)) {   // row Class type implements the ContextualVisibilityProvidable<SearchTerm> interface method
                    defaultRowSorter.setRowFilter(new ContextualRowFilter_TableModel());
                } else if (VisibilityProvidable.class.isAssignableFrom(rowOfClassType)) {   // row Class type implements the VisibilityProvidable method
                    defaultRowSorter.setRowFilter(new RowFilter_TableModel());
                }
            }
        }

        // listener
        if (JPopupMenuProvidable.class.isAssignableFrom(rowOfClassType)) {   // auto support right-click or control-click popup menu support for single selection.
            jTable.addMouseListener(new MouseAdapter() // anonymous class
            {
                @Override
                public void mouseClicked(final MouseEvent e) {   // e.isControlDown() is used to deselect selections
                    if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {   // mouse listener only added if row class supports JPopupMenuProvidable
                        final JTable table = getJTable();
                        final int row = table.rowAtPoint(e.getPoint()); // did test, view->model xlation not needed
                        if (row > -1) table.setRowSelectionInterval(row, row);

                        final R rowObject = getSingleSelection();
                        if (rowObject != null) {
                            final JPopupMenu jpm = ((JPopupMenuProvidable) rowObject).provideJPopupMenu();
                            if (jpm != null) {
                                jpm.show(table, e.getX(), e.getY());
                            }
                        }
                        // else in table but below rows, i.e. no-man's land
                    }
                }
            });
        }
    }

    /**
     * Used by derived class constructor to set other options.
     * Technically there could be multiple tables with the same model but this is most rare.
     * To get just the header bar, use <CODE>.getJTable().getTableHeader()</CODE>
     *
     * @return The JTable associated with this TableModel
     */
    public JTable getJTable() {
        return jTable;
    }

    /**
     * To get just the header bar, use <CODE>.getJTable().getTableHeader()</CODE>
     *
     * @return .add() this table embedded JScrollPane to the parent container.  Always 'null' when object constructed with EScrolling.DISABLE.
     */
    public JScrollPane getJScrollPane() {
        return jScrollPane;
    }

    /**
     * @return a non-scrolling page axis Boxed panel which wraps both the table header and table content together
     */
    public JPanel createNonScrollingJPanel() {
        final JPanel jp = new JPanel();
        jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS)); // adding two items
        jp.add(jTable.getTableHeader());
        jp.add(jTable);

        // provide a common border
        jp.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

        // remove previous border from constructor
        jTable.setBorder(null);
        jTable.getTableHeader().setBorder(null);
        return jp;
    }

    /**
     * A column of a table has changed.  For example, the user changed a global time format.
     *
     * @param columnIndex
     */
    final private void fireColumnChanged(final int columnIndex) {
        final int rowCount = getRowCount();
        if (rowCount > 0) {
            final TableModelEvent tme = new TableModelEvent
                    (this
                            , 0
                            , rowCount - 1
                            , columnIndex
                            , TableModelEvent.UPDATE
                    );
            fireTableChanged(tme);
        }
    }

    /**
     * A column of a table has changed.  For example, the user changed a global time format.
     *
     * @param columnEnum
     */
    final public void fireColumnChanged(final C columnEnum) {
        final int index = Util.indexOfIdentity(columnEnum, fColumnEnums);
        fireColumnChanged(index);
    }

    /**
     * Choose the primary sort column based on index.
     * 'final' because may be called from derived class constructor.
     *
     * @param columnIndex Prompt the auto-sort to sort by the Column.
     */
    final private void toggelSortColumn(final int columnIndex) {
        if (columnIndex != Util.INVALID_INDEX) {
            final RowSorter rowSorter = jTable.getRowSorter();
            if (rowSorter != null) {   // avoid exception
                rowSorter.toggleSortOrder(columnIndex);
            }
            // else no row sorter available
        }
    }

    /**
     * Choose the primary sort column based on Enum.
     * 'final' because may be called from derived class constructor.
     * Call at Swing time only.
     *
     * @param columnEnum the column to sort the table with
     */
    final public void toggelSortColumn(final C columnEnum) {
        // Can not use .ordinal() as the ATableModel_ may have been created with a subset of an Enum or mixed Enums.
        // So no <CODE>if( columnEnum == kColumnEnums[ columnEnum.ordinal() ] ) { toggelSortColumn( columnEnum.ordinal() ); } </CODE>
        final int index = Util.indexOfIdentity(columnEnum, fColumnEnums);
        toggelSortColumn(index); // invalid index handled by 'int' signature method
    }

    /**
     * Useful in switch() statements usually seen in .getValueAt().
     * This method is more efficient than the array copy of <CODE>Enum.values()</CODE>.
     * One gains code simplicity over copy-pasting
     * <CODE>static final private EColumn[] _ENUMS=EColumn.values();</CODE>
     * at the cost of a dynamic cast.
     * <p>
     * Have not got <CODE>kColumnEnumType.cast(kColumnEnums[index])</CODE> working yet due to
     * not wanting to commit to making all derived classes specify column of class type <CODE><C></CODE>.
     *
     * @param enumClass
     * @param index
     * @return
     */
    private C getColumnEnum(final Class<C> enumClass, final int index) {
        return enumClass.cast(fColumnEnums[index]);
    }

    @Override
    final public int getColumnCount() {
        return fColumnEnums.length;
    }

    /**
     * Fixes displaying special cases like ImageIcon, GeoLocation, and Boolean-Checkbox columns.
     *
     * @param columnIndex
     * @return
     */
    @Override
    final public Class<?> getColumnClass(int columnIndex) {
        return fColumnClasses[columnIndex];
    }

    /**
     * Not called often.
     *
     * @param columnIndex
     * @return
     */
    @Override
    final public String getColumnName(final int columnIndex) {
        final C columnEnum = fColumnEnums[columnIndex];
        return (columnEnum instanceof DisplayTextProvidable dtp)
                ? dtp.provideDisplayText()
                : columnEnum.toString().replace('_', ' '); // space
    }

    /**
     * @param columnEnum
     * @return
     */
    private TableColumn getTableColumn(final C columnEnum) {
        final int columnIndex = Util.indexOfIdentity(columnEnum, fColumnEnums);
        return (columnIndex != Util.INVALID_INDEX)
                ? jTable.getColumnModel().getColumn(columnIndex)
                : null;
    }

    protected Class<R> getRowOfClassType() {
        return fRowOfClassType;
    }

    protected boolean isRowEquatable() {
        return fIsRowEquatable;
    }

    protected boolean isRowComparable() {
        return fIsRowComparable;
    }

    /**
     * Get array of multiple selected objects in the table.
     * Use when ERowSelection.SINGLE_INTERVAL_SELECTION or .MULTIPLE_INTERVAL_SELECTION.
     *
     * @return multiple row selections as an array, []{} is nothing selected
     */
    public R[] getSelection() {
        final int[] rows = jTable.getSelectedRows(); // empty [] if no table row selection
        final R[] selects = Util.createArray(fRowOfClassType, rows.length);

        if (rows.length > 0) {   // has to be xlated to get selection from sorted rows
            int i = 0;
            for (final int row : rows) {
                final int xlatedRow = jTable.convertRowIndexToModel(row);
                selects[i] = provideRow(xlatedRow);
                i += 1;
            }
        }
        // else no table row selection

        return selects;
    }

    /**
     * Get selected object in the table.
     * Use when ERowSelection.SINGLE_SELECTION
     * or when the last user selection is desired.
     *
     * @return row object lead selection, 'null' if none
     */
    public R getSingleSelection() {
        final ListSelectionModel lsm = jTable.getSelectionModel();
        if (!lsm.isSelectionEmpty()) {
            final int row = lsm.getLeadSelectionIndex(); // or use .getJTable().getSelectedRow() for single or lsm.isSelectedIndex() for multiple selection
            if (row != Util.INVALID_INDEX) {
                final int xlatedRow = jTable.convertRowIndexToModel(row); // has to be done with auto-sorter
                return provideRow(xlatedRow);
            }
        }

        return null;
    }

    /**
     * Clears the current table selection.
     */
    public void clearSelection() {
        final ListSelectionModel selectionModel = jTable.getSelectionModel();
        selectionModel.clearSelection();
    }

    /**
     * Selects a single element in the table, unless the element is not found, in which case the selection is cleared.
     *
     * @param toElement    'null' to clear selection
     * @param needScrollTo show the new selection
     */
    public void setSelection(final R toElement, final boolean needScrollTo) {
        if (toElement == null) {
            clearSelection();
        } else {   // find the element
            final ListSelectionModel selectionModel = jTable.getSelectionModel();
            final int count = getRowCount();
            for (int rowIndex = 0; rowIndex < count; rowIndex++) {
                final R row = provideRow(rowIndex);
                if (row.equals(toElement)) {
                    final int xlatedRowIndex = jTable.convertRowIndexToView(rowIndex);
                    selectionModel.setSelectionInterval(xlatedRowIndex, xlatedRowIndex); // for arbitrary multi use .clearSelection() & .addSelectionInterval()

                    if (needScrollTo) {
                        jTable.scrollRectToVisible(jTable.getCellRect(xlatedRowIndex, 0, true));
                    }
                    break; // for
                }
            }
        }
    }

    /**
     * Filters out Rows that do not match the SearchTerm.
     * Row's Class type must implement the ContextualVisibilityProvidable<SearchTerm> interface method to use this.
     *
     * @param searchTermsString 'null' or "" for no filtering
     */
    public void applySearchTerms(final String searchTermsString) {
        final SearchTerm2<String> searchTerm = new SearchTerm2<String>(searchTermsString, true);
        if (!searchTerm.isEmptyTerm()) {
            mFilteringSearchTerm = searchTerm;
            fireTableDataChanged(); // must call the row filter mechanism, so .repaint() is not good enough, i.e. this.getJTable().repaint(); won't work
        } else {   // no filtering, but must restore all normally viewable table rows
            mFilteringSearchTerm = null;
            fireTableDataChanged();
        }
    }

    /**
     * Clearly we should be using delegates.
     *
     * @return hit count where '0' is no hits
     */
    private int getSearchHitCount() {
        return (mFilteringSearchTerm != null) ? mFilteringSearchTerm.getHitCount() : 0;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (fIsAnyCellEditable && columnIndex < fColumnEnums.length) {   // at least one of the columns implements EnabledProvidable
            final C columnEnum = fColumnEnums[columnIndex];
            return (columnEnum instanceof EnabledProvidable ep && ep.provideIsEnabled());
        } else {   // AbstractTableModel returns the same
            return false;
        }
    }

    /**
     * //... Need to verify if these goofy argument conventions of model and view is correct with table row/column.
     *
     * @param modelRowIndex
     * @param viewColumnIndex
     * @return an Object that conforms to the Column class expectations, i.e. Object/String, Boolean, Integer, Double, ImageIcon
     */
    @Override
    final public Object getValueAt(final int modelRowIndex, final int viewColumnIndex) {
        final R row = provideRow(modelRowIndex);
        final C column = getColumnEnum(fColumnEnumType, viewColumnIndex);
        return getValueOf(row, column);
    }

    @Override
    final public void setValueAt(final Object value, final int modelRowIndex, final int viewColumnIndex) {
        final R row = provideRow(modelRowIndex);
        final C columnEnum = getColumnEnum(fColumnEnumType, viewColumnIndex);
        setValueOf(value, row, columnEnum);
    }

    /**
     * Override if editable, otherwise leave as a NO-OP
     *
     * @param value      to update model with
     * @param row
     * @param columnEnum
     */
    public void setValueOf(final Object value, final R row, final C columnEnum) {
    }

    /**
     * @param row        xlated
     * @param columnEnum xlated
     * @return placed in table
     */
    abstract public Object getValueOf(final R row, final C columnEnum);


    // ================================================================================

    /**
     * Filter's .include() called if <R> was determined to implement VisibilityProvidable in ATableModel_ constructor.
     */
    private class RowFilter_TableModel extends RowFilter<RowProvidable<R>, Integer> {
        @Override
        public boolean include(final RowFilter.Entry entry) {
            final Integer modelRowIndex = (Integer) entry.getIdentifier(); // guaranteed to be in model order
            final R rowObject = provideRow(modelRowIndex);
            return ((VisibilityProvidable) rowObject).provideIsVisible();
        }
    }


    // ================================================================================

    /**
     * Filter's .include() called if <R> was determined to implement VisibilityProvidable in ATableModel_ constructor.
     */
    private class ContextualRowFilter_TableModel extends RowFilter<AEnumTableModel<R, ?>, Integer> {
        @SuppressWarnings("unchecked")
        @Override
        public boolean include(final RowFilter.Entry entry) {
            final SearchTerm2<String> searchTermContext = AEnumTableModel.this.mFilteringSearchTerm; // alias
            if (searchTermContext == null) return true;

            final Integer modelRowIndex = (Integer) entry.getIdentifier(); // guaranteed to be in model order
            final R rowObject = provideRow(modelRowIndex);
            return ((ContextualVisibilityProvidable<SearchTerm2<String>>) rowObject).provideIsVisible(searchTermContext);
        }
    }
}
