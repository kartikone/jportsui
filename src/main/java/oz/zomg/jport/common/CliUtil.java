package oz.zomg.jport.common;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Command Line Interface command execution with Input/Output/Exception/Completion monitoring and threading.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class CliUtil {
    /**
     * Not using "/bin/sh" as this default shell -may- not permit Bash syntax or Bash built-in cmds.
     */
    static final public String UNIX_BIN_BASH = "/bin/bash";

    /**
     * Treat next Bash arg as the cmd string to shell through.
     */
    static final public String BASH_OPT_C = "-c";

    /**
     * Whether to print a sorted host OS ProcessBuilder environment to the NetBeans console.
     */
    static final private boolean _CONSOLE_OUT_PROCESS_ENVIRONMENT = false;

    /**
     * When DEBUG true shows CLI command and Errors on console.
     */
    static final private boolean DEBUG = false;

    static // initializer block
    {
        if (_CONSOLE_OUT_PROCESS_ENVIRONMENT) {
            final ProcessBuilder pb = new ProcessBuilder("cd".split(" ")); // need an object ref to get at .environment()
            final Map<String, String> env = pb.environment();
            final String[] keys = StringsUtil_.toStrings(env.keySet());

            for (final String key : StringsUtil_.sort(true, keys)) {
                System.out.println(key + " = " + env.get(key));
            }
        }
    }

    private CliUtil() {
    }

    /**
     * Executes the CLI commands but **BLOCKS**.
     * Non-listening, fire-and-forget style command.
     *
     * @param cliCommands
     * @return standard output from process exec
     */
    static public String[] executeCommand(final String... cliCommands) {
        return executeCommand(new Adapter(), cliCommands);
    }

    /**
     * Executes the CLI commands but **BLOCKS**.
     * Optionally invokes your listener during at a Swing GUI safe time.
     *
     * @param listener
     * @param cliCommands
     * @return standard output from process exec
     */
    static public String[] executeCommand(final Listener listener, final String... cliCommands) {
        final CliProcess cliProcess = new CliProcess(listener, cliCommands);
        final int resultCode = cliProcess.execute();
        return StringsUtil_.toStrings(cliProcess.getOutputFromCliList());
    }

    /**
     * Concurrent execution.
     * Non-blocking, non-listening, fire-and-forget style command.
     *
     * @param cliCommands
     * @return started thread running the CLI command.  Also for 'synchronize' or .join() or .isAlive()
     */
    static public Thread forkCommand(final String... cliCommands) {
        return forkCommand(new Adapter(), cliCommands);
    }

    /**
     * Concurrent execution.
     * Non-blocking deferred execution of CLI Commands in a separate fork.
     * Invokes your listener incrementally and optionally at a Swing GUI safe time.
     *
     * @param listener    callback can be Listener.EMPTY
     * @param cliCommands if "/bin/sh", "-c", "xxx xxx xxx" is not available then the cliCommands args MUST have no space 0x20 characters in them
     *                    *** DO -NOT- PUT QUOTES AROUND THE CMD LIKE "\"xxx xxx\"", instead use "locate java" or "locate TWO\\ WORDS.txt"
     * @return started thread running the CLI command.  Also for 'synchronize' or .join() or .isAlive()
     */
    static public Thread forkCommand(final Listener listener, final String... cliCommands) {
        if (listener == null) throw new NullPointerException();

        // para-lambda expression
        final Runnable runnable = new Runnable() // anonymous class
        {
            @Override
            public void run() {
                executeCommand(listener, cliCommands);
            }
        };
        final Thread thread = new Thread(runnable, CliProcess.class.getCanonicalName());

        // default is daemon=false, should fully complete so that command is atomic
        thread.start();
        return thread;
    }

    /**
     * Relay abnormal execution.
     * Swing thread safe.
     *
     * @param ex
     * @param listener
     */
    static private void _doException(final Exception ex, final Listener listener) {
        if (DEBUG) ex.printStackTrace();

        if (!listener.cliProcessListenerNeedsSwingDeferment() || SwingUtilities.isEventDispatchThread()) {   // immediate
            listener.cliProcessException(ex);
        } else {   // deferred to Event Dispatch thread
            SwingUtilities.invokeLater(new Runnable() // anonymous class
            {
                @Override
                public void run() {
                    listener.cliProcessException(ex);
                }
            });
        }
    }


    // ================================================================================

    /**
     * Blocks until CLI process is fully executed while updating listeners at at a GUI safe time.
     * <p>
     * Unless explicitly passed to '/bin/bash -c', many facilities are not available.
     * Failure ex. <CODE> /bin/echo "" | /usr/bin/mysql --version --quick > /home/raid/sql-version.txt</CODE>
     * Success ex. <CODE> /bin/bash -c /bin/echo password1 | /usr/bin/sudo -S /bin/ls /bin/</CODE>
     */
    static private class CliProcess {
        final private Listener fListener;
        final private ProcessBuilder fProcessBuilder;

        final List<String> fOutputFromCliList = new ArrayList<String>();
        final List<String> fErrorFromCliList = new ArrayList<String>();

        /**
         * Constructor **BLOCKS** to run and wait for command completion.
         *
         * @param listener
         * @param cliCommands ex.
         */
        private CliProcess(final Listener listener, final String... cliCommands) {
            if (listener == null) throw new NullPointerException();
            if (cliCommands.length == 0) throw new IllegalArgumentException();

            fListener = listener;

            if (DEBUG) {
                System.out.print("  TO CLI --> ");
                for (final String cmd : cliCommands) System.out.print(cmd + " "); // same line
                System.out.println();
            }

            final File fsRootFilePath = File.listRoots()[0]; // formerly new File( "/" )
            fProcessBuilder = new ProcessBuilder(cliCommands); // non-[] .exec() ends up calling StringTokeinzer to split on spaces, TAB, CR, etc breaking the embedded space, ex. mysql --execute="SELECT NOW()"
            fProcessBuilder.directory(fsRootFilePath); // set working directory to root level, away from .jar folder
        }

        private int execute() {
            int resultCode = -1;
            InputStream inputStream = null; // JDK6 does not have try(=resource=)

            try {
                // builds the Process
                final Process process = fProcessBuilder.start(); // does not block
                inputStream = process.getInputStream();

                // must run a thread to receive potentially lots of input from the process
                final Thread inputDrainThread = new Thread_InputStreamDrain(fListener, inputStream, fOutputFromCliList);
                inputDrainThread.start();

                final long startMillisec = System.currentTimeMillis();

                // @link "http://www.javamex.com/tutorials/threads/yield.shtml"
                // required for proper fork staging, failed on Win/Mac as Linux robins -all- Threads, whereas Mac/Win does not
                Thread.yield(); // did not want to .sleep(1) if avoidable however .yield() on some JVMs is implemented as .sleep( 0 )

                resultCode = process.waitFor(); // **BLOCKS**
                doError(process); // is still normal execution

                synchronized (inputDrainThread) {   // wait for drainage completion to notify of released lock
                    while (inputDrainThread.isAlive() && !Thread.currentThread().isInterrupted()) {   // wait for drainage to complete on Win/Mac, their thread scheduler does not .yield() like Linux
                        inputDrainThread.wait(2000);
                    }
                }

                final long elapsedMillisec = System.currentTimeMillis() - startMillisec;
                if (DEBUG)
                    System.out.println(CliProcess.class.getSimpleName() + ".execute()= " + elapsedMillisec + " ms <-" + StringsUtil_.concatenate(" ", fProcessBuilder.command()));

                // completed
                final String[] outputLines = StringsUtil_.toStrings(fOutputFromCliList);
                final String[] errorLines = StringsUtil_.toStrings(fErrorFromCliList);

                if (!fListener.cliProcessListenerNeedsSwingDeferment() || SwingUtilities.isEventDispatchThread()) {   // immediate
                    fListener.cliProcessCompleted(resultCode, outputLines, errorLines);
                } else {   // deferred to Event Dispatch thread
                    final int theResultCode = resultCode;
                    SwingUtilities.invokeLater(new Runnable() // anonymous class
                    {
                        @Override
                        public void run() {
                            fListener.cliProcessCompleted(theResultCode, outputLines, errorLines);
                        }
                    });
                }
            } catch (IOException ex) {   // abnormal execution
                resultCode = -1;
                _doException(ex, fListener);
            } catch (InterruptedException ex2) {
                resultCode = -1;
                _doException(ex2, fListener);
            } finally {
                Util.close(inputStream);
            }

            return resultCode;
        }

        /**
         * Emit error reportage if any.
         * Swing thread safe.
         *
         * @param process
         * @throws IOException
         */
        private void doError(final Process process) throws IOException {   // decorator
            final BufferedReader errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String errLine = "";
            while ((errLine = errorStream.readLine()) != null) {   // display err output from CLI
                if (DEBUG) System.err.println("FROM CLI <-- " + errLine);

                if (!"Password:".equals(errLine)) {
                    fErrorFromCliList.add(errLine); // there is no trailing LF or CR to .trim()

                    if (!fListener.cliProcessListenerNeedsSwingDeferment() || SwingUtilities.isEventDispatchThread()) {   // immediate
                        fListener.cliProcessError(errLine);
                    } else {   // deferred to Event Dispatch thread
                        final String theLine = errLine; // alias
                        SwingUtilities.invokeLater(new Runnable() // anonymous class
                        {
                            @Override
                            public void run() {
                                fListener.cliProcessError(theLine);
                            }
                        });
                    }
                }
                // else special cased as this is not a real problem needing user attention
            }

            final int resultCode = process.exitValue();
            if (DEBUG && (resultCode != 0 || (errLine != null && !errLine.isEmpty())))
                System.err.println("RESULT=" + resultCode);

            Util.close(errorStream);
        }

        public List<String> getOutputFromCliList() {
            return fOutputFromCliList;
        }

        public List<String> getErrorFromCliList() {
            return fErrorFromCliList;
        }
    }


    // ================================================================================

    /**
     * Drains the Process input stream before the OS-JVM shared memory mechanism overflows.
     */
    static private class Thread_InputStreamDrain extends Thread {
        final private Listener fListener;
        final private InputStream fInputStream;
        final private List<String> fLineList;

        /**
         * @param listener
         * @param inputStream bufferedReader to InputStream
         * @param lineList    accumulated in here but if unneeded use 'null'
         */
        private Thread_InputStreamDrain
        (final Listener listener
                , final InputStream inputStream
                , final List<String> lineList
        ) {
            super(Thread_InputStreamDrain.class.getCanonicalName() + " <- " + Thread.currentThread().getName()); // name the thread, setDaemon() default=false

            fListener = listener;
            fInputStream = inputStream;
            fLineList = lineList;
        }

        /**
         * Swing thread safe.
         */
        @Override
        public void run() {
            synchronized (this) // acquire the monitor before the process.exec() caller starts
            {   // decorator
                final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fInputStream), 128 * 1024); // found that .ready()=false always

                try {
                    String outputLine;
                    while ((outputLine = bufferedReader.readLine()) != null) //... && this.isInterrupted() == false )
                    {   // consume stream
                        fLineList.add(outputLine); // there is no trailing LF or CR to .trim()

                        if (!fListener.cliProcessListenerNeedsSwingDeferment() || SwingUtilities.isEventDispatchThread()) {   // immediate
                            fListener.cliProcessOutput(outputLine);
                        } else {   // deferred to Event Dispatch thread
                            final String theLine = outputLine;
                            SwingUtilities.invokeLater(new Runnable() // anonymous class
                            {
                                @Override
                                public void run() {
                                    fListener.cliProcessOutput(theLine);
                                }
                            });
                        }
                    } // EOF signalled, done!
                } catch (final IOException ex) {   // not expected
                    _doException(ex, fListener);
                } finally {
                    Util.close(bufferedReader);
                    try {
                        bufferedReader.close();
                    } catch (IOException ex) {
                    }

                    this.notifyAll(); // Findbugs sniffs at .notify()
                }
            }
        }
    }


    // ================================================================================

    /**
     * Supports incremental output from the CLI process being executed.
     */
    public interface Listener {
        boolean cliProcessListenerNeedsSwingDeferment();

        void cliProcessException(final Exception ex);

        void cliProcessOutput(final String outputLine);

        void cliProcessError(final String errorLine);

        void cliProcessCompleted
                (final int resultCode
                        , final String[] cliOutputLines
                        , final String[] cliErrorLines
                );
    }


    // ================================================================================

    /**
     * "Does nothing" adapter methods.
     */
    static public class Adapter
            implements Listener {
        @Override
        public boolean cliProcessListenerNeedsSwingDeferment() {
            return false;
        }

        @Override
        public void cliProcessException(Exception ex) {
        }

        @Override
        public void cliProcessOutput(String outputLine) {
        }

        @Override
        public void cliProcessError(String errorLine) {
        }

        @Override
        public void cliProcessCompleted(int resultCode, String[] cliOutputLines, String[] cliErrorLines) {
        }
    }
}
