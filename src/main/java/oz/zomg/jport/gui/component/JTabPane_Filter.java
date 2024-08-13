package oz.zomg.jport.gui.component;

import oz.zomg.jport.common.Interfacing_.Creatable;
import oz.zomg.jport.common.Util;
import oz.zomg.jport.common.gui.EmbeddedCreatorFactory_;
import oz.zomg.jport.gui.panel.JPanel_Histogram;
import oz.zomg.jport.gui.panel.JPanel_StatusFilter;

import javax.swing.*;
import java.awt.*;


/**
 * Contains controls for filtering the Port inventory table.
 * On the left side of the window.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JTabPane_Filter extends JTabbedPane {
    public JTabPane_Filter() {
        super
                ((Util.isOnMac()) ? JTabbedPane.RIGHT : JTabbedPane.TOP // Metal-PLAF doesn't do small numbers of LEFT & RIGHT tabs
                        , JTabbedPane.WRAP_TAB_LAYOUT
                );

//?        final SingleSelectionModel ssm = this.getModel(); // manages which tab

//        final HistogramUiFactory histogramUi = new HistogramUiFactory();

        this.addTab("Status", EmbeddedCreatorFactory_.create(new Creatable<Component>() {
            @Override
            public Component create() {
                return new JPanel_StatusFilter();
            }
        }));
        this.addTab("Totals", EmbeddedCreatorFactory_.create(new Creatable<Component>() {
            @Override
            public Component create() {
                return new JPanel_Histogram();
            }
        }));

//        if( HistogramUiFactory.NEED_BREAK_OUT_CATEGORIES )
//        this.addTab( "Categories" , EmbeddedCreatorFactory.create( new Creatable<Component>() { @Override public Component create() { return histogramUi.createCategoriesComponent(); } } ) );

//...        this.addTab( "Search Results", new JLabel( "SHOW HERE..." ) ); //...

        // doen't have expected declutter effect on Mac -> this.setBorder( null );
    }
}
