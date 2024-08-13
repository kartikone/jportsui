package oz.zomg.jport.gui.window;

import oz.zomg.jport.PortConstants;
import oz.zomg.jport.TheApplication;
import oz.zomg.jport.TheOsBinaries;
import oz.zomg.jport.common.HttpUtil;
import oz.zomg.jport.common.ImageUtil_;
import oz.zomg.jport.common.Interfacing_.Targetable;
import oz.zomg.jport.common.StringsUtil_;
import oz.zomg.jport.common.gui.FocusedButtonFactory;
import oz.zomg.jport.gui.TheUiHolder;
import oz.zomg.jport.type.EPortStatus;
import oz.zomg.jport.type.Portable;
import oz.zomg.jport.type.Portable.Predicatable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Show an Application Information dialog box with
 * native bin or Installed port domain browse buttons.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JDialog_AboutApp extends JDialog
        implements ActionListener {
    static final private int _MAX_PIXEL_SIZE = 48;
    static final private Font _SAN_SERIF_9_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 9);
    static final private String
            _DEV_ENV = "Originally designed and coded in NetBeans IDE 7.2 on JDK 1.6.0_35", _COPYRIGHT_NOTICE = "(c) 2012 by Stephen Baber", _LICENSE_TEXT = "<HTML><CENTER><SMALL>"
            + _COPYRIGHT_NOTICE + "<BR>"
            + "<BR>"
            + "<IMG src=\"http://i.creativecommons.org/l/by-sa/3.0/80x15.png\"><BR><FONT color=blue><U>"
            + "This work is licensed under<BR>"
            + "a Creative Commons<BR>"
            + "Attribution-ShareAlike<BR>"
            + "3.0 Unported License<BR>";

    static {
    }

    final private AbstractButton jAb_Ok = FocusedButtonFactory.create("OK", "Close");

    public JDialog_AboutApp() {
        super
                (TheUiHolder.INSTANCE.getMainFrame() // stay on top
                        , "About " + PortConstants.APP_NAME
                        , ModalityType.APPLICATION_MODAL
                );

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setLayout(new BorderLayout());
        this.setSize(1024, 600);
        this.setLocationByPlatform(true);

        // center panel
        final JPanel centerPanel = new JPanel(new FlowLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 10)); // T L B R

        // determine unique domains
        final Map<String, Set<Portable>> orderedDomain_to_PortSet_Map = new TreeMap<String, Set<Portable>>();

        // filter by installed or native
        final Predicatable predicate = new Predicatable() // para-lambda expression
        {
            @Override
            public boolean evaluate(final Portable port) {
                return port.hasStatus(EPortStatus.INSTALLED) || TheOsBinaries.INSTANCE.has(port.getName());
            }
        };
        final Portable[] ports = TheApplication.INSTANCE.getPortsCatalog().getPortsInventory().filter(predicate);
        for (final Portable port : ports) {   // installed or Native
            final String domain = port.getDomain();
            if (!orderedDomain_to_PortSet_Map.containsKey(domain)) {
                orderedDomain_to_PortSet_Map.put(domain, new TreeSet<Portable>());
            }

            final Set<Portable> orderedSet = orderedDomain_to_PortSet_Map.get(domain);
            orderedSet.add(port);
        }

        orderedDomain_to_PortSet_Map.remove(""); // sometimes the domain is unknown even though installed
        orderedDomain_to_PortSet_Map.remove("http://www.darwinsys.com"); // already natively installed

        // fetch images from cache or in another I/O thread
        for (final Map.Entry<String, Set<Portable>> entry : orderedDomain_to_PortSet_Map.entrySet()) {
            final String domain = entry.getKey(); // alias
            final Set<Portable> portSet = entry.getValue(); // alias

            HttpUtil.retrieveLogoConcurrently
                    (domain
                            , new Targetable<Image>() // anonymous class
                            {
                                @Override
                                public void target(Image image) {
                                    if (image != null && image.getWidth(null) >= 16) {   // ".ICO" image may be usable
                                        _addDomainButton
                                                (centerPanel
                                                        , domain
                                                        , image
                                                        , portSet
                                                );
                                    }
                                }
                            }
                    );
        }

        // vertical panel
        final JPanel westPanel = new JPanel(null);
        westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.PAGE_AXIS));
        westPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 15, 0)); // T L B R

        final String version = PortConstants.getVersion();
        final JLabel jLabel_AppName = new JLabel("<HTML><CENTER><BIG><B>" + PortConstants.APP_NAME + "</B></BIG>" + (version.isEmpty() ? "" : "<BR>" + version));
        jLabel_AppName.setHorizontalAlignment(JLabel.CENTER);
        jLabel_AppName.setHorizontalTextPosition(JLabel.CENTER);

        final Image ozzomgImage = ImageUtil_.parseImage("/oz/zomg/jport/gui/window/oz-dorothy-public-domain-16-color.png");
        final AbstractButton jAb_BrowseHosting = _createBrowsingButton
                (new ImageIcon(ozzomgImage)
                        , "<HTML><CENTER><FONT color=blue><U>Java based, graphical user<BR>interface to MacPorts 2.0+"
                        , PortConstants.PROJ_HOSTING
                );

        final AbstractButton jAb_BrowseLicense = _createBrowsingButton
                (null
                        , _LICENSE_TEXT
                        , "http://creativecommons.org/licenses/by-sa/3.0/deed.en_US"
                );

        // center (OK) <- misaligns vertPanel due to HTML above
        final JPanel southPanel = new JPanel(); // FlowLayout.CENTER is default
        southPanel.add(jAb_Ok);

        // sub-assemble
        westPanel.add(jLabel_AppName);
        westPanel.add(jAb_BrowseHosting);
        westPanel.add(Box.createVerticalGlue());
        westPanel.add(jAb_BrowseLicense);
        // westPanel.add( Box.createVerticalGlue() );
        // Gaaah, LayoutManager vs embedded HTML issues. -> vertPanel.add( jAb_Ok );

        // assemble
        this.add(centerPanel, BorderLayout.CENTER);
        this.add(southPanel, BorderLayout.SOUTH);
        this.add(westPanel, BorderLayout.WEST);

        // listener
        jAb_Ok.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object obj = e.getSource();

        if (obj == jAb_Ok) {
            this.dispose();
        }
    }

    /**
     * Live adds a browse button with domain "favicon".
     * Avoids duplicate buttons.
     * Swing thread safe.
     *
     * @param toContainer button factory adds here
     * @param domain      web site of multiple Ports
     * @param image       logo for button
     * @param portSet     for tool tip
     */
    static private void _addDomainButton
    (final Container toContainer
            , final String domain
            , final Image image
            , final Set<Portable> portSet
    ) {
        if (SwingUtilities.isEventDispatchThread()) {
            final Icon icon = new ImageIcon(ImageUtil_.reduceImage(image, _MAX_PIXEL_SIZE));

            final AbstractButton ab = new JButton("<HTML><U>" + domain, icon);
            ab.setToolTipText(StringsUtil_.htmlTabularize(6, " ", " ", portSet));
            ab.setFont(_SAN_SERIF_9_FONT);
            ab.setBorderPainted(false);
            ab.setFocusPainted(false);
            ab.setContentAreaFilled(false);
            ab.setFocusable(false);
            ab.addActionListener(new ActionListener() // anonymous class
            {
                @Override
                public void actionPerformed(ActionEvent e) {
                    HttpUtil.browseTo(domain);
                }
            });

            toContainer.add(ab);
            toContainer.add(Box.createHorizontalStrut(5));
            toContainer.validate();
        } else {
            SwingUtilities.invokeLater(new Runnable() // anonymous class
            {
                @Override
                public void run() {
                    _addDomainButton(toContainer, domain, image, portSet);
                }
            });
        }
    }

    static private AbstractButton _createBrowsingButton
            (final Icon icon
                    , final String label
                    , final String webUrl
            ) {
        final AbstractButton ab = new JButton(label, icon);
        ab.setToolTipText("Browse to " + webUrl);
        ab.setHorizontalAlignment(AbstractButton.CENTER);
        ab.setHorizontalTextPosition(AbstractButton.CENTER);
        ab.setVerticalTextPosition(AbstractButton.BOTTOM);
        ab.setBorderPainted(false);
        ab.setContentAreaFilled(false);
        ab.setFocusable(false);

        ab.addActionListener(new ActionListener() // anonymous class
        {
            @Override
            public void actionPerformed(ActionEvent e) {
                HttpUtil.browseTo(webUrl);
            }
        });

        return ab;
    }

    static public void main(String[] args) {
        new JDialog_AboutApp().setVisible(true);
    }
}
