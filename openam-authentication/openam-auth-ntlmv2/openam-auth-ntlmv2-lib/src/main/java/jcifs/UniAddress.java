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

package jcifs;

import jcifs.netbios.Lmhosts;
import jcifs.netbios.NbtAddress;
import jcifs.util.LogStream;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

/**
 * <p>Under normal conditions it is not necessary to use
 * this class to use jCIFS properly. Name resolusion is
 * handled internally to the <code>jcifs.smb</code> package.
 * <p>
 * This class is a wrapper for both {@link NbtAddress}
 * and {@link InetAddress}. The name resolution mechanisms
 * used will systematically query all available configured resolution
 * services including WINS, broadcasts, DNS, and LMHOSTS. See
 * <a href="../../resolver.html">Setting Name Resolution Properties</a>
 * and the <code>jcifs.resolveOrder</code> property. Changing
 * jCIFS name resolution properties can greatly affect the behavior of
 * the client and may be necessary for proper operation.
 * <p>
 * This class should be used in favor of <tt>InetAddress</tt> to resolve
 * hostnames on LANs and WANs that support a mixture of NetBIOS/WINS and
 * DNS resolvable hosts.
 */

public class UniAddress {

    private static final int RESOLVER_WINS    = 0;
    private static final int RESOLVER_BCAST   = 1;
    private static final int RESOLVER_DNS     = 2;
    private static final int RESOLVER_LMHOSTS = 3;

    private static int[] resolveOrder;
    private static InetAddress baddr;

    private static LogStream log = LogStream.getInstance();

    static {
        String ro = Config.getProperty( "jcifs.resolveOrder" );
        InetAddress nbns = NbtAddress.getWINSAddress();

        try {
            baddr = Config.getInetAddress( "jcifs.netbios.baddr",
                                InetAddress.getByName( "255.255.255.255" ));
        } catch( UnknownHostException uhe ) {
        }

        if( ro == null || ro.length() == 0 ) {

            /* No resolveOrder has been specified, use the
             * default which is LMHOSTS,WINS,BCAST,DNS or just
             * LMHOSTS,BCAST,DNS if jcifs.netbios.wins has not
             * been specified.
             */

            if( nbns == null ) {
                resolveOrder = new int[3];
                resolveOrder[0] = RESOLVER_LMHOSTS;
                resolveOrder[1] = RESOLVER_DNS;
                resolveOrder[2] = RESOLVER_BCAST;
            } else {
                resolveOrder = new int[4];
                resolveOrder[0] = RESOLVER_LMHOSTS;
                resolveOrder[1] = RESOLVER_WINS;
                resolveOrder[2] = RESOLVER_DNS;
                resolveOrder[3] = RESOLVER_BCAST;
            }
        } else {
            int[] tmp = new int[4];
            StringTokenizer st = new StringTokenizer( ro, "," );
            int i = 0;
            while( st.hasMoreTokens() ) {
                String s = st.nextToken().trim();
                if( s.equalsIgnoreCase( "LMHOSTS" )) {
                    tmp[i++] = RESOLVER_LMHOSTS;
                } else if( s.equalsIgnoreCase( "WINS" )) {
                    if( nbns == null ) {
                        if( log.level > 1 ) {
                            log.println( "UniAddress resolveOrder specifies WINS however the " +
                                    "jcifs.netbios.wins property has not been set" );
                        }
                        continue;
                    }
                    tmp[i++] = RESOLVER_WINS;
                } else if( s.equalsIgnoreCase( "BCAST" )) {
                    tmp[i++] = RESOLVER_BCAST;
                } else if( s.equalsIgnoreCase( "DNS" )) {
                    tmp[i++] = RESOLVER_DNS;
                } else if( log.level > 1 ) {
                    log.println( "unknown resolver method: " + s );
                }
            }
            resolveOrder = new int[i];
            System.arraycopy( tmp, 0, resolveOrder, 0, i );
        }
    }

    static class Sem {
        Sem( int count ) {
            this.count = count;
        }
        int count;
    }

    static class QueryThread extends Thread {
    
        Sem sem;
        String host, scope;
        int type;
        NbtAddress ans = null;
        InetAddress svr;
        UnknownHostException uhe;
    
        QueryThread( Sem sem, String host, int type,
                        String scope, InetAddress svr ) {
            super( "JCIFS-QueryThread: " + host );
            this.sem = sem;
            this.host = host;
            this.type = type;
            this.scope = scope;
            this.svr = svr;
        }
        public void run() {
            try {
                ans = NbtAddress.getByName( host, type, scope, svr );
            } catch( UnknownHostException uhe ) {
                this.uhe = uhe;
            } catch( Exception ex ) {
                this.uhe = new UnknownHostException( ex.getMessage() );
            } finally {
                synchronized( sem ) {
                    sem.count--;
                    sem.notify();
                }
            }
        }
    }

    static NbtAddress lookupServerOrWorkgroup( String name, InetAddress svr )
                                                    throws UnknownHostException {
        Sem sem = new Sem( 2 );
        int type = NbtAddress.isWINS( svr ) ? 0x1b : 0x1d;

        QueryThread q1x = new QueryThread( sem, name, type, null, svr );
        QueryThread q20 = new QueryThread( sem, name, 0x20, null, svr );
        q1x.setDaemon( true );
        q20.setDaemon( true );
        try {
            synchronized( sem ) {
                q1x.start();
                q20.start();

                while( sem.count > 0 && q1x.ans == null && q20.ans == null ) {
                    sem.wait();
                }
            }
        } catch( InterruptedException ie ) {
            throw new UnknownHostException( name );
        }
        if( q1x.ans != null ) {
            return q1x.ans;
        } else if( q20.ans != null ) {
            return q20.ans;
        } else {
            throw q1x.uhe;
        }
    }

    /** 
     * Determines the address of a host given it's host name. The name can be a
     * machine name like "jcifs.samba.org",  or an IP address like "192.168.1.15".
     *
     * @param hostname NetBIOS or DNS hostname to resolve
     * @throws UnknownHostException if there is an error resolving the name
     */

    public static UniAddress getByName( String hostname )
                                        throws UnknownHostException {
        return getByName( hostname, false );
    }

    static boolean isDotQuadIP( String hostname ) {
        if( Character.isDigit( hostname.charAt( 0 ))) {
            int i, len, dots;
            char[] data;

            i = dots = 0;                    /* quick IP address validation */
            len = hostname.length();
            data = hostname.toCharArray();
            while( i < len && Character.isDigit( data[i++] )) {
                if( i == len && dots == 3 ) {
                    // probably an IP address
                    return true;
                }
                if( i < len && data[i] == '.' ) {
                    dots++;
                    i++;
                }
            }
        }

        return false;
    }

    static boolean isAllDigits( String hostname ) {
        for (int i = 0; i < hostname.length(); i++) {
            if (Character.isDigit( hostname.charAt( i )) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lookup <tt>hostname</tt> and return it's <tt>UniAddress</tt>. If the
     * <tt>possibleNTDomainOrWorkgroup</tt> parameter is <tt>true</tt> an
     * addtional name query will be performed to locate a master browser.
     */

    public static UniAddress getByName( String hostname,
                                        boolean possibleNTDomainOrWorkgroup )
                                        throws UnknownHostException {
        UniAddress[] addrs = UniAddress.getAllByName(hostname, possibleNTDomainOrWorkgroup);
        return addrs[0];
    }
    public static UniAddress[] getAllByName( String hostname,
                                        boolean possibleNTDomainOrWorkgroup )
                                        throws UnknownHostException {
        Object addr;
        int i;

        if( hostname == null || hostname.length() == 0 ) {
            throw new UnknownHostException();
        }

        if( isDotQuadIP( hostname )) {
            UniAddress[] addrs = new UniAddress[1];
            addrs[0] = new UniAddress( NbtAddress.getByName( hostname ));
            return addrs;
        }

        for( i = 0; i < resolveOrder.length; i++ ) {
            try {
                switch( resolveOrder[i] ) {
                    case RESOLVER_LMHOSTS:
                        if(( addr = Lmhosts.getByName( hostname )) == null ) {
                            continue;
                        }
                        break;
                    case RESOLVER_WINS:
                        if( hostname == NbtAddress.MASTER_BROWSER_NAME ||
                                                    hostname.length() > 15 ) {
                                                    // invalid netbios name
                            continue;
                        }
                        if( possibleNTDomainOrWorkgroup ) {
                            addr = lookupServerOrWorkgroup( hostname, NbtAddress.getWINSAddress() );
                        } else {
                            addr = NbtAddress.getByName( hostname, 0x20, null, NbtAddress.getWINSAddress() );
                        }
                        break;
                    case RESOLVER_BCAST:
                        if( hostname.length() > 15 ) {
                            // invalid netbios name
                            continue;
                        }
                        if( possibleNTDomainOrWorkgroup ) {
                            addr = lookupServerOrWorkgroup( hostname, baddr );
                        } else {
                            addr = NbtAddress.getByName( hostname, 0x20, null, baddr );
                        }
                        break;
                    case RESOLVER_DNS:
                        if( isAllDigits( hostname )) {
                            throw new UnknownHostException( hostname );
                        }
                        InetAddress[] iaddrs = InetAddress.getAllByName( hostname );
                        UniAddress[] addrs = new UniAddress[iaddrs.length];
                        for (int ii = 0; ii < iaddrs.length; ii++) {
                            addrs[ii] = new UniAddress(iaddrs[ii]);
                        }
                        return addrs; // Success
                    default:
                        throw new UnknownHostException( hostname );
                }
                UniAddress[] addrs = new UniAddress[1];
                addrs[0] = new UniAddress( addr );
                return addrs; // Success
            } catch( IOException ioe ) {
                // Failure
            }
        }
        throw new UnknownHostException( hostname );
    }

    /**
     * Perform DNS SRV lookup on successively shorter suffixes of name
     * and return successful suffix or throw an UnknownHostException.
import javax.naming.*;
import javax.naming.directory.*;
    public static String getDomainByName(String name) throws UnknownHostException {
        DirContext context;
        UnknownHostException uhe = null;

        try {
            context = new InitialDirContext();
            for ( ;; ) {
                try {
                    Attributes attributes = context.getAttributes(
                        "dns:/_ldap._tcp.dc._msdcs." + name,
                        new String[] { "SRV" }
                    );
                    return name;
                } catch (NameNotFoundException nnfe) {
                    uhe = new UnknownHostException(nnfe.getMessage());
                }
                int dot = name.indexOf('.');
                if (dot == -1)
                    break;
                name = name.substring(dot + 1);
            }
        } catch (NamingException ne) {
            if (log.level > 1)
                ne.printStackTrace(log);
        }

        throw uhe != null ? uhe : new UnknownHostException("invalid name");
    }
     */


    Object addr;
    String calledName;

    /**
     * Create a <tt>UniAddress</tt> by wrapping an <tt>InetAddress</tt> or
     * <tt>NbtAddress</tt>.
     */

    public UniAddress( Object addr ) {
        if( addr == null ) {
            throw new IllegalArgumentException();
        }
        this.addr = addr;
    }

    /**
     * Return the IP address of this address as a 32 bit integer.
     */

    public int hashCode() {
        return addr.hashCode();
    }

    /**
     * Compare two addresses for equality. Two <tt>UniAddress</tt>s are equal
     * if they are both <tt>UniAddress</tt>' and refer to the same IP address.
     */
    public boolean equals( Object obj ) {
        return obj instanceof UniAddress && addr.equals(((UniAddress)obj).addr);
    }
/*
    public boolean equals( Object obj ) {
        return obj instanceof UniAddress && addr.hashCode() == obj.hashCode();
    }
*/

    /**
     * Guess first called name to try for session establishment. This
     * method is used exclusively by the <tt>jcifs.smb</tt> package.
     */

    public String firstCalledName() {
        if( addr instanceof NbtAddress ) {
            return ((NbtAddress)addr).firstCalledName();
        } else {
            calledName = ((InetAddress)addr).getHostName();
            if( isDotQuadIP( calledName )) {
                calledName = NbtAddress.SMBSERVER_NAME;
            } else {
                int i = calledName.indexOf( '.' );
                if( i > 1 && i < 15 ) {
                    calledName = calledName.substring( 0, i ).toUpperCase();
                } else if( calledName.length() > 15 ) {
                    calledName = NbtAddress.SMBSERVER_NAME;
                } else {
                    calledName = calledName.toUpperCase();
                }
            }
        }

        return calledName;
    }

    /**
     * Guess next called name to try for session establishment. This
     * method is used exclusively by the <tt>jcifs.smb</tt> package.
     */

    public String nextCalledName() {
        if( addr instanceof NbtAddress ) {
            return ((NbtAddress)addr).nextCalledName();
        } else if( calledName != NbtAddress.SMBSERVER_NAME ) {
            calledName = NbtAddress.SMBSERVER_NAME;
            return calledName;
        }
        return null;
    }

    /**
     * Return the underlying <tt>NbtAddress</tt> or <tt>InetAddress</tt>.
     */

    public Object getAddress() {
        return addr;
    }

    /**
     * Return the hostname of this address such as "MYCOMPUTER".
     */

    public String getHostName() {
        if( addr instanceof NbtAddress ) {
            return ((NbtAddress)addr).getHostName();
        }
        return ((InetAddress)addr).getHostName();
    }

    /**
     * Return the IP address as text such as "192.168.1.15".
     */

    public String getHostAddress() {
        if( addr instanceof NbtAddress ) {
            return ((NbtAddress)addr).getHostAddress();
        }
        return ((InetAddress)addr).getHostAddress();
    }

    /**
     * Return the a text representation of this address such as
     * <tt>MYCOMPUTER/192.168.1.15</tt>.
     */
    public String toString() {
        return addr.toString();
    }
}
