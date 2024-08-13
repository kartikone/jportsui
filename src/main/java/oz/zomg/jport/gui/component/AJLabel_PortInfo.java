package oz.zomg.jport.gui.component;

import oz.zomg.jport.TheApplication;
import oz.zomg.jport.TheOsBinaries;
import oz.zomg.jport.common.Elemental;
import oz.zomg.jport.common.Elemental.EElemental;
import oz.zomg.jport.common.StringsUtil_;
import oz.zomg.jport.common.Util;
import oz.zomg.jport.ports.PortsCliUtil;
import oz.zomg.jport.type.EPortStatus;
import oz.zomg.jport.type.Portable;

import javax.swing.*;
import java.util.Arrays;


/**
 * Outer abstraction for embedding Port information in the various JTabPane_Detail tabs.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public abstract class AJLabel_PortInfo extends JLabel
        implements Elemental.Listenable<Portable> {
    static final public String SELECT_PORT_TEXT = "<HTML><BIG><I>Select a port from the above list";
    static final private char SQUARE_ROOT_CHAR = '\u221A';

    static {
    }

    final private boolean fIsAssignmentLocked;

    /**
     * Mutable for .actionPerformed() and follows table selection via .notify().  Must begin with 'null'
     */
    transient private Portable mAssignedPort = null;

    /**
     * @param assignedPort Portable.NONE for follows selection
     */
    AJLabel_PortInfo(final Portable assignedPort) {
        if (assignedPort == null) throw new NullPointerException();

        fIsAssignmentLocked = assignedPort != Portable.NONE;

        this.setOpaque(false);
        this.setVerticalAlignment(SwingConstants.TOP);
        this.setHorizontalAlignment(SwingConstants.LEFT);

        // listener
        TheApplication.INSTANCE.getCrudNotifier().addListenerWeakly(this); // automatically calls .notify() and updates mAssignedPort conforming the view
    }

    /**
     * Previously listens only if driven by main table selections.
     * Now can monitor any change.
     *
     * @param elemental action
     * @param port      of marking view
     */
    @Override
    final public void notify(final EElemental elemental, final Portable port) {
        switch (elemental) {
            case RETRIEVED: {
                if (!fIsAssignmentLocked || mAssignedPort == null) {
                    mAssignedPort = port;
                    setPort(port);
                }
            }
            break;

            case UPDATED: {
                if (mAssignedPort == port) {   // filtered out non-related updates
                    setPort(port);
                    break; // something may have changed
                }
            }
            break;
        }
    }

    /**
     * Convert to multi-line HTML.
     *
     * @param lines
     */
    private void setText(final String[] lines) {
        setText(false, lines);
    }

    /**
     * @param needTable if 'true' then each line is a table row, still needs table data tags though
     * @param lines
     */
    private void setText(final boolean needTable, final String[] lines) {
        final StringBuilder sb = new StringBuilder("<HTML>" + (needTable ? "<TABLE>" : ""));

        for (final String line : lines) {
            sb.append(needTable ? "<TR>" : "");
            sb.append(line);
            sb.append(needTable ? "</TR>" : "<BR>");
        }

        this.setText(sb.toString());
    }

    /**
     * @param port conforming the label view
     */
    abstract void setPort(Portable port);

    /**
     * Not needed as instance listens to CRUD weakly.
     */
    @Override
    public void removeNotify() {
        TheApplication.INSTANCE.getCrudNotifier().removeListener(this);
    }


    // ================================================================================

    /**
     * Debug view of port private, etc. instance fields
     */
    static class JLabel_PortFields extends AJLabel_PortInfo {
        JLabel_PortFields(final Portable port) {
            super(port);
        }

        @Override
        void setPort(final Portable port) {
            if (port == Portable.NONE) {
                setText(SELECT_PORT_TEXT);
            } else {
                final String text = StringsUtil_.toHtmlTable(false, Util.dumpFields(port, false));
                setText(text);
            }
        }
    }


    // ================================================================================

    /**
     * Short and long description of the port.
     */
    static class JLabel_Description extends AJLabel_PortInfo {
        JLabel_Description(final Portable port) {
            super(port);

            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setBorder(BorderFactory.createEmptyBorder(10, 20, 0, 20)); // T L B R
//            this.setHorizontalTextPosition( JLabel.RIGHT );
//            this.setVerticalTextPosition( JLabel.TOP );
        }

        @Override
        void setPort(final Portable port) {
            if (port == Portable.NONE) {
                this.setText(SELECT_PORT_TEXT);
            } else {
                final String text = "<HTML><CENTER><BIG><B>" + _getFontHtml(port) + port.getName() + "</FONT></B></BIG><BR><BR>"
                        + "<B><I> " + port.getShortDescription() + "</I></B></CENTER><BR><BR>";

                final String text2 = (!port.getShortDescription().equals(port.getLongDescription()))
                        ? port.getLongDescription().replace("{", "<B>").replace("}", "</B>")
                        : ""; // not shown when short=long

                this.setText(text + text2);
            }
        }
    }


    // ================================================================================

    /**
     * This port depends on what other ports.
     */
    static class JLabel_Dependencies extends AJLabel_PortInfo {
        JLabel_Dependencies(final Portable port) {
            super(port);
        }

        @Override
        void setPort(final Portable port) {
            int count = 0;
            if (port == Portable.NONE) {
                setText(SELECT_PORT_TEXT);
            } else {
                final Portable[] ports = port.getFullDependencies();
                final String[] names = StringsUtil_.toStrings(ports);

                for (int i = 0; i < ports.length; i++) {
                    final String prepend = "<TD>" + _getFontHtml(ports[i]);
                    final String append = "</FONT></TD> <TD><FONT color=gray>" + ports[i].getShortDescription() + "</FONT></TD>";

                    if (ports[i].hasStatus(EPortStatus.ACTIVE)) {   // color Active ports green
                        names[i] = prepend + "<B>" + SQUARE_ROOT_CHAR + ' ' + names[i] + "</B>" + append; // needs prepended space for sort
                    } else {   // not installed
                        names[i] = prepend + names[i] + append;
                    }
                }

                Arrays.sort(names); // moves Actives <FONT.. to the top
                super.setText(true, names); // table

                count = ports.length;
            }

            this.setToolTipText((count == 0) ? null : "<HTML><B>" + count + "</B> dependencies");
        }
    }


    // ================================================================================

    /**
     * This ports is depended upon by these other ports.
     */
    static class JLabel_Dependants extends AJLabel_PortInfo {
        JLabel_Dependants(final Portable port) {
            super(port);
        }

        @Override
        void setPort(final Portable port) {
            int count = 0;
            if (port == Portable.NONE) {
                setText(SELECT_PORT_TEXT);
            } else {
                final Portable[] ports = port.getDependants();
                final String[] names = StringsUtil_.toStrings(ports);

                for (int i = 0; i < ports.length; i++) {
                    final String prepend = "<TD>";
                    final String append = "</TD> <TD><FONT color=gray>" + ports[i].getShortDescription() + "</FONT></TD>";

                    if (ports[i].hasStatus(EPortStatus.ACTIVE)) {   // color Active ports red
                        names[i] = prepend + " <FONT color=red><B>" + SQUARE_ROOT_CHAR + ' ' + names[i] + " </B></FONT>" + append; // needs prepended space for sort
                    } else {   // not installed
                        names[i] = prepend + names[i] + append;
                    }
                }

                Arrays.sort(names); // moves Actives <FONT.. to the top
                super.setText(true, names); // table

                count = ports.length;
            }

            this.setToolTipText((count == 0) ? null : "<HTML><B>" + count + "</B> Ports dependant on <B>" + port.getName());
        }
    }


    // ================================================================================

    /**
     * Ports CLI reports that the following "/opt/..." files were installed by this port.
     */
    static class JLabel_FilesInstalled extends AJLabel_PortInfo {
        JLabel_FilesInstalled(final Portable port) {
            super(port);
        }

        @Override
        void setPort(final Portable port) {
            if (port == Portable.NONE) {
                setText(SELECT_PORT_TEXT);
            } else {
                super.setText(PortsCliUtil.cliFileContents(port));
            }
        }
    }


    // ================================================================================

    /**
     * Other versions of the port still installed and available in the user's "/opt/..."
     */
    static class JLabel_Versioning extends AJLabel_PortInfo {
        // via port echo $PORT actinact ?
        JLabel_Versioning(final Portable port) {
            super(port);
        }

        @Override
        void setPort(final Portable port) {
            if (port == Portable.NONE) {
                setText(SELECT_PORT_TEXT);
            } else {
                //...                 Arrays.sort( ports );
            }
        }
    }

    static private String _getFontHtml(final Portable port) {
        return (port.hasStatus(EPortStatus.ACTIVE)) ? "<FONT color=blue>"
                : (TheOsBinaries.INSTANCE.has(port.getName())) ? "<FONT color=green>"
                : "<FONT>";
    }
}
