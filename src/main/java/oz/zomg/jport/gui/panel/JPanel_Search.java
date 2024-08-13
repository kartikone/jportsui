package oz.zomg.jport.gui.panel;

import oz.zomg.jport.common.Reset.Resetable;
import oz.zomg.jport.common.gui.GuiUtil;
import oz.zomg.jport.gui.Commander;
import oz.zomg.jport.gui.Commander.ESearchWhere;
import oz.zomg.jport.gui.TheUiHolder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 * UI for only the search functions.
 *
 * @author sbaber
 */
@SuppressWarnings("serial")
class JPanel_Search extends JPanel
        implements
        ActionListener
        , FocusListener
        , KeyListener {
    static {
    }

    final private Commander fCommander;

    final private AbstractButton ab_ClearSearch = new JButton("X");
    final private JComboBox jCombo_LookIn = new JComboBox(ESearchWhere.values());
    final private JTextField jField_Search = new JTextField(16);


    JPanel_Search(final Commander commander) {
        assert commander != null;

        fCommander = commander;

        ab_ClearSearch.setToolTipText("Clear search text");
        jCombo_LookIn.setToolTipText("Choose what Port information to search");
        jField_Search.setToolTipText("Use '+' to require each search term be present");

        ab_ClearSearch.setFocusable(false);
        jCombo_LookIn.setFocusable(false);

        jField_Search.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        jField_Search.setHorizontalAlignment(JTextField.CENTER);
        jField_Search.requestFocusInWindow();

        ab_ClearSearch.setPreferredSize(new Dimension(22, 22)); // Mac-PLAF ignored .setMaximumSize() alone
        ab_ClearSearch.setMargin(GuiUtil.ZERO_INSET);

        // assemble
        this.add(jCombo_LookIn);
        this.add(new JLabel("<HTML><BIG>\u26B2")); // unicode character "neuter", looks sort of like Mac's magnify glass
        this.add(jField_Search);
        this.add(ab_ClearSearch);

        // listeners
        ab_ClearSearch.addActionListener(this);

        jCombo_LookIn.addActionListener(this);
        jField_Search.addActionListener(this);

        jField_Search.addFocusListener(this);
        jField_Search.addKeyListener(this);

        TheUiHolder.getResetNotifier().addListener(new Resetable() // anonymous class
        {
            @Override
            public void reset() {
                clearTextSearch();
            }
        });
    }

    /**
     * Reset text filter.
     */
    private void clearTextSearch() {
        jField_Search.setText("");
        doDirectedTextSearch();
    }

    /**
     * Set up by user interaction with GUI Components.
     */
    private void doDirectedTextSearch() {
        final String searchText = jField_Search.getText();
        final ESearchWhere searchWhereEnum = (ESearchWhere) jCombo_LookIn.getSelectedItem();

        fCommander.doDirectedTextSearch(searchText, searchWhereEnum);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object obj = e.getSource();
        if (obj instanceof AbstractButton ab) {
            if (ab == ab_ClearSearch) {
                clearTextSearch();
            }
        } else if (obj instanceof JComboBox) {
            doDirectedTextSearch();
        } else if (obj instanceof JTextField) {
            doDirectedTextSearch();
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        jField_Search.select(0, 9999); // select all
    }

    @Override
    public void focusLost(FocusEvent e) {
        jField_Search.select(9999, 9999); // deselect
    }

    /**
     * Ignores the single, initial key stroke.
     *
     * @param e
     */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (jField_Search.getText().length() != 1) {   // Don't waste time with initial, worse-case of a single char.
            // Though single char searches can be initiated by typing [ENTER] or [CR].
            doDirectedTextSearch();
        }
    }
}
