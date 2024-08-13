// default package

import oz.zomg.jport.TheApplication;


/**
 * Main entry point for project.
 * A class needed here in the default package to give the executing .JAR
 * in the MacOSX application Dock a more sensible name.
 * <p>
 * Call it JPortsUI if this project is to also support FreeBSD or NetBSD.
 * Call it MacNaptic if it can integrate with HomeBrew package manager for the Mac.
 * JMacPortUI? PortExplorer? PortSnort?
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareaAlike 3.0 Unported License</a>.</SMALL>
 */
public class JPortsUI {

    private JPortsUI() {
    }

    public static void main(String[] args) {
        TheApplication.INSTANCE.init();
    }
}
