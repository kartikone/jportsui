package oz.zomg.jport.gui.window;

import oz.zomg.jport.common.Interfacing_.Targetable;
import oz.zomg.jport.common.gui.FocusedButtonFactory;
import oz.zomg.jport.gui.TheUiHolder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;


/**
 * Present Administrator password request that only
 * calls back a Targetable lambda expression when user
 * types [ENTER] or clicks (OK).
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JDialog_PasswordPlease extends JDialog
        implements ActionListener {
    static {
    }

    final private AbstractButton ab_Cancel = new JButton("Cancel");

    /**
     * Default button.
     */
    final private AbstractButton ab_Ok = FocusedButtonFactory.create("OK", "Use the password");

    /**
     * Character hiding when typing, will accommodate more than 28 chars.
     */
    final private JPasswordField jPassword = new JPasswordField(28);

    final private Targetable<String> fTargetable;

    /**
     * Title of the password dialog is "Authentication"
     *
     * @param okTitle
     * @param commandLabelText
     * @param adminPassword
     * @param targetable       calls back with password
     */
    public JDialog_PasswordPlease
    (final String okTitle
            , final String commandLabelText
            , final String adminPassword
            , final Targetable<String> targetable
    ) {
        this
                (TheUiHolder.INSTANCE.getMainFrame() // stay on top
                        , okTitle
                        , commandLabelText
                        , adminPassword
                        , targetable
                );
    }

    /**
     * Title of the password dialog is "Authentication"
     *
     * @param owner            parent window
     * @param okTitle
     * @param commandLabelText
     * @param adminPassword
     * @param targetable       calls back with password
     */
    private JDialog_PasswordPlease
    (final Window owner
            , final String okTitle
            , final String commandLabelText
            , final String adminPassword
            , final Targetable<String> targetable
    ) {
        super
                (owner
                        , "Authentication"
                        , ModalityType.APPLICATION_MODAL
                );

        if (targetable == null) throw new NullPointerException();
        fTargetable = targetable;

        // inner north
        final JLabel jLabel_Cmd = new JLabel("<HTML><FONT size=+0><I>An admin or root password is required to</I><BR><B>" + commandLabelText);

        // inner south
        jPassword.setText(adminPassword);
        jPassword.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        jPassword.setSelectionStart(0);
        jPassword.setSelectionEnd(99999);
        jPassword.requestFocusInWindow(); // put insertion caret here

        // outer center
        final JPanel centerPanel = new JPanel(new BorderLayout(20, 20));
        centerPanel.add(jLabel_Cmd, BorderLayout.NORTH);
        centerPanel.add(jPassword, BorderLayout.SOUTH);

        // outer south
        final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // T L B R
        southPanel.add(ab_Cancel);
        southPanel.add(Box.createHorizontalStrut(15));
        southPanel.add(ab_Ok);

        if (okTitle != null && !okTitle.isEmpty()) {
            ab_Ok.setText(okTitle);
        }

        // outer west
        final JPanel westPanel = new JPanel(); // moves icon to top of west constraint
        final JLabel jLabel_Warn = new JLabel("   ", UIManager.getIcon("OptionPane.warningIcon"), JLabel.LEFT); // 3 spaces
        jLabel_Warn.setVerticalTextPosition(JLabel.BOTTOM);
        westPanel.add(jLabel_Warn);

        // assemble
        this.add(centerPanel, BorderLayout.CENTER);
        this.add(southPanel, BorderLayout.SOUTH);
        this.add(westPanel, BorderLayout.WEST);

        ((JPanel) this.getContentPane()).setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20)); // T L B R

        this.pack();
        this.setLocationRelativeTo(owner);

        // listener
        ab_Cancel.addActionListener(this);
        ab_Ok.addActionListener(this);
        jPassword.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object obj = e.getSource(); // alias

        // javadoc recommended password security precautions
        final char[] chars = jPassword.getPassword(); // alias
        final String password = new String(chars);
        Arrays.fill(chars, (char) 0);

        //... if obj == ok, cancel, jPass
        this.dispose(); // close window since Future can not receive a reference to the JDialog

        if (obj != ab_Cancel) {
            fTargetable.target(password);
        }
    }

    /**
     * No way to make it use a JPasswordField?
     *
     * @param cmd
     * @param adminPassword
     * @return
     */
    static private String showPasswordInput(final String cmd, final String adminPassword) {
        final String response = JOptionPane.showInputDialog
                (TheUiHolder.INSTANCE.getMainFrame()
                        , "<HTML>An admin or root password is required to <BR><B>" + cmd
                );
        return (response != null) ? response : "";
    }

    // TEST
//    static public void main( String[] args )
//    {
//        final Targetable<String> targetable = new Targetable<String>()
//                {   @Override public void target( String obj )
//                    {   System.out.println( "result="+ obj );
//                    }
//                };
//
//        JDialog jd = new JDialog_PasswordPlease( null, "Alpha Alpha Alpha Alpha Alpha <BR>Bravo Bravo <BR>Charlie", "*password*", targetable );
//        jd.setVisible( true );
//    }
}
