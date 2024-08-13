package oz.zomg.jport.gui.table;

import oz.zomg.jport.TheApplication;
import oz.zomg.jport.common.Elemental;
import oz.zomg.jport.common.Elemental.EElemental;
import oz.zomg.jport.common.Providers_.ClassProvidable;
import oz.zomg.jport.common.Providers_.EnabledProvidable;
import oz.zomg.jport.common.Providers_.WidthProvidable;
import oz.zomg.jport.common.StringsUtil_;
import oz.zomg.jport.common.gui.AEnumTableModel_Array;
import oz.zomg.jport.ports.PortsVariants;
import oz.zomg.jport.type.Portable;


/**
 * UI for changing desired variants.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
@SuppressWarnings("serial")
public class TableModel_Variants extends AEnumTableModel_Array<String, TableModel_Variants.EColumn> implements Elemental.Listenable<Portable> {

    final private PortsVariants fPortsVariants = TheApplication.INSTANCE.getPortsCatalog().getPortsVariants(); // alias
    final private boolean fIsAssignmentLocked;
    /**
     * Mutable for .actionPerformed() and follows table selection via .notify().  Must start with 'null'
     */
    transient private Portable mAssignedPort = null;

    /**
     * @param assignedPort is the target model.  Use Portable.NONE to signal driven by user's table selection.
     */
    public TableModel_Variants(final Portable assignedPort) {
        super(String.class, EEditable.ENABLE, ERowSelection.NO_SELECTION, EColorize.DISABLE, EColumn.values());

        if (assignedPort == null) throw new NullPointerException();

        fIsAssignmentLocked = assignedPort != Portable.NONE;

        this.toggelSortColumn(EColumn.VARIANT);

        // listener
        TheApplication.INSTANCE.getCrudNotifier().addListenerWeakly(this); // automatically calls .notify() and updates mAssignedPort conforming the view
    }

    private void setPort(final Portable port) {
        if (port == Portable.NONE) {
            this.setRows(StringsUtil_.NO_STRINGS);
        } else {
            this.setRows(port.getVariants().clone());
            fireTableDataChanged(); // update check boxes
        }
    }

    /**
     * Follows main table selection or initializes the locked port assignment.
     *
     * @param elemental action
     * @param port      of marking view
     */
    @Override
    public void notify(final EElemental elemental, final Portable port) {
        switch (elemental) {
            case RETRIEVED: {
                if (!fIsAssignmentLocked || mAssignedPort == null) {
                    mAssignedPort = port;
                    setPort(port);
                }
            }
            break;

            case UPDATED: {
                if (mAssignedPort == port) {   // filtered out non-related updates
                    setPort(port);
                }
            }
            break;
        }
    }

    /**
     * @param variant    row
     * @param columnEnum
     * @return placed in table
     */
    @Override
    public Object getValueOf(final String variant, final EColumn columnEnum) {
        switch (columnEnum) {
            case dWidth:
                return fPortsVariants.isApplicableVariant(mAssignedPort, variant);
            case VARIANT:
                return variant;

            default:
                return "ERR";
        }
    }

    /**
     * @param value      to update model with
     * @param variant    row
     * @param columnEnum
     */
    @Override
    public void setValueOf(final Object value, final String variant, final EColumn columnEnum) {
        switch (columnEnum) {
            case dWidth: {
                final boolean isInstalling = ((Boolean) value).booleanValue();
                fPortsVariants.setVariant(mAssignedPort, variant, isInstalling);
            }
            break;

            default:
                break;
        }
    }

    enum EColumn implements WidthProvidable, EnabledProvidable, ClassProvidable {
        dWidth(18) {
            @Override
            public Class<?> provideClass() {
                return Boolean.class;
            }

            @Override
            public boolean provideIsEnabled() {
                return true;
            }
        }  // check box
        , VARIANT(-1);

        private final int fWidth;

        EColumn(final int width) {
            fWidth = width;
        }

        @Override
        public int provideWidth() {
            return fWidth;
        }

        @Override
        public boolean provideIsEnabled() {
            return false;
        }

        @Override
        public Class<?> provideClass() {
            return Object.class;
        }
    }
}
