package oz.zomg.jport.common;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;


/**
 * Name space class for
 * mostly awesome Notifications.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class Notification {
    /**
     * Name space class.
     */
    private Notification() {
    }


    // ================================================================================

    /**
     * Allows exposing only the subscription aspect of the Notification implementation.
     *
     * @param <L> listener of class type will be inferred
     */
    public interface Notifiable<L extends NotificationListenable> {
        /**
         * Unsubscribe from notifications.
         * Note: if added weakly then removal happens automatically when no other Strongly reachable objects refer to it.
         *
         * @param listenable does not have to exist
         */
        void removeListener(final L listenable);

        /**
         * Subscribe to notifications.
         *
         * @param listenable
         */
        void addListener(final L listenable);

        /**
         * Subscribe to notifications for as long as the instance of L's implementor is strongly referenced outside of the Notifier.
         * Works well for Swing components but do not use with non-retained Anonymous Class objects.
         * Depending on the implementation, those might be immediately GC'd.
         *
         * @param listenable will automatically stop being notified when the reference count to this instance (it) drops to zero
         */
        void addListenerWeakly(final L listenable);
    }


    // ================================================================================

    /**
     * Abstract base class that aggregates Listeners but does not cause notifications.
     * Your derived class will invoke listener notification via an Enumeration visitor.
     * Notification method can -not- be declared 'abstract' because the actual .causeNotification( ... )
     * method will have various differing parameters per implementation,
     * which would break Java's method signature driven interfaces.
     * <p>
     * Thread safe.
     *
     * @param <L> class type will be inferred
     */
    static abstract public class ANotifier<L extends NotificationListenable>
            implements Notifiable<L> {
        /**
         * No further allocations required, para-lambda expression.
         */
        static final private Enumeration<?> EMPTY_ENUMERATION = new Enumeration<Object>() // anonymous class
        {
            @Override
            public boolean hasMoreElements() {
                return false;
            }

            @Override
            public Object nextElement() {
                throw new NoSuchElementException();
            }
        };

        static {
        }

        /**
         * Non-Garbage Collected hard references to Listeners.
         */
        volatile private NotificationListenable[] vNoGcListeners = null;

        /**
         * WARNING! non-retained anonymous classes will be immediately GC'd.
         */
        volatile private Reference<L>[] vWeakListeners = null;

        /**
         * Can be sub-classed.
         */
        public ANotifier() {
        }

        /**
         * @param listenable      can not be 'null'.
         * @param isWeakReference 'true' to automatically remove when no strong reference exists in the object retention graph
         */
        @SuppressWarnings("unchecked")
        synchronized private void add(final L listenable, final boolean isWeakReference) {
            if (listenable == null) throw new NullPointerException();

            if (!isWeakReference) {   // normally retained
                if (vNoGcListeners == null) {   // start up case
                    vNoGcListeners = new NotificationListenable[]{listenable};
                } else {   // append to new array, too small to warrant a List
                    final NotificationListenable[] srcRefs = vNoGcListeners;
                    final int length = srcRefs.length;
                    final NotificationListenable[] destRefs = new NotificationListenable[length + 1]; // nulls
                    System.arraycopy(srcRefs, 0, destRefs, 0, length); // native copy
                    destRefs[length] = listenable; // at end
                    vNoGcListeners = destRefs;
                }
            } else {   // weakly retained
                final WeakReference<L> weakRef = new WeakReference<L>(listenable);
                if (vWeakListeners == null) {   // start up case
                    vWeakListeners = (Reference<L>[]) new Reference[]{weakRef};
                } else {   // append to new array, too small to warrant a List
                    final Reference<L>[] srcRefs = vWeakListeners;
                    final int length = srcRefs.length;
                    final Reference<L>[] destRefs = (Reference<L>[]) new Reference[length + 1]; // nulls
                    System.arraycopy(srcRefs, 0, destRefs, 0, length); // native copy
                    destRefs[length] = weakRef; // at end
                    vWeakListeners = destRefs;
                }
            }
        }

        /**
         * @param listenable should later be .removeListener() to avoid CPU or memory leaks.
         */
        @Override
        public void addListener(final L listenable) {
            add(listenable, false);
        }

        /**
         * Weakly referenced so does not CPU/mem leak when no
         * other Strongly reachable objects refer to the listener.
         *
         * @param listenable
         */
        @Override
        public void addListenerWeakly(final L listenable) {
            add(listenable, true);
        }

        /**
         * Unsubscribe from notifications.
         * Note: calling with 'null' will clean-up the weakly referenced array but this is not necessary as
         * the Enumeration will skip them as needed.
         *
         * @param listenable removal happens automatically by Weak ref when no other Strongly reachable objects refer to it.
         */
        @SuppressWarnings("unchecked")
        @Override
        synchronized public void removeListener(final L listenable) {
            if (vNoGcListeners != null) {
                if (vNoGcListeners.length > 1) {
                    final NotificationListenable[] srcArray = vNoGcListeners;
                    final int atIndex = Util.indexOfIdentity(listenable, srcArray);
                    if (atIndex != Util.INVALID_INDEX) {   // got one, perform slice that removes identity
                        final int lengthMinusOne = srcArray.length - 1;
                        final NotificationListenable[] destArray = new NotificationListenable[lengthMinusOne];
                        if (atIndex != lengthMinusOne)
                            System.arraycopy(srcArray, atIndex + 1, destArray, atIndex, lengthMinusOne - atIndex); // fill in from after removeRef
                        if (atIndex != 0)
                            System.arraycopy(srcArray, 0, destArray, 0, atIndex); // fill in from before removeRef
                        vNoGcListeners = destArray;
                        return;
                    }
                } else if (vNoGcListeners[0] == listenable) {   // length must be 1
                    vNoGcListeners = null; // safely releases array while other observers may still be iterating thru it
                    return;
                }
                // else might be weakly listening
            }
            // else might be weakly listening

            if (vWeakListeners != null) {
                if (vWeakListeners.length > 1) {   // safe to assume the listener is present, rebuild the array without it or any weakly GC'd refs
                    final Reference<L>[] srcRefs = vWeakListeners;
                    final List<Reference<L>> refList = new ArrayList<Reference<L>>(srcRefs.length);
                    // else drop the ref-to-remove or weakly GC'd ref by not adding
                    Arrays.stream(srcRefs).forEach(ref -> {
                        final L listener = ref.get();
                        if (listener != listenable && listener != null) {
                            refList.add(ref);
                        }
                    });

                    vWeakListeners = !refList.isEmpty()
                            ? refList.toArray(new Reference[0])
                            : null;
                } else if (vWeakListeners[0].get() == listenable) {   // length must be 1
                    vWeakListeners = null; // safely releases array while other observers may still be iterating thru it
                }
                // else ignore
            }
            // else equivalent to length=0
        }

        /**
         * Safely releases array while other observers may still be iterating thru it.
         */
        synchronized public void clearAllListeners() {
            vWeakListeners = null;
            vNoGcListeners = null;
        }

        synchronized public boolean hasAnyListeners() {
            return vWeakListeners != null || vNoGcListeners != null;
        }

        /**
         * Visitor factory, does not incur an allocation if no listeners have been added.
         * Derived class caller does -not- have to check for 'null' listeners because they will be skipped.
         * Does not implement Iterable because we do not want .remove() exposed.
         *
         * @return all listeners
         */
        @SuppressWarnings("unchecked")
        synchronized public Enumeration<L> createEnumeration() {
            return (hasAnyListeners())
                    ? new ListenerEnumeration<L>(this)
                    : (Enumeration<L>) EMPTY_ENUMERATION;
        }


        // ================================================================================

        /**
         * Visitor pattern to combine both the Notifier's strongly and weakly retained listeners.
         *
         * @param <L>
         */
        static private class ListenerEnumeration<L extends NotificationListenable>
                implements Enumeration<L> {
            /**
             * 'static' so can not be genericized to <L>.
             */
            static final private NotificationListenable[] NO_LISTENERS = new NotificationListenable[0];
            static final private Reference<?>[] NO_REFS = new Reference[0];

            /**
             * Compiler enforces Java array covariance.  Can not be declared invariant with <CODE>L[]</CODE>.
             */
            final private NotificationListenable[] fNoGcListeners;
            private int mNoGcIndex = 0;

            final private Reference<L>[] fWeakRefListeners;
            private int mWeakRefIndex = 0;

            /**
             * Commit to hard reference here any promised next listenable.  This allowed dropping the NO-OP Listenable paradigm completely.
             */
            private L mListener;

            @SuppressWarnings("unchecked")
            private ListenerEnumeration(final ANotifier<L> notifier) {
                fNoGcListeners = (notifier.vNoGcListeners != null) ? notifier.vNoGcListeners : NO_LISTENERS;
                fWeakRefListeners = (notifier.vWeakListeners != null) ? notifier.vWeakListeners : (Reference<L>[]) NO_REFS;

                mListener = hardReferenceNext(); // needed for staging .hasMoreElements()
            }

            /**
             * Listener notifications are expected to be invoked from within a single thread, i.e. not concurrently.
             *
             * @return via the stack makes a weak reference a hard reference
             */
            @SuppressWarnings("unchecked")
            private L hardReferenceNext() {
                if (mNoGcIndex < fNoGcListeners.length) {   // take a 'cast' hit
                    return (L) fNoGcListeners[mNoGcIndex++]; // post incr
                }

                while (mWeakRefIndex < fWeakRefListeners.length) {   // when weakly GC'd .get() returns 'null'
                    final L listener = fWeakRefListeners[mWeakRefIndex++].get(); // post incr
                    if (listener != null) return listener;
                }

                return null; // .nextElement() will throw the exception
            }

            /**
             * @return 'true' if more elements
             */
            @Override
            public boolean hasMoreElements() {
                return mListener != null;
            }

            /**
             * @return guaranteed to be non-'null'
             */
            @Override
            public L nextElement() {
                final L listener = mListener;
                if (listener == null) throw new NoSuchElementException();
                mListener = hardReferenceNext(); // retain locally
                return listener;
            }
        }
    }


    // ================================================================================

    /**
     * Example of Notifications with no arguments.
     */
    static private class Notifier_NoArgument extends ANotifier<NoArgumentListenable> {
        synchronized public void causeNotification() {
            final Enumeration<NoArgumentListenable> enumerati = createEnumeration();
            while (enumerati.hasMoreElements()) {
                enumerati.nextElement().listen();
            }
        }
    }


    // ================================================================================

    /**
     * Example of one argument notifications
     * where the listener is automatically initialized.
     *
     * @param <E> event of class type will be inferred
     */
    static public class Notifier<E> extends ANotifier<OneArgumentListenable<E>> {
        final private Initializable<E> fInitializable;

        /**
         * No initialization event context.
         */
        public Notifier() {
            this(null);
        }

        /**
         * Provide an initialization event context.
         *
         * @param initializable automatic cold-starts new listeners only when not 'null'
         */
        public Notifier(final Initializable<E> initializable) {
            fInitializable = initializable;
        }

        /**
         * Anti-pattern, why you no copy-pasta?
         *
         * @param listenable
         */
        private void contextualize(final OneArgumentListenable<E> listenable) {
            if (fInitializable != null) {   // cold-start
                listenable.listen(fInitializable.provideInitialEvent());
            }
        }

        /**
         * Subscribe to notifications.
         *
         * @param listenable
         */
        @Override
        synchronized public void addListener(final OneArgumentListenable<E> listenable) {
            contextualize(listenable);
            super.addListener(listenable);
        }

        /**
         * Subscribe to notifications weakly.
         * I.e. for as long as the instance of L's implementor is strongly referenced outside of the Notifier.
         * Works well for Swing components but do not use with non-retained Anonymous Class objects.
         * Those would be immediately GC'd.
         *
         * @param listenable will automatically stop being notified when the reference count to this instance (it) drops to zero
         */
        @Override
        synchronized public void addListenerWeakly(final OneArgumentListenable<E> listenable) {
            contextualize(listenable);
            super.addListenerWeakly(listenable);
        }

        synchronized public void causeNotification(final E event) {
            final Enumeration<OneArgumentListenable<E>> enumerati = createEnumeration();
            while (enumerati.hasMoreElements()) {
                enumerati.nextElement().listen(event);
            }
        }


        // ================================================================================

        /**
         * Cold-start an listener.
         *
         * @param <E> event of class type will be inferred
         */
        public interface Initializable<E> {
            E provideInitialEvent();
        }
    }


    // ================================================================================

    /**
     * Example of two argument notifications.
     *
     * @param <S> source of class type will be inferred
     * @param <E> event of class type will be inferred
     */
    static private class Notifier_TwoArgument<S, E> extends ANotifier<TwoArgumentListenable<S, E>> {
        synchronized public void causeNotification(final S source, final E event) {
            final Enumeration<TwoArgumentListenable<S, E>> enumerati = createEnumeration();
            while (enumerati.hasMoreElements()) {
                enumerati.nextElement().listen(source, event);
            }
        }
    }


    // ================================================================================

    /**
     * Tagging interface for starting a Listenable.
     */
    public interface NotificationListenable {
    }


    // ================================================================================

    /**
     * No argument example analogous to an Observer.
     */
    private interface NoArgumentListenable extends NotificationListenable {
        void listen();
    }


    // ================================================================================

    /**
     * One argument example analogous to an Observer.
     *
     * @param <E> event of class type will be inferred
     */
    public interface OneArgumentListenable<E> extends NotificationListenable {
        void listen(E event);
    }


    // ================================================================================

    /**
     * Two argument example analogous to an Observer.
     *
     * @param <S> source of class type will be inferred
     * @param <E> event of class type will be inferred
     */
    private interface TwoArgumentListenable<S, E> extends NotificationListenable {
        void listen(S source, E event);
    }
}
