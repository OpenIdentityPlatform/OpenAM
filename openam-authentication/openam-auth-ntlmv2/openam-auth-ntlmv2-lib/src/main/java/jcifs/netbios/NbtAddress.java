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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * This class represents a NetBIOS over TCP/IP address. Under normal
 * conditions, users of jCIFS need not be concerned with this class as
 * name resolution and session services are handled internally by the smb package.
 * 
 * <p> Applications can use the methods <code>getLocalHost</code>,
 * <code>getByName</code>, and
 * <code>getAllByAddress</code> to create a new NbtAddress instance. This
 * class is symmetric with {@link InetAddress}.
 *
 * <p><b>About NetBIOS:</b> The NetBIOS name
 * service is a dynamic distributed service that allows hosts to resolve
 * names by broadcasting a query, directing queries to a server such as
 * Samba or WINS. NetBIOS is currently the primary networking layer for
 * providing name service, datagram service, and session service to the
 * Microsoft Windows platform. A NetBIOS name can be 15 characters long
 * and hosts usually registers several names on the network. From a
 * Windows command prompt you can see
 * what names a host registers with the nbtstat command.
 * <p><blockquote><pre>
 * C:\>nbtstat -a 192.168.1.15
 * 
 *        NetBIOS Remote Machine Name Table
 * 
 *    Name               Type         Status
 * ---------------------------------------------
 * JMORRIS2        <00>  UNIQUE      Registered
 * BILLING-NY      <00>  GROUP       Registered
 * JMORRIS2        <03>  UNIQUE      Registered
 * JMORRIS2        <20>  UNIQUE      Registered
 * BILLING-NY      <1E>  GROUP       Registered
 * JMORRIS         <03>  UNIQUE      Registered
 * 
 * MAC Address = 00-B0-34-21-FA-3B
 * </blockquote></pre>
 * <p> The hostname of this machine is <code>JMORRIS2</code>. It is
 * a member of the group(a.k.a workgroup and domain) <code>BILLING-NY</code>. To
 * obtain an {@link InetAddress} for a host one might do:
 *
 * <pre>
 *   InetAddress addr = NbtAddress.getByName( "jmorris2" ).getInetAddress();
 * </pre>
 * <p>From a UNIX platform with Samba installed you can perform similar
 * diagnostics using the <code>nmblookup</code> utility.
 *
 * @author    Michael B. Allen
 * @see       InetAddress
 * @since     jcifs-0.1
 */ 

public final class NbtAddress {

/*
 * This is a special name that means all hosts. If you wish to find all hosts
 * on a network querying a workgroup group name is the preferred method.
 */ 

    static final String ANY_HOSTS_NAME = "*\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000";

/** 
 * This is a special name for querying the master browser that serves the
 * list of hosts found in "Network Neighborhood".
 */ 

    public static final String MASTER_BROWSER_NAME = "\u0001\u0002__MSBROWSE__\u0002";

/**
 * A special generic name specified when connecting to a host for which
 * a name is not known. Not all servers respond to this name.
 */

    public static final String SMBSERVER_NAME = "*SMBSERVER     ";

/** 
 * A B node only broadcasts name queries. This is the default if a
 * nameserver such as WINS or Samba is not specified.
 */ 

    public static final int B_NODE = 0;

/**
 * A Point-to-Point node, or P node, unicasts queries to a nameserver
 * only. Natrually the <code>jcifs.netbios.nameserver</code> property must
 * be set.
 */

    public static final int P_NODE = 1;

/** 
 * Try Broadcast queries first, then try to resolve the name using the
 * nameserver.
 */

    public static final int M_NODE = 2;

/** 
 * A Hybrid node tries to resolve a name using the nameserver first. If
 * that fails use the broadcast address. This is the default if a nameserver
 * is provided. This is the behavior of Microsoft Windows machines.
 */ 

    public static final int H_NODE = 3;

    static final InetAddress[] NBNS = Config.getInetAddressArray( "jcifs.netbios.wins", ",", new InetAddress[0] );

    /* Construct the shared static client object that will
     * conduct all encoding and decoding of NetBIOS name service
     * messages as well as socket IO in a synchronized fashon.
     */

    private static final NameServiceClient CLIENT = new NameServiceClient();

    private static final int DEFAULT_CACHE_POLICY = 30;
    private static final int CACHE_POLICY = Config.getInt( "jcifs.netbios.cachePolicy", DEFAULT_CACHE_POLICY );
    private static final int FOREVER = -1;
    private static int nbnsIndex = 0;

    private static final HashMap ADDRESS_CACHE = new HashMap();
    private static final HashMap LOOKUP_TABLE = new HashMap();

    static final Name UNKNOWN_NAME = new Name( "0.0.0.0", 0x00, null );
    static final NbtAddress UNKNOWN_ADDRESS = new NbtAddress( UNKNOWN_NAME, 0, false, B_NODE );
    static final byte[] UNKNOWN_MAC_ADDRESS = new byte[] {
        (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00
    };

    static final class CacheEntry {
        Name hostName;
        NbtAddress address;
        long expiration;

        CacheEntry( Name hostName, NbtAddress address, long expiration ) {
            this.hostName = hostName;
            this.address = address;
            this.expiration = expiration;
        }
    }

    static NbtAddress localhost;

    static {
        InetAddress localInetAddress;
        String localHostname;
        Name localName;

        /* Create an address to represent failed lookups and cache forever.
         */

        ADDRESS_CACHE.put( UNKNOWN_NAME, new CacheEntry( UNKNOWN_NAME, UNKNOWN_ADDRESS, FOREVER ));

        /* Determine the InetAddress of the local interface
         * if one was not specified.
         */
        localInetAddress = CLIENT.laddr;
        if( localInetAddress == null ) {
            try {
                localInetAddress = InetAddress.getLocalHost();
            } catch( UnknownHostException uhe ) {
                /* Java cannot determine the localhost. This is basically a config
                 * issue on the host. There's not much we can do about it. Just
                 * to suppress NPEs that would result we can create a possibly bogus
                 * address. Pretty sure the below cannot actually thrown a UHE tho.
                 */
                try {
                    localInetAddress = InetAddress.getByName("127.0.0.1");
                } catch( UnknownHostException ignored ) {
                }
            }
        }

        /* If a local hostname was not provided a name like
         * JCIFS34_172_A6 will be dynamically generated for the
         * client. This is primarily (exclusively?) used as a
         * CallingName during session establishment.
         */
        localHostname = Config.getProperty( "jcifs.netbios.hostname", null );
        if( localHostname == null || localHostname.length() == 0 ) {
            byte[] addr = localInetAddress.getAddress();
            localHostname = "JCIFS" +
                    ( addr[2] & 0xFF ) + "_" +
                    ( addr[3] & 0xFF ) + "_" +
                    Hexdump.toHexString( (int)( Math.random() * (double)0xFF ), 2 );
        }

        /* Create an NbtAddress for the local interface with
         * the name deduced above possibly with scope applied and
         * cache it forever.
         */
        localName = new Name( localHostname, 0x00,
                            Config.getProperty( "jcifs.netbios.scope", null ));
        localhost = new NbtAddress( localName,
                                    localInetAddress.hashCode(),
                                    false,
                                    B_NODE,
                                    false, false, true, false,
                                    UNKNOWN_MAC_ADDRESS );
        cacheAddress( localName, localhost, FOREVER );
    }

    static void cacheAddress( Name hostName, NbtAddress addr ) {
        if( CACHE_POLICY == 0 ) {
            return;
        }
        long expiration = -1;
        if( CACHE_POLICY != FOREVER ) {
            expiration = System.currentTimeMillis() + CACHE_POLICY * 1000;
        }
        cacheAddress( hostName, addr, expiration );
    }
    static void cacheAddress( Name hostName, NbtAddress addr, long expiration ) {
        if( CACHE_POLICY == 0 ) {
            return;
        }
        synchronized( ADDRESS_CACHE ) {
            CacheEntry entry = (CacheEntry)ADDRESS_CACHE.get( hostName );
            if( entry == null ) {
                entry = new CacheEntry( hostName, addr, expiration );
                ADDRESS_CACHE.put( hostName, entry );
            } else {
                entry.address = addr;
                entry.expiration = expiration;
            }
        }
    }
    static void cacheAddressArray( NbtAddress[] addrs ) {
        if( CACHE_POLICY == 0 ) {
            return;
        }
        long expiration = -1;
        if( CACHE_POLICY != FOREVER ) {
            expiration = System.currentTimeMillis() + CACHE_POLICY * 1000;
        }
        synchronized( ADDRESS_CACHE ) {
            for( int i = 0; i < addrs.length; i++ ) {
                CacheEntry entry = (CacheEntry)ADDRESS_CACHE.get( addrs[i].hostName );
                if( entry == null ) {
                    entry = new CacheEntry( addrs[i].hostName, addrs[i], expiration );
                    ADDRESS_CACHE.put( addrs[i].hostName, entry );
                } else {
                    entry.address = addrs[i];
                    entry.expiration = expiration;
                }
            }
        }
    }
    static NbtAddress getCachedAddress( Name hostName ) {
        if( CACHE_POLICY == 0 ) {
            return null;
        }
        synchronized( ADDRESS_CACHE ) {
            CacheEntry entry = (CacheEntry)ADDRESS_CACHE.get( hostName );
            if( entry != null && entry.expiration < System.currentTimeMillis() &&
                                                entry.expiration >= 0 ) {
                entry = null;
            }
            return entry != null ? entry.address : null;
        }
    }

    static NbtAddress doNameQuery( Name name, InetAddress svr )
                                                    throws UnknownHostException {
        NbtAddress addr;

        if( name.hexCode == 0x1d && svr == null ) {
            svr = CLIENT.baddr; // bit of a hack but saves a lookup
        }
        name.srcHashCode = svr != null ? svr.hashCode() : 0;
        addr = getCachedAddress( name );

        if( addr == null ) {
            /* This is almost exactly like InetAddress.java. See the
             * comments there for a description of how the LOOKUP_TABLE prevents
             * redundant queries from going out on the wire.
             */
            if(( addr = (NbtAddress)checkLookupTable( name )) == null ) {
                try {
                    addr = CLIENT.getByName( name, svr );
                } catch( UnknownHostException uhe ) {
                    addr = UNKNOWN_ADDRESS;
                } finally {
                    cacheAddress( name, addr );
                    updateLookupTable( name );
                }
            }
        }
        if( addr == UNKNOWN_ADDRESS ) {
            throw new UnknownHostException( name.toString() );
        }
        return addr;
    }

    private static Object checkLookupTable( Name name ) {
        Object obj;

        synchronized( LOOKUP_TABLE ) {
            if( LOOKUP_TABLE.containsKey( name ) == false ) {
                LOOKUP_TABLE.put( name, name );
                return null;
            }
            while( LOOKUP_TABLE.containsKey( name )) {
                try {
                    LOOKUP_TABLE.wait();
                } catch( InterruptedException e ) {
                }
            }
        }
        obj = getCachedAddress( name );
        if( obj == null ) {
            synchronized( LOOKUP_TABLE ) {
                LOOKUP_TABLE.put( name, name );
            }
        }

        return obj;
    }
    private static void updateLookupTable( Name name ) {
        synchronized( LOOKUP_TABLE ) {
            LOOKUP_TABLE.remove( name );
            LOOKUP_TABLE.notifyAll();
        }
    }

/** 
 * Retrieves the local host address.
 *
 * @throws UnknownHostException This is not likely as the IP returned
 *                    by <code>InetAddress</code> should be available
 */ 

    public static NbtAddress getLocalHost() throws UnknownHostException {
        return localhost;
    }
    public static Name getLocalName() {
        return localhost.hostName;
    }

/** 
 * Determines the address of a host given it's host name. The name can be a NetBIOS name like
 * "freto" or an IP address like "192.168.1.15". It cannot be a DNS name;
 * the analygous {@link jcifs.UniAddress} or {@link InetAddress}
 * <code>getByName</code> methods can be used for that.
 *
 * @param host hostname to resolve
 * @throws UnknownHostException if there is an error resolving the name
 */

    public static NbtAddress getByName( String host )
                                        throws UnknownHostException {
        return getByName( host, 0x00, null );
    }

/** 
 * Determines the address of a host given it's host name. NetBIOS
 * names also have a <code>type</code>. Types(aka Hex Codes)
 * are used to distiquish the various services on a host. <a
 * href="../../../nbtcodes.html">Here</a> is
 * a fairly complete list of NetBIOS hex codes. Scope is not used but is
 * still functional in other NetBIOS products and so for completeness it has been
 * implemented. A <code>scope</code> of <code>null</code> or <code>""</code>
 * signifies no scope.
 *
 * @param host the name to resolve
 * @param type the hex code of the name
 * @param scope the scope of the name
 * @throws UnknownHostException if there is an error resolving the name
 */

    public static NbtAddress getByName( String host,
                                        int type,
                                        String scope )
                                        throws UnknownHostException {

        return getByName( host, type, scope, null );
    }

/* 
 * The additional <code>svr</code> parameter specifies the address to
 * query. This might be the address of a specific host, a name server,
 * or a broadcast address.
 */ 

    public static NbtAddress getByName( String host,
                                        int type,
                                        String scope,
                                        InetAddress svr )
                                        throws UnknownHostException {

        if( host == null || host.length() == 0 ) {
            return getLocalHost();
        }
        if( !Character.isDigit( host.charAt(0) )) {
            return (NbtAddress)doNameQuery( new Name( host, type, scope ), svr );
        } else {
            int IP = 0x00;
            int hitDots = 0;
            char[] data = host.toCharArray();

            for( int i = 0; i < data.length; i++ ) {
                char c = data[i];
                if( c < 48 || c > 57 ) {
                    return (NbtAddress)doNameQuery( new Name( host, type, scope ), svr );
                }
                int b = 0x00;
                while( c != '.' ) {
                    if( c < 48 || c > 57 ) {
                        return (NbtAddress)doNameQuery( new Name( host, type, scope ), svr );
                    }
                    b = b * 10 + c - '0';

                    if( ++i >= data.length )
                        break;

                    c = data[i];
                }
                if( b > 0xFF ) {
                    return (NbtAddress)doNameQuery( new Name( host, type, scope ), svr );
                }
                IP = ( IP << 8 ) + b;
                hitDots++;
            }
            if( hitDots != 4 || host.endsWith( "." )) {
                return (NbtAddress)doNameQuery( new Name( host, type, scope ), svr );
            }
            return new NbtAddress( UNKNOWN_NAME, IP, false, B_NODE );
        }
    }

    public static NbtAddress[] getAllByName( String host,
                                        int type,
                                        String scope,
                                        InetAddress svr )
                                        throws UnknownHostException {
        return CLIENT.getAllByName( new Name( host, type, scope ), svr );
    }

/**
 * Retrieve all addresses of a host by it's address. NetBIOS hosts can
 * have many names for a given IP address. The name and IP address make the
 * NetBIOS address. This provides a way to retrieve the other names for a
 * host with the same IP address.
 *
 * @param host hostname to lookup all addresses for
 * @throws UnknownHostException if there is an error resolving the name
 */


    public static NbtAddress[] getAllByAddress( String host )
                                                throws UnknownHostException {
        return getAllByAddress( getByName( host, 0x00, null ));
    }


/**
 * Retrieve all addresses of a host by it's address. NetBIOS hosts can
 * have many names for a given IP address. The name and IP address make
 * the NetBIOS address. This provides a way to retrieve the other names
 * for a host with the same IP address.  See {@link #getByName}
 * for a description of <code>type</code>
 * and <code>scope</code>.
 *
 * @param host hostname to lookup all addresses for
 * @param type the hexcode of the name
 * @param scope the scope of the name
 * @throws UnknownHostException if there is an error resolving the name
 */


    public static NbtAddress[] getAllByAddress( String host,
                                        int type,
                                        String scope )
                                        throws UnknownHostException {
        return getAllByAddress( getByName( host, type, scope ));
    }


/**
 * Retrieve all addresses of a host by it's address. NetBIOS hosts can
 * have many names for a given IP address. The name and IP address make the
 * NetBIOS address. This provides a way to retrieve the other names for a
 * host with the same IP address.
 *
 * @param addr the address to query
 * @throws UnknownHostException if address cannot be resolved
 */

    public static NbtAddress[] getAllByAddress( NbtAddress addr )
                                                throws UnknownHostException {
        try {
            NbtAddress[] addrs = CLIENT.getNodeStatus( addr );
            cacheAddressArray( addrs );
            return addrs;
        } catch( UnknownHostException uhe ) {
            throw new UnknownHostException( "no name with type 0x" +
                            Hexdump.toHexString( addr.hostName.hexCode, 2 ) +
                            ((( addr.hostName.scope == null ) ||
                            ( addr.hostName.scope.length() == 0 )) ?
                            " with no scope" : " with scope " + addr.hostName.scope ) +
                            " for host " + addr.getHostAddress() );
        }
    }

    public static InetAddress getWINSAddress() {
        return NBNS.length == 0 ? null : NBNS[nbnsIndex];
    }
    public static boolean isWINS( InetAddress svr ) {
        for( int i = 0; svr != null && i < NBNS.length; i++ ) {
            if( svr.hashCode() == NBNS[i].hashCode() ) {
                return true;
            }
        }
        return false;
    }
    static InetAddress switchWINS() {
        nbnsIndex = (nbnsIndex + 1) < NBNS.length ? nbnsIndex + 1 : 0;
        return NBNS.length == 0 ? null : NBNS[nbnsIndex];
    }

    Name hostName;
    int address, nodeType;
    boolean groupName,
        isBeingDeleted,
        isInConflict,
        isActive,
        isPermanent,
        isDataFromNodeStatus;
    byte[] macAddress;
    String calledName;

    NbtAddress( Name hostName, int address, boolean groupName, int nodeType ) {
        this.hostName = hostName;
        this.address = address;
        this.groupName = groupName;
        this.nodeType = nodeType;
    }

    NbtAddress( Name hostName,
                int address,
                boolean groupName,
                int nodeType,
                boolean isBeingDeleted,
                boolean isInConflict,
                boolean isActive,
                boolean isPermanent,
                byte[] macAddress ) {

/* The NodeStatusResponse.readNodeNameArray method may also set this
 * information. These two places where node status data is populated should
 * be consistent. Be carefull!
 */
        this.hostName = hostName;
        this.address = address;
        this.groupName = groupName;
        this.nodeType = nodeType;
        this.isBeingDeleted = isBeingDeleted;
        this.isInConflict = isInConflict;
        this.isActive = isActive;
        this.isPermanent = isPermanent;
        this.macAddress = macAddress;
        isDataFromNodeStatus = true;
    }

/* Guess next called name to try for session establishment. These
 * methods are used by the smb package.
 */

    public String firstCalledName() {

        calledName = hostName.name;

        if( Character.isDigit( calledName.charAt( 0 ))) {
            int i, len, dots;
            char[] data;

            i = dots = 0;                    /* quick IP address validation */
            len = calledName.length();
            data = calledName.toCharArray();
            while( i < len && Character.isDigit( data[i++] )) {
                if( i == len && dots == 3 ) {
                    // probably an IP address
                    calledName = SMBSERVER_NAME;
                    break;
                }
                if( i < len && data[i] == '.' ) {
                    dots++;
                    i++;
                }
            }
        } else {
            switch (hostName.hexCode) {
                case 0x1B:
                case 0x1C:
                case 0x1D:
                    calledName = SMBSERVER_NAME;
            }
        }

        return calledName;
    }
    public String nextCalledName() {

        if( calledName == hostName.name ) {
            calledName = SMBSERVER_NAME;
        } else if( calledName == SMBSERVER_NAME ) {
            NbtAddress[] addrs;

            try {
                addrs = CLIENT.getNodeStatus( this );
                if( hostName.hexCode == 0x1D ) {
                    for( int i = 0; i < addrs.length; i++ ) {
                        if( addrs[i].hostName.hexCode == 0x20 ) {
                            return addrs[i].hostName.name;
                        }
                    }
                    return null;
                } else if( isDataFromNodeStatus ) {
                    /* 'this' has been updated and should now
                     * have a real NetBIOS name
                     */
                    calledName = null;
                    return hostName.name;
                }
            } catch( UnknownHostException uhe ) {
                calledName = null;
            }
        } else {
            calledName = null;
        }

        return calledName;
    }

/* 
 * There are three degrees of state that any NbtAddress can have.
 * 
 * 1) IP Address - If a dot-quad IP string is used with getByName (or used
 * to create an NbtAddress internal to this netbios package), no query is
 * sent on the wire and the only state this object has is it's IP address
 * (but that's enough to connect to a host using *SMBSERVER for CallingName).
 * 
 * 2) IP Address, NetBIOS name, nodeType, groupName - If however a
 * legal NetBIOS name string is used a name query request will retreive
 * the IP, node type, and whether or not this NbtAddress represents a
 * group name. This degree of state can be obtained with a Name Query
 * Request or Node Status Request.
 * 
 * 3) All - The NbtAddress will be populated with all state such as mac
 * address, isPermanent, isBeingDeleted, ...etc. This information can only
 * be retrieved with the Node Status request.
 * 
 * The degree of state that an NbtAddress has is dependant on how it was
 * created and what is required of it. The second degree of state is the
 * most common. This is the state information that would be retrieved from
 * WINS for example. Natrually it is not practical for every NbtAddress
 * to be populated will all state requiring a Node Status on every host
 * encountered. The below methods allow state to be populated when requested
 * in a lazy fashon.
 */ 

    void checkData() throws UnknownHostException {
        if( hostName == UNKNOWN_NAME ) {
            getAllByAddress( this );
        }
    }
    void checkNodeStatusData() throws UnknownHostException {
        if( isDataFromNodeStatus == false ) {
            getAllByAddress( this );
        }
    }

/**
 * Determines if the address is a group address. This is also
 * known as a workgroup name or group name.
 *
 * @throws UnknownHostException if the host cannot be resolved to find out.
 */

    public boolean isGroupAddress() throws UnknownHostException {
        checkData();
        return groupName;
    }

/** 
 * Checks the node type of this address.
 * @return {@link NbtAddress#B_NODE},
 * {@link NbtAddress#P_NODE}, {@link NbtAddress#M_NODE},
 * {@link NbtAddress#H_NODE}
 *
 * @throws UnknownHostException if the host cannot be resolved to find out.
 */ 

    public int getNodeType() throws UnknownHostException {
        checkData();
        return nodeType;
    }

/** 
 * Determines if this address in the process of being deleted.
 *
 * @throws UnknownHostException if the host cannot be resolved to find out.
 */ 

    public boolean isBeingDeleted() throws UnknownHostException {
        checkNodeStatusData();
        return isBeingDeleted;
    }

/** 
 * Determines if this address in conflict with another address.
 *
 * @throws UnknownHostException if the host cannot be resolved to find out.
 */ 

    public boolean isInConflict() throws UnknownHostException {
        checkNodeStatusData();
        return isInConflict;
    }

/** 
 * Determines if this address is active.
 *
 * @throws UnknownHostException if the host cannot be resolved to find out.
 */ 

    public boolean isActive() throws UnknownHostException {
        checkNodeStatusData();
        return isActive;
    }

/** 
 * Determines if this address is set to be permanent.
 *
 * @throws UnknownHostException if the host cannot be resolved to find out.
 */ 

    public boolean isPermanent() throws UnknownHostException {
        checkNodeStatusData();
        return isPermanent;
    }

/** 
 * Retrieves the MAC address of the remote network interface. Samba returns all zeros.
 *
 * @return the MAC address as an array of six bytes
 * @throws UnknownHostException if the host cannot be resolved to
 * determine the MAC address.
 */ 

    public byte[] getMacAddress() throws UnknownHostException {
        checkNodeStatusData();
        return macAddress;
    }

/** 
 * The hostname of this address. If the hostname is null the local machines
 * IP address is returned.
 *
 * @return the text representation of the hostname associated with this address
 */ 

    public String getHostName() {
        /* 2010 - We no longer try a Node Status to get the
         * hostname because apparently some servers do not respond
         * anymore. I think everyone post Windows 98 will accept
         * an IP address as the tconHostName which is the principal
         * use of this method.
         */
        if (hostName == UNKNOWN_NAME) {
            return getHostAddress();
        }
        return hostName.name;
    }


/** 
 * Returns the raw IP address of this NbtAddress. The result is in network
 * byte order: the highest order byte of the address is in getAddress()[0].
 *
 * @return a four byte array
 */ 

    public byte[] getAddress() {    
        byte[] addr = new byte[4];

        addr[0] = (byte)(( address >>> 24 ) & 0xFF );
        addr[1] = (byte)(( address >>> 16 ) & 0xFF );
        addr[2] = (byte)(( address >>> 8 ) & 0xFF );
        addr[3] = (byte)( address & 0xFF );
        return addr;
    }

/** 
 * To convert this address to an <code>InetAddress</code>.
 *
 * @return the {@link InetAddress} representation of this address.
 */ 

    public InetAddress getInetAddress() throws UnknownHostException {
        return InetAddress.getByName( getHostAddress() );
    }

/** 
 * Returns this IP adress as a {@link String} in the form "%d.%d.%d.%d".
 */ 

    public String getHostAddress() {    
        return (( address >>> 24 ) & 0xFF ) + "." +
            (( address >>> 16 ) & 0xFF ) + "." +
            (( address >>> 8 ) & 0xFF ) + "." +
            (( address >>> 0 ) & 0xFF );
    }

/**
 * Returned the hex code associated with this name(e.g. 0x20 is for the file service)
 */

    public int getNameType() {
        return hostName.hexCode;
    }

/** 
 * Returns a hashcode for this IP address. The hashcode comes from the IP address
 * and is not generated from the string representation. So because NetBIOS nodes
 * can have many names, all names associated with an IP will have the same
 * hashcode.
 */ 

    public int hashCode() {
        return address;
    }

/** 
 * Determines if this address is equal two another. Only the IP Addresses
 * are compared. Similar to the {@link #hashCode} method, the comparison
 * is based on the integer IP address and not the string representation.
 */ 

    public boolean equals( Object obj ) {
        return ( obj != null ) && ( obj instanceof NbtAddress ) &&
                                        ( ((NbtAddress)obj).address == address );
    }

/** 
 * Returns the {@link String} representaion of this address.
 */ 

    public String toString() {
        return hostName.toString() + "/" + getHostAddress();
    }
}
