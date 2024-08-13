package oz.zomg.jport.gui.component;

import oz.zomg.jport.common.Notification.OneArgumentListenable;
import oz.zomg.jport.common.Util;

import javax.swing.*;


/**
 * Enum driven progress bar that listens to Notification.OneArgumentListenable<Enum>
 *
 * @param <E>
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class JProgress_Enum<E extends Enum<E>> extends JProgressBar
        implements OneArgumentListenable<E> {

    /**
     * Can be a smaller subset of the entire enumeration or follow a non-ordinal sequence.
     */
    final private E[] fEnums;

    /**
     * @param isLabeled shows enum.toString()
     * @param enumClass the full enumeration in ordinal sequence
     */
    public JProgress_Enum(final boolean isLabeled, final Class<E> enumClass) {
        this(isLabeled, enumClass.getEnumConstants());
    }

    /**
     * @param isLabeled
     * @param enums     can be a smaller subset of the entire enumeration or follow a non-ordinal sequence
     */
    @SafeVarargs
    public JProgress_Enum(final boolean isLabeled, final E... enums) {
        super(0, enums.length);

        fEnums = enums;
        setStringPainted(isLabeled);
        setVisible(false);
    }

    /**
     * Swing thread safe.
     *
     * @param here
     */
    @Override
    public void listen(final E here) {
        if (SwingUtilities.isEventDispatchThread()) {
            final int index = Util.indexOfIdentity(here, fEnums);
            if (index == Util.INVALID_INDEX) throw new IllegalArgumentException();

            setString(here.toString());
            setValue(index);
            setVisible(index != fEnums.length - 1);
        } else {
            // anonymous class
            SwingUtilities.invokeLater(() -> listen(here));
        }
    }
}
