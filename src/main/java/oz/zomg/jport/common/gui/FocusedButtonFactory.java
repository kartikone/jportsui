package oz.zomg.jport.common.gui;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


/**
 * Mac users expect one "Default" button per Dialog window.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class FocusedButtonFactory {
    private FocusedButtonFactory() {
    }

    /**
     * Highlighted as default action when [Enter] key typed.
     *
     * @param text
     * @param tip
     * @return
     */
    static public AbstractButton create(final String text, final String tip) {
        final JButton jb = new JButton(text);
        jb.setToolTipText((tip != null && !tip.isEmpty()) ? tip : null);
        jb.setSelected(true);
        jb.setDefaultCapable(true);
        jb.setFocusable(true);
        jb.requestFocusInWindow(); // successfully hilites only if in Container?

        // accept [ENTER] or [CR] key as well as [SPACE BAR] but don't double fire with .isActionKey() by disabling the button
        jb.addKeyListener(new KeyAdapter() // anonymous class
        {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.isActionKey()) {
                    jb.doClick(); // cause action in EDT first
                    jb.setEnabled(false); // then debounce any follow ups
                }
            }
        });

        return jb;
    }
}
