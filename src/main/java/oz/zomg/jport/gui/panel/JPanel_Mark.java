package oz.zomg.jport.gui.panel;

import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.Elemental;
import oz.zomg.jport.common.Elemental.EElemental;
import oz.zomg.jport.common.Util;
import oz.zomg.jport.gui.MarkConfirmUi;
import oz.zomg.jport.type.EPortMark;
import oz.zomg.jport.type.Portable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Mark up the selected port for "APPLY" processing on the CLI.
 * Also shows the present mark.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JPanel_Mark extends JPanel
        implements
        ActionListener
        , Elemental.Listenable<Portable> {
    static {
    }

    final private boolean fIsAssignmentLocked;
    final private AbstractButton[] ab_Marks;
    final private AbstractButton ab_Unmark = new JButton("Unmark");

    /**
     * Mutable for .actionPerformed() and follows table selection via .notify().  Must begin with 'null'
     */
    transient private Portable mAssignedPort = null;

    /**
     * Reflects the change request Mark of the big Ports table selection.
     */
    public JPanel_Mark() {
        this(Portable.NONE);
    }

    /**
     * @param assignedPort is the target Mark model.  Use Portable.NONE to signal that the Mark is driven by user's table selection.
     */
    public JPanel_Mark(final Portable assignedPort) {
        super(new BorderLayout());

        if (assignedPort == null) throw new NullPointerException();

        this.setBorder(BorderFactory.createEmptyBorder(0, (Util.isOnMac()) ? 0 : 5, 10, 5)); // T L B R gets buttons off the window resize handle

        fIsAssignmentLocked = assignedPort != Portable.NONE;

        final JPanel subPanel = new JPanel(new GridLayout(0, 1, 0, 5)); // row col hgap vgap

        int i = 0;
        ab_Marks = new AbstractButton[EPortMark.VALUES.length]; // nulls
        for (final EPortMark e : EPortMark.VALUES) {
            final AbstractButton ab = new JRadioButton(e.toString());
            ab.setActionCommand(e.name());
            ab.setToolTipText(e.provideTipText()); // built into enum
            ab.setEnabled(false);
            ab.setFocusable(false); // work-around for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4289940
            ab.addActionListener(this);
            ab_Marks[i] = ab;
            i++;

            if (e.provideIsVisible()) subPanel.add(ab, 0); // inserted in reverse of CLI exec order
        }

        ab_Unmark.setToolTipText("<HTML>Remove any pending Port status<BR>change requests for Apply");
        ab_Unmark.setFocusable(false); // work-around for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4289940
        ab_Unmark.setEnabled(false);
        subPanel.add(ab_Unmark, 0); // put [Unmark] at top

        this.add(subPanel, BorderLayout.NORTH); // using CENTER makes them not the expected rounded buttons of the Mac-PLAF

        // listener
        ab_Unmark.addActionListener(this);
        TheApplication.INSTANCE.getCrudNotifier().addListenerWeakly(this); // automatically calls .notify() and updates mAssignedPort conforming the view
    }

    /**
     * Needed as we are not using the normal ButtonGroup mutex.
     * Mutually-exclusive selection with radios normally managed with a ButtonGroup but we need
     * a weird state of nothing selected without extending the ButtonModel class.
     *
     * @param port
     */
    private void setGui_MarkSelection(final Portable port) {
        final EPortMark selectedMark = port.getMark(); //  can be 'null' for none
        for (final EPortMark markEnum : EPortMark.VALUES) {
            final AbstractButton ab = ab_Marks[markEnum.ordinal()];
            ab.setSelected(markEnum == selectedMark);
        }

        ab_Unmark.setEnabled(selectedMark != null);
    }

    /**
     * Listens only if driven by main table selections.
     *
     * @param elemental action
     * @param port      of marking view
     */
    @Override
    public void notify(final EElemental elemental, final Portable port) {
        switch (elemental) {
            case RETRIEVED: {
                if (!fIsAssignmentLocked || mAssignedPort == null) {
                    mAssignedPort = port;

                    for (final EPortMark markEnum : EPortMark.VALUES) {
                        final AbstractButton ab = ab_Marks[markEnum.ordinal()];
                        final boolean enable = port != Portable.NONE && markEnum.isApplicable(port); // make sure .NONE doesn't get the "install" option
                        ab.setEnabled(enable);
                    }

                    setGui_MarkSelection(port);
                }
            }
            break;

            case UPDATED: {
                if (mAssignedPort == port) {   // filtered out non-related updates
                    setGui_MarkSelection(port);
                }
            }
            break;
        }
    }

    /**
     * Control Port marking.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        final AbstractButton ab = (AbstractButton) e.getSource();

        if (ab == ab_Unmark) {   // clear command
            mAssignedPort.unmark();
            setGui_MarkSelection(mAssignedPort); // needed as we are not using the normal ButtonGroup mutex
        } else {   // enum radios
            final EPortMark markEnum = EPortMark.valueOf(ab.getActionCommand());
            MarkConfirmUi.showConfirmation(mAssignedPort, markEnum);
        }
    }

    /**
     * Not needed as instance listens to CRUD weakly.
     */
    @Override
    public void removeNotify() {
        TheApplication.INSTANCE.getCrudNotifier().removeListener(this);
    }
}
