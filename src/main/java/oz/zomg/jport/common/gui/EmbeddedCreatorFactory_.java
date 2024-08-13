package oz.zomg.jport.common.gui;

import oz.zomg.jport.common.Interfacing_.Creatable;
import oz.zomg.jport.common.gui.JScrollPaneFactory_.EScrollPolicy;

import javax.swing.*;
import java.awt.*;


/**
 * Defer creation of a Component.
 * <H3><I><FONT color="#770000">Subset of original source.</FONT></I></H3>
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class EmbeddedCreatorFactory_ {
    static // initializer block
    {
    }

    private EmbeddedCreatorFactory_() {
    }

    /**
     * Factory of future.
     *
     * @param futureComponentCreator implementor lazily creates a Component. Defers until wrapper component is made visible.
     * @return non-'null' guaranteed here as JTabbedPane source says it can not really accommodate/contain 'null' Components
     */
    static public Component create(final Creatable<Component> futureComponentCreator) {
        return create(EScrollPolicy.VERT_NONE__HORIZ_NONE, futureComponentCreator);
    }

    /**
     * Factory of future.
     *
     * @param scrollPolicy           wrapper scroll pane
     * @param futureComponentCreator implementor lazily creates a Component. Defers until wrapper component is made visible.
     * @return non-'null' guaranteed here as JTabbedPane source says it can not really accommodate/contain 'null' Components
     */
    @SuppressWarnings("serial")
    static public Component create(final EScrollPolicy scrollPolicy, final Creatable<Component> futureComponentCreator) {
        return new AContainer_EmbeddedCreator(scrollPolicy) // anonymous class
        {
            @Override
            public Component create() {
                final Component component = futureComponentCreator.create();
                return (component != null)
                        ? component
                        : new Component() {
                    static final long serialVersionUID = -1L;
                }; // anonymous class
            }
        };
    }


    // ================================================================================

    /**
     * A Container embeddable Component future.
     * Instantiates a stub Container for anonymously overriding .create().
     */
    static abstract public class AContainer_EmbeddedCreator extends Container
            implements Creatable<Component> {
        final private EScrollPolicy fScrollPolicy;

        private boolean mIsCancelled = false;

        public AContainer_EmbeddedCreator() {
            this(EScrollPolicy.VERT_NONE__HORIZ_NONE); // none is none
        }

        /**
         * @param scrollPolicy if any JScollPane to wrap a view of the future component
         */
        public AContainer_EmbeddedCreator(final EScrollPolicy scrollPolicy) {
            fScrollPolicy = scrollPolicy;
            setLayout(new GridLayout(0, 1));
        }

        /**
         * Cancel the future creation of the Component.
         * This method is required by the Future design pattern.
         */
        public void cancel() {
            mIsCancelled = true;
        }

        /**
         * This method is required by the Future design pattern.
         *
         * @return 'true' if future component was created or if the future was canceled
         */
        public boolean isDone() {
            return mIsCancelled || getComponentCount() > 0;
        }

        /**
         * Enables deferred / lazy instantiation of the tab contents until tab is selected.
         * When setting visible, this wrapper Container will, only once, create a single Component.
         * Note: Other option for creation, besides the tamperation of this method, would be a HierarchyListener.
         *
         * @param visible
         */
        @Override
        public void setVisible(boolean visible) {
            if (visible && !isDone()) {   // this wrapper Container will only create a single Component
                final Component embedComponent = create();

                if (fScrollPolicy == EScrollPolicy.VERT_NONE__HORIZ_NONE) {
                    add(embedComponent);
                } else {   // scroll wrapper desired
                    final JScrollPane scrollPane = JScrollPaneFactory_.create(embedComponent, fScrollPolicy);
                    add(scrollPane);
                }
            }

            super.setVisible(visible);
        }
    }
}
