/* jcifs smb client library in Java
 * Copyright (C) 2000  "Michael B. Allen" <jcifs at samba dot org>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jcifs.netbios;

import jcifs.Config;
import jcifs.util.Hexdump;
import jcifs.util.LogStream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.StringTokenizer;

class NameServiceClient implements Runnable {

    static final int DEFAULT_SO_TIMEOUT = 5000;
    static final int DEFAULT_RCV_BUF_SIZE = 576;
    static final int DEFAULT_SND_BUF_SIZE = 576;
    static final int NAME_SERVICE_UDP_PORT = 137;
    static final int DEFAULT_RETRY_COUNT = 2;
    static final int DEFAULT_RETRY_TIMEOUT = 3000;

    static final int RESOLVER_LMHOSTS = 1;
    static final int RESOLVER_BCAST   = 2;
    static final int RESOLVER_WINS    = 3;

    private static final int SND_BUF_SIZE = Config.getInt( "jcifs.netbios.snd_buf_size", DEFAULT_SND_BUF_SIZE );
    private static final int RCV_BUF_SIZE = Config.getInt( "jcifs.netbios.rcv_buf_size", DEFAULT_RCV_BUF_SIZE );
    private static final int SO_TIMEOUT = Config.getInt( "jcifs.netbios.soTimeout", DEFAULT_SO_TIMEOUT );
    private static final int RETRY_COUNT = Config.getInt( "jcifs.netbios.retryCount", DEFAULT_RETRY_COUNT );
    private static final int RETRY_TIMEOUT = Config.getInt( "jcifs.netbios.retryTimeout", DEFAULT_RETRY_TIMEOUT);
    private static final int LPORT = Config.getInt( "jcifs.netbios.lport", 0 );
    private static final InetAddress LADDR = Config.getInetAddress( "jcifs.netbios.laddr", null );
    private static final String RO = Config.getProperty( "jcifs.resolveOrder" );

    private static LogStream log = LogStream.getInstance();

    private final Object LOCK = new Object();

    private int lport, closeTimeout;
    private byte[] snd_buf, rcv_buf;
    private DatagramSocket socket;
    private DatagramPacket in, out;
    private HashMap responseTable = new HashMap();
    private Thread thread;
    private int nextNameTrnId = 0;
    private int[] resolveOrder;

    InetAddress laddr, baddr;

    NameServiceClient() {
        this( LPORT, LADDR );
    }
    NameServiceClient( int lport, InetAddress laddr ) {
        this.lport = lport;
        this.laddr = laddr;

        try {
            baddr = Config.getInetAddress( "jcifs.netbios.baddr",
                        InetAddress.getByName( "255.255.255.255" ));
        } catch( UnknownHostException uhe ) {
        }

        snd_buf = new byte[SND_BUF_SIZE];
        rcv_buf = new byte[RCV_BUF_SIZE];
        out = new DatagramPacket( snd_buf, SND_BUF_SIZE, baddr, NAME_SERVICE_UDP_PORT );
        in  = new DatagramPacket( rcv_buf, RCV_BUF_SIZE );

        if( RO == null || RO.length() == 0 ) {

            /* No resolveOrder has been specified, use the
             * default which is LMHOSTS,DNS,WINS,BCAST
             * LMHOSTS,BCAST,DNS if jcifs.netbios.wins has not
             * been specified.
             */

            if( NbtAddress.getWINSAddress() == null ) {
                resolveOrder = new int[2];
                resolveOrder[0] = RESOLVER_LMHOSTS;
                resolveOrder[1] = RESOLVER_BCAST;
            } else {
                resolveOrder = new int[3];
                resolveOrder[0] = RESOLVER_LMHOSTS;
                resolveOrder[1] = RESOLVER_WINS;
                resolveOrder[2] = RESOLVER_BCAST;
            }
        } else {
            int[] tmp = new int[3];
            StringTokenizer st = new StringTokenizer( RO, "," );
            int i = 0;
            while( st.hasMoreTokens() ) {
                String s = st.nextToken().trim();
                if( s.equalsIgnoreCase( "LMHOSTS" )) {
                    tmp[i++] = RESOLVER_LMHOSTS;
                } else if( s.equalsIgnoreCase( "WINS" )) {
                    if( NbtAddress.getWINSAddress() == null ) {
                        if( log.level > 1 ) {
                            log.println( "NetBIOS resolveOrder specifies WINS however the " +
                                    "jcifs.netbios.wins property has not been set" );
                        }
                        continue;
                    }
                    tmp[i++] = RESOLVER_WINS;
                } else if( s.equalsIgnoreCase( "BCAST" )) {
                    tmp[i++] = RESOLVER_BCAST;
                } else if( s.equalsIgnoreCase( "DNS" )) {
                    ; // skip
                } else if( log.level > 1 ) {
                    log.println( "unknown resolver method: " + s );
                }
            }
            resolveOrder = new int[i];
            System.arraycopy( tmp, 0, resolveOrder, 0, i );
        }
    }

    int getNextNameTrnId() {
        if(( ++nextNameTrnId & 0xFFFF ) == 0 ) {
            nextNameTrnId = 1;
        }
        return nextNameTrnId;
    }
    void ensureOpen( int timeout ) throws IOException {
        closeTimeout = 0;
        if( SO_TIMEOUT != 0 ) {
            closeTimeout = Math.max( SO_TIMEOUT, timeout );
        }
        // If socket is still good, the new closeTimeout will
        // be ignored; see tryClose comment.
        if( socket == null ) {
            socket = new DatagramSocket( lport, laddr );
            thread = new Thread( this, "JCIFS-NameServiceClient" );
            thread.setDaemon( true );
            thread.start();
        }
    }
    void tryClose() {
        synchronized( LOCK ) {

            /* Yes, there is the potential to drop packets
             * because we might close the socket during a
             * request. However the chances are slim and the
             * retry code should ensure the overall request
             * is serviced. The alternative complicates things
             * more than I think is worth it.
             */

            if( socket != null ) {
                socket.close();
                socket = null;
            }
            thread = null;
            responseTable.clear();
        }
    }
    public void run() {
        int nameTrnId;
        NameServicePacket response;

        try {
            while( thread == Thread.currentThread() ) {
                in.setLength( RCV_BUF_SIZE );

                socket.setSoTimeout( closeTimeout );
                socket.receive( in );

                if( log.level > 3 )
                    log.println( "NetBIOS: new data read from socket" );

                nameTrnId = NameServicePacket.readNameTrnId( rcv_buf, 0 );
                response = (NameServicePacket)responseTable.get( new Integer( nameTrnId ));
                if( response == null || response.received ) {
                    continue;
                }
                synchronized( response ) {
                    response.readWireFormat( rcv_buf, 0 );
                    response.received = true;

                    if( log.level > 3 ) {
                        log.println( response );
                        Hexdump.hexdump( log, rcv_buf, 0, in.getLength() );
                    }

                    response.notify();
                }
            }
        } catch(SocketTimeoutException ste) {
        } catch( Exception ex ) {
            if( log.level > 2 )
                ex.printStackTrace( log );
        } finally {
            tryClose();
        }
    }
    void send( NameServicePacket request, NameServicePacket response,
                                            int timeout ) throws IOException {
        Integer nid = null;
        int max = NbtAddress.NBNS.length;

        if (max == 0)
            max = 1; /* No WINs, try only bcast addr */

        synchronized( response ) {
            while (max-- > 0) {
                try {
                    synchronized( LOCK ) {
                        request.nameTrnId = getNextNameTrnId();
                        nid = new Integer( request.nameTrnId );

                        out.setAddress( request.addr );
                        out.setLength( request.writeWireFormat( snd_buf, 0 ));
                        response.received = false;

                        responseTable.put( nid, response );
                        ensureOpen( timeout + 1000 );
                        socket.send( out );

                        if( log.level > 3 ) {
                            log.println( request );
                            Hexdump.hexdump( log, snd_buf, 0, out.getLength() );
                        }
                    }

                    long start = System.currentTimeMillis();
                    while (timeout > 0) {
                        response.wait( timeout );

                        /* JetDirect printer can respond to regular broadcast query
                         * with node status so we need to check to make sure that
                         * the record type matches the question type and if not,
                         * loop around and try again.
                         */
                        if (response.received && request.questionType == response.recordType)
                            return;

                        response.received = false;
                        timeout -= System.currentTimeMillis() - start;
                    }

                } catch( InterruptedException ie ) {
                    throw new IOException(ie.getMessage());
                } finally {
                    responseTable.remove( nid );
                }

                synchronized (LOCK) {
                    if (NbtAddress.isWINS( request.addr ) == false)
                        break;
                                    /* Message was sent to WINS but
                                     * failed to receive response.
                                     * Try a different WINS server.
                                     */
                    if (request.addr == NbtAddress.getWINSAddress())
                        NbtAddress.switchWINS();
                    request.addr = NbtAddress.getWINSAddress();
                }
            }
        }
    }

    NbtAddress[] getAllByName( Name name, InetAddress addr )
                                            throws UnknownHostException {
        int n;
        NameQueryRequest request = new NameQueryRequest( name );
        NameQueryResponse response = new NameQueryResponse();

        request.addr = addr != null ? addr : NbtAddress.getWINSAddress();
        request.isBroadcast = request.addr == null;

        if( request.isBroadcast ) {
            request.addr = baddr;
            n = RETRY_COUNT;
        } else {
            request.isBroadcast = false;
            n = 1;
        }

        do {
            try {
                send( request, response, RETRY_TIMEOUT );
            } catch( IOException ioe ) {
                if( log.level > 1 )
                    ioe.printStackTrace( log );
                throw new UnknownHostException( name.name );
            }

            if( response.received && response.resultCode == 0 ) {
                return response.addrEntry;
            }
        } while( --n > 0 && request.isBroadcast );

        throw new UnknownHostException( name.name );
    }
    NbtAddress getByName( Name name, InetAddress addr )
                                            throws UnknownHostException {
        int n;
        NameQueryRequest request = new NameQueryRequest( name );
        NameQueryResponse response = new NameQueryResponse();

        if( addr != null ) { /* UniAddress calls always use this
                              * because it specifies addr
                              */
            request.addr = addr; /* if addr ends with 255 flag it bcast */
            request.isBroadcast = (addr.getAddress()[3] == (byte)0xFF);

            n = RETRY_COUNT;
            do {
                try {
                    send( request, response, RETRY_TIMEOUT );
                } catch( IOException ioe ) {
                    if( log.level > 1 )
                        ioe.printStackTrace( log );
                    throw new UnknownHostException( name.name );
                }

                if( response.received && response.resultCode == 0 ) {
                    int last = response.addrEntry.length - 1;
                    response.addrEntry[last].hostName.srcHashCode = addr.hashCode();
                    return response.addrEntry[last];
                }
            } while( --n > 0 && request.isBroadcast );

            throw new UnknownHostException( name.name );
        }

        /* If a target address to query was not specified explicitly
         * with the addr parameter we fall into this resolveOrder routine.
         */

        for( int i = 0; i < resolveOrder.length; i++ ) {
            try {
                switch( resolveOrder[i] ) {
                    case RESOLVER_LMHOSTS:
                        NbtAddress ans = Lmhosts.getByName( name );
                        if( ans != null ) {
                            ans.hostName.srcHashCode = 0; // just has to be different
                                                          // from other methods
                            return ans;
                        }
                        break;
                    case RESOLVER_WINS:
                    case RESOLVER_BCAST:
                        if( resolveOrder[i] == RESOLVER_WINS &&
                                name.name != NbtAddress.MASTER_BROWSER_NAME &&
                                name.hexCode != 0x1d ) {
                            request.addr = NbtAddress.getWINSAddress();
                            request.isBroadcast = false;
                        } else {
                            request.addr = baddr;
                            request.isBroadcast = true;
                        }

                        n = RETRY_COUNT;
                        while( n-- > 0 ) {
                            try {
                                send( request, response, RETRY_TIMEOUT );
                            } catch( IOException ioe ) {
                                if( log.level > 1 )
                                    ioe.printStackTrace( log );
                                throw new UnknownHostException( name.name );
                            }
                            if( response.received && response.resultCode == 0 ) {

/* Before we return, in anticipation of this address being cached we must
 * augment the addresses name's hashCode to distinguish those resolved by
 * Lmhosts, WINS, or BCAST. Otherwise a failed query from say WINS would
 * get pulled out of the cache for a BCAST on the same name.
 */
                                response.addrEntry[0].hostName.srcHashCode =
                                                        request.addr.hashCode();
                                return response.addrEntry[0];
                            } else if( resolveOrder[i] == RESOLVER_WINS ) {
                                /* If WINS reports negative, no point in retry
                                 */
                                break;
                            }
                        }
                        break;
                }
            } catch( IOException ioe ) {
            }
        }
        throw new UnknownHostException( name.name );
    }
    NbtAddress[] getNodeStatus( NbtAddress addr ) throws UnknownHostException {
        int n, srcHashCode;
        NodeStatusRequest request;
        NodeStatusResponse response;

        response = new NodeStatusResponse( addr );
        request = new NodeStatusRequest(
                            new Name( NbtAddress.ANY_HOSTS_NAME, 0x00, null));
        request.addr = addr.getInetAddress();

        n = RETRY_COUNT;
        while( n-- > 0 ) {
            try {
                send( request, response, RETRY_TIMEOUT );
            } catch( IOException ioe ) {
                if( log.level > 1 )
                    ioe.printStackTrace( log );
                throw new UnknownHostException( addr.toString() );
            }
            if( response.received && response.resultCode == 0 ) {

        /* For name queries resolved by different sources (e.g. WINS,
         * BCAST, Node Status) we need to augment the hashcode generated
         * for the addresses hostname or failed lookups for one type will
         * be cached and cause other types to fail even though they may
         * not be the authority for the name. For example, if a WINS lookup
         * for FOO fails and caches unknownAddress for FOO, a subsequent
         * lookup for FOO using BCAST should not fail because of that
         * name cached from WINS.
         *
         * So, here we apply the source addresses hashCode to each name to
         * make them specific to who resolved the name.
         */

                srcHashCode = request.addr.hashCode();
                for( int i = 0; i < response.addressArray.length; i++ ) {
                    response.addressArray[i].hostName.srcHashCode = srcHashCode;
                }
                return response.addressArray;
            }
        }
        throw new UnknownHostException( addr.hostName.name );
    }
}
