package oz.zomg.jport.common;

import oz.zomg.jport.common.Notification.ANotifier;
import oz.zomg.jport.common.Notification.NotificationListenable;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


/**
 * Name space for Observer and Observable C.R.U.D. (Create, Retrieve, Update, Delete)
 * events for a Collection of elements or an array of elements.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class Elemental {
    /**
     * Operation to apply when late synchronizing collections of elements via the Producer-Consumer design pattern.
     */
    public enum EElemental {
        CREATED     // element .add()
        , RETRIEVED   // element selected by user
        , UPDATED     // element changed, usually mutable in composition
        , DELETED     // element .remove()
//...            , REMOVED_ALL // all elements .clear()
//...            , REBUILT     // flush and cold start from the backing collection
        ;
        static final EElemental[] VALUES = EElemental.values();
    }

    private Elemental() {
    }

    /**
     * Cause elemental notifications to occur that would mutate listeners
     * from a "before" Set state into an "after" Set state.
     *
     * @param <E>
     * @param notifier
     * @param beforeSet
     * @param afterSet
     */
    static private <E> void differenceEngine(final Notifier<E> notifier, final Set<E> beforeSet, final Set<E> afterSet) {
        if (!afterSet.equals(beforeSet)) {
            final Set<E> includeSet = new HashSet<E>();
            for (final E element : afterSet) {
                if (!beforeSet.contains(element)) {
                    includeSet.add(element);
                }
            }

            final Set<E> excludeSet = new HashSet<E>();
            for (final E element : afterSet) {
                if (!beforeSet.contains(element)) {
                    excludeSet.add(element);
                }
            }

            for (final E element : includeSet) {
                notifier.causeNotification(EElemental.CREATED, element);
            }

            for (final E element : excludeSet) {
                notifier.causeNotification(EElemental.DELETED, element);
            }
        }
    }


    // ================================================================================

    /**
     * @param <E> element of class type
     */
    public interface Listenable<E> extends NotificationListenable {
        void notify(EElemental elemental, E obj);
    }


    // ================================================================================

    /**
     * Single element C.R.U.D. notifications.
     * Thread safe.
     *
     * @param <E> element of class type
     */
    static public class Notifier<E> extends ANotifier<Listenable<E>> {
        /**
         * for automatic catch-up of listener when it subscribes with .add()
         */
        private E mLastRetrieved;

        public Notifier() {
            this(null);
        }

        /**
         * @param initiallyRetrieved usually a guard reference representing a NONE or UNKNOWN state but can be a "selection"
         */
        public Notifier(final E initiallyRetrieved) {
            mLastRetrieved = initiallyRetrieved;
        }

        /**
         * Avoid copy-pasta.
         *
         * @param listenable Will catch-up / cold-start the new listener.
         */
        private void contextualize(final Listenable<E> listenable) {
            listenable.notify(EElemental.RETRIEVED, mLastRetrieved);
        }

        /**
         * Subscribe to notifications until <CODE>.removeListener( x )</CODE> is invoked.
         *
         * @param listenable Will catch-up / cold-start the new listener.
         */
        @Override
        synchronized public void addListener(final Listenable<E> listenable) {
            contextualize(listenable);
            super.addListener(listenable);
        }

        /**
         * Subscribe to notifications until 'listenable' is no longer strongly reachable elsewhere.
         *
         * @param listenable Will catch-up / cold-start the new listener.
         */
        @Override
        synchronized public void addListenerWeakly(final Listenable<E> listenable) {
            contextualize(listenable);
            super.addListenerWeakly(listenable);
        }

        /**
         * Notify listeners.
         *
         * @param elemental
         * @param obj
         */
        synchronized public void causeNotification(final EElemental elemental, final E obj) {
            if (elemental == EElemental.RETRIEVED) {   // remember for catching-up / cold-starting new listeners
                if (obj != mLastRetrieved) {
                    mLastRetrieved = obj; // continues below
                } else {   // no change, so do not notify listeners
                    return;
                }
            }

            final Enumeration<Listenable<E>> enumerati = createEnumeration();
            while (enumerati.hasMoreElements()) {
                enumerati.nextElement().notify(elemental, obj);
            }
        }

        /**
         * @return for GUI cold starts of a past singular selection element
         */
        synchronized E getLastRetrieved() {
            return mLastRetrieved;
        }
    }
}
