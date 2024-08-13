package oz.zomg.jport.gui.table;

import oz.zomg.jport.common.Providers_.ClassProvidable;
import oz.zomg.jport.common.Providers_.WidthProvidable;
import oz.zomg.jport.common.gui.AEnumTableModel_Array;

import java.awt.*;
import java.util.Map.Entry;


/**
 * Shows occurrence counts of various attributes.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class TableModel_Histogram extends AEnumTableModel_Array<Entry, TableModel_Histogram.EColumn> {
    enum EColumn implements WidthProvidable, ClassProvidable {
        NAME(-1) {
            @Override
            public Class provideClass() {
                return String.class;
            }
        }, TOTAL(64) {
            @Override
            public Class provideClass() {
                return Integer.class;
            }
        } // 64 is the smallest with Mac-PLAF
        ;

        EColumn(final int width) {
            fWidth = width;
        }

        private final int fWidth;

        @Override
        public int provideWidth() {
            return fWidth;
        }
    }

    static {
    }

    public TableModel_Histogram() {
        super
                (Entry.class
                        , EEditable.DISABLE
                        , ERowSelection.SINGLE_SELECTION
                        , EColorize.DISABLE
                        , EColumn.values()
                );

        getJTable().setGridColor(Color.LIGHT_GRAY);
        getJTable().setShowHorizontalLines(true);
        getJTable().setShowVerticalLines(false);
    }

    /**
     * @param entry      row
     * @param columnEnum
     * @return placed in table
     */
    @Override
    public Object getValueOf(final Entry entry, final EColumn columnEnum) {
        switch (columnEnum) {
            case NAME:
                return entry.getValue();
            case TOTAL:
                return entry.getKey();

            default:
                return "ERR";
        }
    }
}
