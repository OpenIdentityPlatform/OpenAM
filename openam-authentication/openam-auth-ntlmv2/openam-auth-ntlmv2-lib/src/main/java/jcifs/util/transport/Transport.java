package jcifs.util.transport;

import jcifs.util.LogStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * This class simplifies communication for protocols that support
 * multiplexing requests. It encapsulates a stream and some protocol
 * knowledge (provided by a concrete subclass) so that connecting,
 * disconnecting, sending, and receiving can be syncronized
 * properly. Apparatus is provided to send and receive requests
 * concurrently.
 */

public abstract class Transport implements Runnable {

    static int id = 0;
    static LogStream log = LogStream.getInstance();

    public static int readn( InputStream in,
                byte[] b,
                int off,
                int len ) throws IOException {
        int i = 0, n = -5;

        while (i < len) {
            n = in.read( b, off + i, len - i );
            if (n <= 0) {
                break;
            }
            i += n;
        }

        return i;
    }

    /* state values
     * 0 - not connected
     * 1 - connecting
     * 2 - run connected
     * 3 - connected
     * 4 - error
     */
    int state = 0;

    String name = "Transport" + id++;
    Thread thread;
    TransportException te;

    protected HashMap response_map = new HashMap( 4 );

    protected abstract void makeKey( Request request ) throws IOException;
    protected abstract Request peekKey() throws IOException;
    protected abstract void doSend( Request request ) throws IOException;
    protected abstract void doRecv( Response response ) throws IOException;
    protected abstract void doSkip() throws IOException;

    public synchronized void sendrecv( Request request,
                    Response response,
                    long timeout ) throws IOException {
            makeKey( request );
            response.isReceived = false;
            try {
                response_map.put( request, response );
                doSend( request );
                response.expiration = System.currentTimeMillis() + timeout;
                while (!response.isReceived) {
                    wait( timeout );
                    timeout = response.expiration - System.currentTimeMillis();
                    if (timeout <= 0) {
                        throw new TransportException( name +
                                " timedout waiting for response to " +
                                request );
                    }
                }
            } catch( IOException ioe ) {
                if (log.level > 2)
                    ioe.printStackTrace( log );
                try {
                    disconnect( true );
                } catch( IOException ioe2 ) {
                    ioe2.printStackTrace( log );
                }
                throw ioe;
            } catch( InterruptedException ie ) {
                throw new TransportException( ie );
            } finally {
                response_map.remove( request );
            }
    }
    private void loop() {
        while( thread == Thread.currentThread() ) {
            try {
                Request key = peekKey();
                if (key == null)
                    throw new IOException( "end of stream" );
                synchronized (this) {
                    Response response = (Response)response_map.get( key );
                    if (response == null) {
                        if (log.level >= 4)
                            log.println( "Invalid key, skipping message" );
                        doSkip();
                    } else {
                        doRecv( response );
                        response.isReceived = true;
                        notifyAll();
                    }
                }
            } catch( Exception ex ) {
                String msg = ex.getMessage();
                boolean timeout = msg != null && msg.equals( "Read timed out" );
                /* If just a timeout, try to disconnect gracefully
                 */
                boolean hard = timeout == false;

                if (!timeout && log.level >= 3)
                    ex.printStackTrace( log );

                try {
                    disconnect( hard );
                } catch( IOException ioe ) {
                    ioe.printStackTrace( log );
                }
            }
        }
    }

    /* Build a connection. Only one thread will ever call this method at
     * any one time. If this method throws an exception or the connect timeout
     * expires an encapsulating TransportException will be thrown from connect
     * and the transport will be in error.
     */

    protected abstract void doConnect() throws Exception;

    /* Tear down a connection. If the hard parameter is true, the diconnection
     * procedure should not initiate or wait for any outstanding requests on
     * this transport.
     */

    protected abstract void doDisconnect( boolean hard ) throws IOException;

    public synchronized void connect( long timeout ) throws TransportException {
        try {
            switch (state) {
                case 0:
                    break;
                case 3:
                    return; // already connected
                case 4:
                    state = 0;
                    throw new TransportException( "Connection in error", te );
                default:
                    TransportException te = new TransportException( "Invalid state: " + state );
                    state = 0;
                    throw te;
            }

            state = 1;
            te = null;
            thread = new Thread( this, name );
            thread.setDaemon( true );

            synchronized (thread) {
                thread.start();
                thread.wait( timeout );          /* wait for doConnect */

                switch (state) {
                    case 1: /* doConnect never returned */
                        state = 0;
                        thread = null;
                        throw new TransportException( "Connection timeout" );
                    case 2:
                        if (te != null) { /* doConnect throw Exception */
                            state = 4;                        /* error */
                            thread = null;
                            throw te;
                        }
                        state = 3;                         /* Success! */
                        return;
                }
            }
        } catch( InterruptedException ie ) {
            state = 0;
            thread = null;
            throw new TransportException( ie );
        } finally {
            /* This guarantees that we leave in a valid state
             */
            if (state != 0 && state != 3 && state != 4) {
                if (log.level >= 1)
                    log.println("Invalid state: " + state);
                state = 0;
                thread = null;
            }
        }
    }
    public synchronized void disconnect( boolean hard ) throws IOException {
        IOException ioe = null;

        switch (state) {
            case 0: /* not connected - just return */
                return;
            case 2:
                hard = true;
            case 3: /* connected - go ahead and disconnect */
                if (response_map.size() != 0 && !hard) {
                    break; /* outstanding requests */
                }
                try {
                    doDisconnect( hard );
                } catch (IOException ioe0) {
                    ioe = ioe0;
                }
            case 4: /* in error - reset the transport */
                thread = null;
                state = 0;
                break;
            default:
                if (log.level >= 1)
                    log.println("Invalid state: " + state);
                thread = null;
                state = 0;
                break;
        }

        if (ioe != null)
            throw ioe;
    }
    public void run() {
        Thread run_thread = Thread.currentThread();
        Exception ex0 = null;

        try {
            /* We cannot synchronize (run_thread) here or the caller's
             * thread.wait( timeout ) cannot reaquire the lock and
             * return which would render the timeout effectively useless.
             */
            doConnect();
        } catch( Exception ex ) {
            ex0 = ex; // Defer to below where we're locked
            return;
        } finally {
            synchronized (run_thread) {
                if (run_thread != thread) {
                    /* Thread no longer the one setup for this transport --
                     * doConnect returned too late, just ignore.
                     */
                    if (ex0 != null) {
                        if (log.level >= 2)
                            ex0.printStackTrace(log);
                    }
                    return;
                }
                if (ex0 != null) {
                    te = new TransportException( ex0 );
                }
                state = 2; // run connected
                run_thread.notify();
            }
        }

        /* Proccess responses
         */
        loop();
    }

    public String toString() {
        return name;
    }
}
