package oz.zomg.jport.gui.component;

import javax.swing.*;


/**
 * Read-only array for facilitating +100K row JList models.
 * Note: In addition to rows, a JList can also appear as a matrix or columns.
 *
 * @param <E> element of class type
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class JListModel_Array<E> extends AbstractListModel {
    final private E[] fElements;

    public JListModel_Array(final E[] elements) {
        if (elements == null) throw new NullPointerException();

        fElements = elements;

        // auto called when .setModel(this) -> fireContentsChanged( this, index0, index1 );
    }

    @Override
    public int getSize() {
        return fElements.length;
    }

    @Override
    public Object getElementAt(int index) {
        return fElements[index];
    }
}
