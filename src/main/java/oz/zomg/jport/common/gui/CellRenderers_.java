package oz.zomg.jport.common.gui;

import oz.zomg.jport.common.Providers_.ColorProvidable;
import oz.zomg.jport.common.Providers_.ForeBackColorProvidable;
import oz.zomg.jport.common.Providers_.RowProvidable;
import oz.zomg.jport.common.gui.AEnumTableModel.EColorize;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;


/**
 * Colors the entire row of cells in a JTable or JList.
 * <H3><I><FONT color="#770000">Subset of original source.</FONT></I></H3>
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class CellRenderers_ {
    /**
     * Two-on / two-off rows are more pastel of a color.
     */
    public enum EPastelEvenOdd {ENABLE, DISABLE}

    static final private DefaultTableCellRenderer _DEFAULT_RENDERER = new DefaultTableCellRenderer(); // extends a JLabel for .paint()

    static // initializer block
    {
    }

    private CellRenderers_() {
    }


    // ================================================================================

    /**
     * Provides a color via an index.  The index could be a JList index, JTable modelRowIndex,
     * columnIndex, or etc.  In some cases an Object whose Class implements Providers.ForeColorProvider
     * and .BackColorProvider will make more sense.
     */
    public interface ColorByNumbersInterface {
        Color getForeColor(final int rowIndex);

        Color getBackColor(final int rowIndex);
    }


    // ================================================================================

    /**
     * Paint the cell with Colors provided by the derived class implementation of ColorByNumbersInterface.
     */
    static abstract private class ATableCellRenderer_
            implements
            ColorByNumbersInterface
            , TableCellRenderer {
        final boolean fNeedEvenRowsColorAdjusted; // should special case when row is selected and not pastellate

        private ATableCellRenderer_(final EPastelEvenOdd pastelEvenOdd) {
            fNeedEvenRowsColorAdjusted = (pastelEvenOdd != EPastelEvenOdd.DISABLE);
        }

        @Override
        public Component getTableCellRendererComponent
                (final JTable table
                        , final Object value
                        , final boolean isSelected
                        , final boolean hasFocus
                        , final int tableRowIndex
                        , final int column
                ) {
            final Component renderer = _DEFAULT_RENDERER.getTableCellRendererComponent // must be 'row' or you lose user row selection
                    (table
                            , value
                            , isSelected
                            , hasFocus
                            , tableRowIndex
                            , column
                    );

            final int modelRowIndex = table.convertRowIndexToModel(tableRowIndex); // has to be done to get correct colors, tested good
            Color fore = this.getForeColor(modelRowIndex);
            Color back = this.getBackColor(modelRowIndex);

            if (isSelected) {   // reverse on selection
                final Color temp = fore;
                fore = back;
                back = temp;
            } else if (fNeedEvenRowsColorAdjusted) {   // special casing when row is selected as text can be hard to read
                back = _getPastelEvenOddColor(back, tableRowIndex);
            }

            renderer.setForeground(fore);
            renderer.setBackground(back);
            // already done by default -> renderer.setOpaque( true ) ;
            return renderer;
        }

        static private Color _getPastelEvenOddColor(final Color color, int tableRowIndex) {
            if ((tableRowIndex / 2) % 2 == 0) {   // even two-on/two-off rows are different
                final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null); // convert to HSB, inescapable float[3] allocation
                hsb[1] *= 0.5; // reduce color Saturation by 50% which increases pastel-ness
                return Color.getHSBColor(hsb[0], hsb[1], hsb[2]); // convert back from HSB (Hue-Saturation-Brightness)
            } else {   // not different
                return color;
            }
        }
    }


    // ================================================================================

    /**
     *
     */
    static public class CellRenderer_ColorByIndex extends ATableCellRenderer_
            implements ColorByNumbersInterface {
        final private ColorByNumbersInterface fColorByNumbersInterface;

        /**
         * Non-RowProvidable variant of the CellRenderer.  A pass-thru for ColorByNumbersInterface
         *
         * @param colorByNumbersInterface usually references an instance of some derived TableModel
         */
        public CellRenderer_ColorByIndex(final ColorByNumbersInterface colorByNumbersInterface) {
            this(colorByNumbersInterface, EPastelEvenOdd.DISABLE);
        }

        public CellRenderer_ColorByIndex(final ColorByNumbersInterface colorByNumbersInterface, final EPastelEvenOdd pastelEvenOdd) {
            super(pastelEvenOdd);
            fColorByNumbersInterface = colorByNumbersInterface;
        }

        @Override
        public Color getForeColor(final int index) {
            return fColorByNumbersInterface.getForeColor(index);
        }

        @Override
        public Color getBackColor(final int index) {
            return fColorByNumbersInterface.getBackColor(index);
        }
    }


    // ================================================================================

    /**
     * Private base class implies the use of RowProvidable to get an Object reference
     * to pass the Object's color.
     */
    static abstract private class ACellRenderer_Rowable_<R> extends ATableCellRenderer_
            implements RowProvidable<R> {
        final RowProvidable<R> fRowProvider;

        private ACellRenderer_Rowable_(final RowProvidable<R> rowProvider) {
            this(rowProvider, EPastelEvenOdd.DISABLE);
        }

        private ACellRenderer_Rowable_(final RowProvidable<R> rowProvider, final EPastelEvenOdd pastelEvenOdd) {
            super(pastelEvenOdd);
            fRowProvider = rowProvider;
        }

        @Override
        public R provideRow(final int index) {
            return fRowProvider.provideRow(index);
        }
    }


    // ================================================================================

    /**
     * Driven by
     *
     * @see ForeBackColorProvidable
     */
    static public class CellRenderer_ForeBackColor extends ACellRenderer_Rowable_<ForeBackColorProvidable>
            implements ColorByNumbersInterface {
        public CellRenderer_ForeBackColor(final RowProvidable<ForeBackColorProvidable> rowProvider) {
            super(rowProvider);
        }

        @Override
        public Color getForeColor(int index) {
            return provideRow(index).provideForeColor();
        }

        @Override
        public Color getBackColor(int index) {
            return provideRow(index).provideBackColor();
        }
    }


    // ================================================================================

    /**
     * Driven by
     *
     * @see ColorProvidable
     */
    static public class CellRenderer_Color extends ACellRenderer_Rowable_<ColorProvidable>
            implements ColorByNumbersInterface {
        final private boolean fIsColorForeground; //else use as a background color

        public CellRenderer_Color(final RowProvidable<ColorProvidable> rowProvider) {
            this
                    (rowProvider
                            , EPastelEvenOdd.DISABLE
                            , true
                    );
        }

        public CellRenderer_Color
                (final RowProvidable<ColorProvidable> rowProvider
                        , final EPastelEvenOdd pastelEvenOdd
                        , final boolean isColorForeground
                ) {
            super(rowProvider, pastelEvenOdd);
            fIsColorForeground = isColorForeground;
        }

        @Override
        public Color getForeColor(int index) {
            return (fIsColorForeground) ? provideRow(index).provideColor() : null;
        }

        @Override
        public Color getBackColor(int index) {
            return (fIsColorForeground) ? null : provideRow(index).provideColor();
        }
    }


    // ================================================================================

    /**
     *
     */
    static public class CellRenderer_AlternatingBar
            implements TableCellRenderer {
        final private int fAlternateEvery;

        final private Color fDisabledForeColor;
        final private Color fForeColor;
        final private Color fBackColor0;
        final private Color fBackColor1;

        public CellRenderer_AlternatingBar
                (final EColorize colorizeRow
                        , final Color foreColor
                        , final Color backColor
                ) {
            this(foreColor, 0.06F, backColor, colorizeRow.ordinal() - 1); // 6%
        }


        /**
         * @param foreColor
         * @param weight         how much to shift to the fore color in RGB, ex. 0.1F
         * @param backColor
         * @param alternateEvery how many bars before adjusting the back color, ex. 1, 2
         */
        public CellRenderer_AlternatingBar
        (final Color foreColor
                , final float weight
                , final Color backColor
                , final int alternateEvery
        ) {
            fForeColor = foreColor;
            fBackColor0 = backColor;
            fBackColor1 = new Color
                    (Math.min(255, (int) ((foreColor.getRed() - backColor.getRed()) * weight) + backColor.getRed())
                            , Math.min(255, (int) ((foreColor.getGreen() - backColor.getGreen()) * weight) + backColor.getGreen())
                            , Math.min(255, (int) ((foreColor.getBlue() - backColor.getBlue()) * weight) + backColor.getBlue())
                    );
            fDisabledForeColor = new Color
                    ((foreColor.getRed() + backColor.getRed()) / 2
                            , (foreColor.getGreen() + backColor.getGreen()) / 2
                            , (foreColor.getBlue() + backColor.getBlue()) / 2
                    );

            fAlternateEvery = alternateEvery;
        }

        @Override
        public Component getTableCellRendererComponent
                (final JTable table
                        , final Object value
                        , final boolean isSelected
                        , final boolean hasFocus
                        , final int tableRowIndex
                        , final int column
                ) {
            final Component renderer = _DEFAULT_RENDERER.getTableCellRendererComponent // must be 'row' or you lose user row selection
                    (table
                            , value
                            , isSelected
                            , hasFocus
                            , tableRowIndex
                            , column
                    );

            Color fore = (table.isEnabled()) ? fForeColor : fDisabledForeColor;
            Color back = ((tableRowIndex / fAlternateEvery) % 2 == 0) // even two-on/two-off rows are different
                    ? fBackColor0
                    : fBackColor1;

            if (isSelected) {   // reverse on selection
                final Color temp = fore;
                fore = back;
                back = temp;
            }

            renderer.setForeground(fore);
            renderer.setBackground(back);
            // already done by default -> renderer.setOpaque( true ) ;
            return renderer;
        }
    }
}
