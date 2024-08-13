package oz.zomg.jport.gui.component;

import oz.zomg.jport.PortConstants;
import oz.zomg.jport.common.Interfacing_.Creatable;
import oz.zomg.jport.common.gui.EmbeddedCreatorFactory_;
import oz.zomg.jport.common.gui.JScrollPaneFactory_.EScrollPolicy;
import oz.zomg.jport.gui.component.AJLabel_PortInfo.*;
import oz.zomg.jport.gui.panel.JPanel_CommonInfo;
import oz.zomg.jport.gui.table.TableModel_Variants;
import oz.zomg.jport.type.Portable;

import javax.swing.*;
import java.awt.*;


/**
 * Show various details of the lead selection from the Ports inventory.
 * At the bottom-right of the main UI or in an external frame.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JTabPane_Detail extends JTabbedPane {
    static {
    }

    /**
     * Follows main table selections.
     */
    public JTabPane_Detail() {
        this(Portable.NONE);
    }

    /**
     * Also constructor for externally framed port details.
     *
     * @param port assign NONE if waiting to be notified by user selections
     */
    public JTabPane_Detail(final Portable port) {
        super(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);

        this.addTab("Info", EmbeddedCreatorFactory_.create(new Creatable<Component>() {
            @Override
            public Component create() {
                return new JPanel_CommonInfo(port);
            }
        }));

        this.addTab("Description", EmbeddedCreatorFactory_.create(new Creatable<Component>() {
            @Override
            public Component create() {
                return new JLabel_Description(port);
            }
        }));

        this.addTab("Variants", EmbeddedCreatorFactory_.create(EScrollPolicy.VERT_AS_NEEDED__HORIZ_NONE
                , new Creatable<Component>() {
                    @Override
                    public Component create() {
                        return new TableModel_Variants(port).getJTable();
                    }
                }));

        this.addTab("Dependencies", EmbeddedCreatorFactory_.create(EScrollPolicy.VERT_AS_NEEDED__HORIZ_NONE
                , new Creatable<Component>() {
                    @Override
                    public Component create() {
                        return new JLabel_Dependencies(port);
                    }
                }));

        this.addTab("Dependants", EmbeddedCreatorFactory_.create(EScrollPolicy.VERT_AS_NEEDED__HORIZ_NONE
                , new Creatable<Component>() {
                    @Override
                    public Component create() {
                        return new JLabel_Dependants(port);
                    }
                }));

        this.addTab("Installed Files", EmbeddedCreatorFactory_.create(EScrollPolicy.VERT_AS_NEEDED__HORIZ_NONE
                , new Creatable<Component>() {
                    @Override
                    public Component create() {
                        return new JLabel_FilesInstalled(port);
                    }
                }));

//        this.addTab( "Versions"       , EmbeddedCreatorFactory.create( EScrollPolicy.VERT_AS_NEEDED__HORIZ_NONE
        //                                                                   , new Creatable<Component>() { @Override public Component create() { return new JLabel_Versioning    ( port ); } } ) );

        if (PortConstants.DEBUG) {   // reflection dump of non-static fields
            this.addTab("<>", EmbeddedCreatorFactory_.create(new Creatable<Component>() {
                @Override
                public Component create() {
                    return new JLabel_PortFields(port);
                }
            }));
        }
    }
}
