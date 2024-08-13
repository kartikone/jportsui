package oz.zomg.jport.gui.panel;

import oz.zomg.jport.TheOsBinaries;
import oz.zomg.jport.common.Reset.Resetable;
import oz.zomg.jport.common.gui.GuiUtil;
import oz.zomg.jport.gui.TheUiHolder;
import oz.zomg.jport.type.EPortStatus;
import oz.zomg.jport.type.Portable;
import oz.zomg.jport.type.Portable.Predicatable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Filter by Port status.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JPanel_StatusFilter extends JPanel
        implements
        ActionListener
        , ChangeListener {
    static final private int DEFAULT_WHATS_NEW_DAY = 2;

    final private AbstractButton
            ab_Marked = new JToggleButton("<HTML><FONT size=+0><I>Marked"), ab_Native = new JToggleButton("<HTML><I>Native"), ab_WhatsNew = new JToggleButton("<HTML><I>What's new?");
    final private ButtonGroup fButtonGroup = new ButtonGroup(); // needed for prefs
    final private JSpinner jSpin_Day = new JSpinner(new SpinnerNumberModel(DEFAULT_WHATS_NEW_DAY, 1, 9999, 1)); // val min max step

    private long mAgeMillisec = (1000 * 60 * 60 * 24L * DEFAULT_WHATS_NEW_DAY);

    public JPanel_StatusFilter() {
        super(new BorderLayout());

        this.setBorder(BorderFactory.createEmptyBorder(10, GuiUtil.GAP_PIXEL, GuiUtil.GAP_PIXEL, 0)); // T L B R
        this.setOpaque(false); // otherwise messes up Mac-PLAF tab pit darkening // this.setBackground( null ) <- copying in the JTabbedPane's background didn't work to darken as expected on Mac-PLAF

        jSpin_Day.setToolTipText("<HTML>Show Ports that have had their<BR>information updated within <I>X</I> days");
        ab_WhatsNew.setToolTipText("<HTML>Show Ports that have had their<BR>information updated within <I>X</I> days");
        ab_Native.setToolTipText("<HTML>Show set of binaries that are<BR>also provided by your OS");
        ab_Marked.setToolTipText("<HTML>Show Ports that have a pending<BR>status change request mark");

        final int BUTTON_HEIGHT_PIX = 34; // needs to be big enough so that the square button Mac-PLAF

        // needs a sub-panel, otherwise buttons will not horiz stretch in BoxLayout
        final JPanel northPanel = new JPanel(new GridLayout(0, 1, 0, GuiUtil.GAP_PIXEL));
        northPanel.setOpaque(false); // otherwise messes up Mac-PLAF tab pit darkening

        final AbstractButton[] enumAbs = new AbstractButton[EPortStatus.VALUES.length]; // nulls

        int i = 0;
        for (final EPortStatus e : EPortStatus.VALUES) {
            final AbstractButton ab = new JToggleButton(e.toString());
            ab.setActionCommand(e.name());
            ab.setToolTipText(e.provideTipText()); // built into enum
            ab.setForeground((e.provideColor() == null) ? null : e.provideColor().darker());
            ab.setFocusable(false);
            ab.setOpaque(false); // otherwise messes up Mac-PLAF tab pit darkening // ab.setBackground( null ) <- not on Mac-PLAF

            fButtonGroup.add(ab);
            northPanel.add(ab);

            switch (e) {
                case ALL: // fall-thru
                case UNINSTALLED: // fall-thru
                case INSTALLED: // fall-thru
                case OUTDATED:
                    ab.setText("<HTML><FONT size=+0><B>" + e);
                    break;
            }

            switch (e) {
                case ALL:
                    ab.setSelected(true);
                    break;

                case OBSOLETE:
                    northPanel.add(ab_Native);
                    break;

                case OUTDATED: // case Obsolete :
                    northPanel.add(ab_Marked); // 0=first, -1=last
                    northPanel.add(Box.createVerticalStrut(BUTTON_HEIGHT_PIX));
                    break; // spacer also controls height of all buttons
            }

            enumAbs[i++] = ab;

            ab.addActionListener(this);
        }

        northPanel.add(Box.createVerticalStrut(BUTTON_HEIGHT_PIX));

        // sub
        final JPanel subPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        subPanel.setOpaque(false);
        subPanel.add(new JLabel("<HTML><SMALL><RIGHT>Days Ago "));
        subPanel.add(jSpin_Day);

        // south
        final JPanel southPanel = new JPanel(new GridLayout(0, 1, 0, GuiUtil.GAP_PIXEL));
        southPanel.setOpaque(false); // otherwise messes up Mac-PLAF tab pit darkening
        jSpin_Day.setEnabled(false);

        ab_Marked.setFocusable(false);
        ab_Native.setFocusable(false);
        ab_WhatsNew.setFocusable(false);

        fButtonGroup.add(ab_Marked);
        fButtonGroup.add(ab_Native);
        fButtonGroup.add(ab_WhatsNew);

        southPanel.add(Box.createVerticalStrut(BUTTON_HEIGHT_PIX));
        southPanel.add(ab_WhatsNew);
        southPanel.add(subPanel);

        this.add(northPanel, BorderLayout.NORTH);
        this.add(southPanel, BorderLayout.SOUTH);
        this.add(Box.createHorizontalStrut(134), BorderLayout.CENTER); // keep subPanel from wrapping

        // listener
        ab_Marked.addActionListener(this);
        ab_Native.addActionListener(this);
        ab_WhatsNew.addActionListener(this);

        jSpin_Day.addChangeListener(this);

        final AbstractButton ab_All = enumAbs[EPortStatus.ALL.ordinal()];
        TheUiHolder.getResetNotifier().addListener(new Resetable() // anonymous class
        {
            @Override
            public void reset() {
                ab_All.doClick();
                jSpin_Day.setValue(DEFAULT_WHATS_NEW_DAY); // autobox
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object obj = e.getSource();

        if (obj instanceof AbstractButton ab) {

            final Predicatable predicate; // para-lambda
            if (ab == ab_WhatsNew) {
                jSpin_Day.setEnabled(true);
                setEpochFilter(mAgeMillisec);
                return; // required
            } else if (ab == ab_Native) {
                predicate = new Predicatable() // anonymous class
                {
                    @Override
                    public boolean evaluate(final Portable port) {
                        return TheOsBinaries.INSTANCE.has(port.getName());
                    }
                };
            } else if (ab == ab_Marked) {
                predicate = new Predicatable() // anonymous class
                {
                    @Override
                    public boolean evaluate(final Portable port) {
                        return !port.isUnmarked();
                    }
                };
            } else {
                final EPortStatus statusEnum = EPortStatus.valueOf(ab.getActionCommand());
                switch (statusEnum) {
                    case ALL:
                        predicate = Predicatable.ANY; // no narrowing, wide open
                        break;

                    default:
                        predicate = new Predicatable() // anonymous class
                        {
                            @Override
                            public boolean evaluate(final Portable port) {
                                return port.hasStatus(statusEnum);
                            }
                        };
                        break;
                }
            }

            jSpin_Day.setEnabled(false);

            TheUiHolder.INSTANCE.getPortFilterPredicate().setStatusFilter(predicate);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == jSpin_Day) {
            if (ab_WhatsNew.isSelected()) {   // check because Reset button was accidentally activating filter
                final int days = (Integer) jSpin_Day.getValue(); // autobox
                setEpochFilter(1000 * 60 * 60 * 24L * days);
            }
        }
    }

    private void setEpochFilter(final long ageMillisec) {
        mAgeMillisec = ageMillisec;
        final long epochAfterMillisec = System.currentTimeMillis() - ageMillisec;

        // para-lambda
        final Predicatable predicate = new Predicatable() // anonymous class
        {
            @Override
            public boolean evaluate(final Portable port) {
                return port.getModificationEpoch() >= epochAfterMillisec;
            }
        };
        TheUiHolder.INSTANCE.getPortFilterPredicate().setStatusFilter(predicate);
    }
}
