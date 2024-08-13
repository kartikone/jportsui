package oz.zomg.jport.common.gui;

import javax.swing.*;
import java.awt.*;


/**
 * <H3><I><FONT color="#770000">Subset of original source.</FONT></I></H3>
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class JScrollPaneFactory_ {
    static final public int SCROLL_INCREMENT_PIXEL = 40; // seemed like a good value
    static final public int SCROLL_INCREMENT_UNSPEC = -1; // let each Java PLAF choose

    /**
     * Draws a box when DEFAULT.
     */
    public enum EUsingInternalBorder {NONE, DEFAULT}

    /**
     * Up-and-down JScrollBar policy.
     */
    public enum EVerticalScroller {NONE, ALWAYS, AS_NEEDED}

    /**
     * Left-to-right JScrollBar policy.
     */
    public enum EHorizontalScroller {NONE, ALWAYS, AS_NEEDED}

    /**
     * Composites horizontal and vertical JScrollBar policies.
     */
    public enum EScrollPolicy {
        VERT_AS_NEEDED__HORIZ_NONE(EVerticalScroller.AS_NEEDED, EHorizontalScroller.NONE), VERT_AS_NEEDED__HORIZ_ALWAYS(EVerticalScroller.AS_NEEDED, EHorizontalScroller.ALWAYS), VERT_AS_NEEDED__HORIZ_AS_NEEDED(EVerticalScroller.AS_NEEDED, EHorizontalScroller.AS_NEEDED), VERT_ALWAYS__HORIZ_NONE(EVerticalScroller.ALWAYS, EHorizontalScroller.NONE), VERT_ALWAYS__HORIZ_ALWAYS(EVerticalScroller.ALWAYS, EHorizontalScroller.ALWAYS), VERT_ALWAYS__HORIZ_AS_NEEDED(EVerticalScroller.ALWAYS, EHorizontalScroller.AS_NEEDED), VERT_NONE__HORIZ_NONE(EVerticalScroller.NONE, EHorizontalScroller.NONE) // can be a signal to not wrap a Component with a JScrollPane
        , VERT_NONE__HORIZ_ALWAYS(EVerticalScroller.NONE, EHorizontalScroller.ALWAYS), VERT_NONE__HORIZ_AS_NEEDED(EVerticalScroller.NONE, EHorizontalScroller.AS_NEEDED);

        EScrollPolicy(EVerticalScroller v, EHorizontalScroller h) {
            fVerticalPolicy = v;
            fHorizontalPolicy = h;
        }

        final private EVerticalScroller fVerticalPolicy;
        final private EHorizontalScroller fHorizontalPolicy;
    }

    static // initializer block
    {
    }

    private JScrollPaneFactory_() {
    }

    static public JScrollPane create
            (final Component componentToEmbed
                    , final EScrollPolicy scrollPolicy
            ) {
        return create
                (componentToEmbed
                        , scrollPolicy.fVerticalPolicy
                        , scrollPolicy.fHorizontalPolicy
                        , EUsingInternalBorder.DEFAULT
                        , SCROLL_INCREMENT_PIXEL
                );
    }

    static public JScrollPane create
            (final Component componentToEmbed
                    , final EScrollPolicy scrollPolicy
                    , final EUsingInternalBorder usingInternalBorder
                    , final int scrollIncrement
            ) {
        return create
                (componentToEmbed
                        , scrollPolicy.fVerticalPolicy
                        , scrollPolicy.fHorizontalPolicy
                        , usingInternalBorder
                        , scrollIncrement
                );
    }

    static public JScrollPane create
            (final Component componentToEmbed // can be 'null'
                    , final EVerticalScroller verticalScroller
                    , final EHorizontalScroller horizontalScroller
                    , final EUsingInternalBorder usingInternalBorder
                    , final int scrollIncrement
            ) {
        if (verticalScroller == null || horizontalScroller == null || usingInternalBorder == null)
            throw new NullPointerException();

        final int verticalPolicy;
        switch (verticalScroller) {
            case NONE:
                verticalPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER;
                break;
            case ALWAYS:
                verticalPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS;
                break;
            case AS_NEEDED:
                verticalPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
                break;
            default:
                throw new IllegalArgumentException();
        }

        final int horizontalPolicy;
        switch (horizontalScroller) {
            case NONE:
                horizontalPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
                break;
            case ALWAYS:
                horizontalPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS;
                break;
            case AS_NEEDED:
                horizontalPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED;
                break;
            default:
                throw new IllegalArgumentException();
        }

        final JScrollPane jScrollPane = new JScrollPane(componentToEmbed, verticalPolicy, horizontalPolicy);

        if (scrollIncrement != SCROLL_INCREMENT_UNSPEC) {
            if (verticalPolicy != JScrollPane.VERTICAL_SCROLLBAR_NEVER)
                jScrollPane.getVerticalScrollBar().setUnitIncrement(scrollIncrement);
            if (horizontalPolicy != JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
                jScrollPane.getHorizontalScrollBar().setUnitIncrement(scrollIncrement);
        }

        switch (usingInternalBorder) {
            case NONE:
                jScrollPane.setBorder(null);
                break; // remove default border
            //... future border ops
            case DEFAULT:
                break;
            default:
                throw new IllegalArgumentException();
        }

        return jScrollPane;
    }
}
