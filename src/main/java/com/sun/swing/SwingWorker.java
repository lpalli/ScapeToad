package com.sun.swing;

import javax.swing.SwingUtilities;

/**
 * This is the 3rd version of SwingWorker (also known as SwingWorker 3), an
 * abstract class that you subclass to perform GUI-related work in a dedicated
 * thread. For instructions on using this class, see:
 * 
 * http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html
 * 
 * Note that the API changed slightly in the 3rd version: You must now invoke
 * start() on the SwingWorker after creating it.
 */
public abstract class SwingWorker {

    /**
     * The value produced by worker thread.
     */
    private Object iValue;

    /**
     * Class to maintain reference to current worker thread under separate
     * synchronization control.
     */
    private static class ThreadVar {

        /**
         * The thread
         */
        private Thread iThread;

        /**
         * Constructor
         * 
         * @param aThread
         *            the thread
         */
        protected ThreadVar(Thread aThread) {
            iThread = aThread;
        }

        /**
         * Get the thread.
         * 
         * @return the thread
         */
        protected synchronized Thread get() {
            return iThread;
        }

        /**
         * Clear the thread.
         */
        protected synchronized void clear() {
            iThread = null;
        }
    }

    /**
     * The thread reference
     */
    protected ThreadVar threadVar;

    /**
     * Get the value produced by the worker thread, or null if it hasn't been
     * constructed yet.
     * 
     * @return the value
     */
    protected synchronized Object getValue() {
        return iValue;
    }

    /**
     * Set the value produced by worker thread
     * 
     * @param aValue
     *            the value
     */
    protected synchronized void setValue(Object aValue) {
        iValue = aValue;
    }

    /**
     * Compute the value to be returned by the <code>get</code> method.
     * 
     * @return an object
     */
    protected abstract Object construct();

    /**
     * Called on the event dispatching thread (not on the worker thread) after
     * the <code>construct</code> method has returned.
     */
    protected void finished() {
        // Nothing to do
    }

    /**
     * A new method that interrupts the worker thread. Call this method to force
     * the worker to stop what it's doing.
     */
    public void interrupt() {
        Thread thread = threadVar.get();
        if (thread != null) {
            thread.interrupt();
        }
        threadVar.clear();
    }

    /**
     * A method to know whether the thread is running or not.
     * 
     * @return <code>true</code> if it is running
     */
    public boolean isRunning() {
        Thread thread = threadVar.get();
        if (thread == null) {
            return false;
        }

        if (thread.getState() == Thread.State.NEW) {
            return false;
        }
        return true;
    }

    /**
     * Return the value created by the <code>construct</code> method. Returns
     * null if either the constructing thread or the current thread was
     * interrupted before a value was produced.
     * 
     * @return the value created by the <code>construct</code> method
     */
    public Object get() {
        while (true) {
            Thread thread = threadVar.get();
            if (thread == null) {
                return getValue();
            }
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // propagate
                return null;
            }
        }
    }

    /**
     * Start a thread that will call the <code>construct</code> method and then
     * exit.
     */
    public SwingWorker() {
        final Runnable doFinished = new Runnable() {
            @Override
            public void run() {
                finished();
            }
        };

        Runnable doConstruct = new Runnable() {
            @Override
            public void run() {
                try {
                    setValue(construct());
                } catch (OutOfMemoryError memEx) {
                    Thread.currentThread().interrupt();
                } finally {
                    threadVar.clear();
                }

                SwingUtilities.invokeLater(doFinished);
            }
        };

        threadVar = new ThreadVar(new Thread(doConstruct));
    }

    /**
     * Start the worker thread.
     */
    public void start() {
        Thread thread = threadVar.get();
        if (thread != null) {
            thread.start();
        }
    }
}