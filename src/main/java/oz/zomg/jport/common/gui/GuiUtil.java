package oz.zomg.jport.common.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Utilities for Swing and AWT Graphical User Interfaces.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class GuiUtil {
    static final public int GAP_PIXEL = 5;
    static final public Insets ZERO_INSET = new Insets(0, 0, 0, 0);

    static {
    }

    private GuiUtil() {
    }

// ENHANCE

    /**
     * Get all children from a safely-cast Container of a certain Class type.
     *
     * @param <T>            is inferred
     * @param ofClassType
     * @param fromContainers and their child sub-Containers
     * @return
     */
    @SuppressWarnings("unchecked")
    static public <T extends Component> Set<T> getChildren(final Class<T> ofClassType, final Container... fromContainers) {
        if (fromContainers.length == 0 || (fromContainers.length == 1 && fromContainers[0].getComponentCount() == 0))
            return Collections.emptySet();

        final Set<Component> allChildrenSet = new HashSet<Component>();
        _putAllChildren(allChildrenSet, fromContainers);

        // reduce
        final Set<T> filteredSet = new HashSet<T>();
        for (final Component child : allChildrenSet) {
            if (ofClassType.isInstance(child)) // note: polymorphic just like 'instanceof'
            {
                filteredSet.add((T) child);
            }
        }

        return filteredSet;
    }

    /**
     * Recursive inventory of all child Containers and Components using depth-first traversal of Containers.
     *
     * @param intoSet    also receives top-level parent and lower-level parent Containers
     * @param components
     */
    static private void _putAllChildren(final Set<Component> intoSet, final Component[] components) {
        if (components.length == 0) return; // all Swing components are Containers but most have no children

        for (final Component component : components) {
            if (component instanceof JMenu childMenu) // BUGFIX
            {
                _putAllChildren(intoSet, childMenu.getMenuComponents());
            } else if (component instanceof Container childContainer) {
                _putAllChildren(intoSet, childContainer.getComponents());
            }
        }

        intoSet.addAll(Arrays.asList(components)); // fast wrapper
    }
}
