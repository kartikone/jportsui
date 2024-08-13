package oz.zomg.jport.common;

import oz.zomg.jport.common.Notification.ANotifier;
import oz.zomg.jport.common.Notification.NotificationListenable;

import java.util.Enumeration;


/**
 * Name space class.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class Reset {
    private Reset() {
    }

    // ================================================================================

    /**
     * Implementor can be .reset().
     * Not part of common.Interfacing because extends Listenable.
     *
     * @author sbaber
     */
    public interface Resetable extends NotificationListenable {
        void reset();
    }


    // ================================================================================

    /**
     * Resets listeners.
     */
    static public class Reseter extends ANotifier<Resetable> {
        /**
         * Called after a probe of Ports information to redisplay all contents in the GUI.
         */
        synchronized public void causeReset() {
            final Enumeration<Resetable> enumerati = createEnumeration();
            while (enumerati.hasMoreElements()) {
                enumerati.nextElement().reset();
            }
        }
    }
}
