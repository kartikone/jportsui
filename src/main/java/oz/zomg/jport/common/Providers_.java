package oz.zomg.jport.common;

import javax.swing.*;
import java.awt.*;


/**
 * Name space class.
 * <H3><I><FONT color="#770000">Subset of original source.</FONT></I></H3>
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class Providers_ {
    private Providers_() {
    }

    // stateless
    public interface ClassProvidable {
        Class<?> provideClass();
    } // use with TabelModel.getColumnClass() etc.

    public interface ColorProvidable {
        Color provideColor();
    }

    public interface DisplayTextProvidable {
        String provideDisplayText();
    }

    public interface EnabledProvidable {
        boolean provideIsEnabled();
    } // Component attribute

    public interface ForeBackColorProvidable {
        Color provideForeColor();

        Color provideBackColor();
    }

    public interface JPopupMenuProvidable {
        JPopupMenu provideJPopupMenu();
    } // enables JTable right-clicks

    public interface TipProvidable {
        String provideTipText();
    } // enables any object to provide a text for a Tooltip

    public interface VisibilityProvidable {
        boolean provideIsVisible();
    } // Component attribute

    public interface WidthProvidable {
        int provideWidth();
    }

    // contextual
    public interface ContextualVisibilityProvidable<T> {
        boolean provideIsVisible(T context);
    }

    public interface ContextualEnabledProvidable<T> {
        boolean provideIsEnabled(T context);
    }

    public interface RowProvidable<R> {
        R provideRow(final int index);
    } // used with JTable for tooltips
}
