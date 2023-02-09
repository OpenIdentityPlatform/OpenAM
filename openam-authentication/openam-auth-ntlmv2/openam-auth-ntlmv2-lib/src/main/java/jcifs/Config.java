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

import jcifs.util.LogStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * This class uses a static {@link Properties} to act
 * as a cental repository for all jCIFS configuration properties. It cannot be
 * instantiated. Similar to <code>System</code> properties the namespace
 * is global therefore property names should be unique. Before use,
 * the <code>load</code> method should be called with the name of a
 * <code>Properties</code> file (or <code>null</code> indicating no
 * file) to initialize the <code>Config</code>. The <code>System</code>
 * properties will then populate the <code>Config</code> as well potentially
 * overwriting properties from the file. Thus properties provided on the
 * commandline with the <code>-Dproperty.name=value</code> VM parameter
 * will override properties from the configuration file.
 * <p>
 * There are several ways to set jCIFS properties. See
 * the <a href="../overview-summary.html#scp">overview page of the API
 * documentation</a> for details.
 */

public class Config {

public static int socketCount = 0;

    /**
     * The static <code>Properties</code>.
     */

    private static Properties prp = new Properties();
    private static LogStream log;
    public static String DEFAULT_OEM_ENCODING = "Cp850";

    static {
        String filename;
        int level;
        FileInputStream in = null;

        log = LogStream.getInstance();

        try {
            filename = System.getProperty( "jcifs.properties" );
            if( filename != null && filename.length() > 1 ) {
                in = new FileInputStream( filename );
            }
            Config.load( in );
            if (in != null)
                in.close();
        } catch( IOException ioe ) {
            if( log.level > 0 )
                ioe.printStackTrace( log );
        }

        if(( level = Config.getInt( "jcifs.util.loglevel", -1 )) != -1 ) {
            LogStream.setLevel( level );
        }

        try {
            "".getBytes(DEFAULT_OEM_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            if (log.level >= 2) {
                log.println("WARNING: The default OEM encoding " + DEFAULT_OEM_ENCODING +
                " does not appear to be supported by this JRE. The default encoding will be US-ASCII.");
            }
            DEFAULT_OEM_ENCODING = "US-ASCII";
        }

        if (log.level >= 4) {
            try {
                prp.store( log, "JCIFS PROPERTIES" );
            } catch( IOException ioe ) {
            }
        }
    }

    /**
     * This static method registers the SMB URL protocol handler which is
     * required to use SMB URLs with the <tt>java.net.URL</tt> class. If this
     * method is not called before attempting to create an SMB URL with the
     * URL class the following exception will occur:
     * <blockquote><pre>
     * Exception MalformedURLException: unknown protocol: smb
     *     at java.net.URL.<init>(URL.java:480)
     *     at java.net.URL.<init>(URL.java:376)
     *     at java.net.URL.<init>(URL.java:330)
     *     at jcifs.smb.SmbFile.<init>(SmbFile.java:355)
     *     ...
     * </pre><blockquote>
     */

    public static void registerSmbURLHandler() {
        String ver, pkgs;

        ver = System.getProperty( "java.version" );
        if( ver.startsWith( "1.1." ) || ver.startsWith( "1.2." )) {
             throw new RuntimeException( "jcifs-0.7.0b4+ requires Java 1.3 or above. You are running " + ver );
        }
        pkgs = System.getProperty( "java.protocol.handler.pkgs" );
        if( pkgs == null ) {
            System.setProperty( "java.protocol.handler.pkgs", "jcifs" );
        } else if( pkgs.indexOf( "jcifs" ) == -1 ) {
            pkgs += "|jcifs";
            System.setProperty( "java.protocol.handler.pkgs", pkgs );
        }
    }

    // supress javadoc constructor summary by removing 'protected'
    Config() {}

    /**
     * Set the default properties of the static Properties used by <tt>Config</tt>. This permits
     * a different Properties object/file to be used as the source of properties for
     * use by the jCIFS library. The Properties must be set <i>before jCIFS
     * classes are accessed</i> as most jCIFS classes load properties statically once.
     * Using this method will also override properties loaded
     * using the <tt>-Djcifs.properties=</tt> commandline parameter.
     */

    public static void setProperties( Properties prp ) {
        Config.prp = new Properties( prp );
        try {
            Config.prp.putAll( System.getProperties() );
        } catch( SecurityException se ) {
            if( log.level > 1 )
                log.println( "SecurityException: jcifs will ignore System properties" );
        }
    }

    /**
     * Load the <code>Config</code> with properties from the stream
     * <code>in</code> from a <code>Properties</code> file.
     */

    public static void load( InputStream in ) throws IOException {
        if( in != null ) {
            prp.load( in );
        }
        try {
            prp.putAll( (java.util.Map)System.getProperties().clone() );
        } catch( SecurityException se ) {
            if( log.level > 1 )
                log.println( "SecurityException: jcifs will ignore System properties" );
        }
    }

    public static void store( OutputStream out, String header ) throws IOException {
        prp.store( out, header );
    }

    /**
     * List the properties in the <code>Code</code>.
     */

    public static void list( PrintStream out ) throws IOException {
        prp.list( out );
    }

    /**
     * Add a property.
     */

    public static Object setProperty( String key, String value ) {
        return prp.setProperty( key, value );
    }

    /**
     * Retrieve a property as an <code>Object</code>.
     */

    public static Object get( String key ) {
        return prp.get( key );
    }

    /**
     * Retrieve a <code>String</code>. If the key cannot be found,
     * the provided <code>def</code> default parameter will be returned.
     */

    public static String getProperty( String key, String def ) {
        return prp.getProperty( key, def );
    }

    /**
     * Retrieve a <code>String</code>. If the property is not found, <code>null</code> is returned.
     */

    public static String getProperty( String key ) {
        return prp.getProperty( key );
    }

    /**
     * Retrieve an <code>int</code>. If the key does not exist or
     * cannot be converted to an <code>int</code>, the provided default
     * argument will be returned.
     */

    public static int getInt( String key, int def ) {
        String s = prp.getProperty( key );
        if( s != null ) {
            try {
                def = Integer.parseInt( s );
            } catch( NumberFormatException nfe ) {
                if( log.level > 0 )
                    nfe.printStackTrace( log );
            }
        }
        return def;
    }

    /**
     * Retrieve an <code>int</code>. If the property is not found, <code>-1</code> is returned.
     */

    public static int getInt( String key ) {
        String s = prp.getProperty( key );
        int result = -1;
        if( s != null ) {
            try {
                result = Integer.parseInt( s );
            } catch( NumberFormatException nfe ) {
                if( log.level > 0 )
                    nfe.printStackTrace( log );
            }
        }
        return result;
    }

    /**
     * Retrieve a <code>long</code>. If the key does not exist or
     * cannot be converted to a <code>long</code>, the provided default
     * argument will be returned.
     */

    public static long getLong( String key, long def ) {
        String s = prp.getProperty( key );
        if( s != null ) {
            try {
                def = Long.parseLong( s );
            } catch( NumberFormatException nfe ) {
                if( log.level > 0 )
                    nfe.printStackTrace( log );
            }
        }
        return def;
    }

    /** 
     * Retrieve an <code>InetAddress</code>. If the address is not
     * an IP address and cannot be resolved <code>null</code> will
     * be returned.
     */

    public static InetAddress getInetAddress( String key, InetAddress def ) {
        String addr = prp.getProperty( key );
        if( addr != null ) {
            try {
                def = InetAddress.getByName( addr );
            } catch( UnknownHostException uhe ) {
                if( log.level > 0 ) {
                    log.println( addr );
                    uhe.printStackTrace( log );
                }
            }
        }
        return def;
    }
    public static InetAddress getLocalHost() {
        String addr = prp.getProperty( "jcifs.smb.client.laddr" );

        if (addr != null) {
            try {
                return InetAddress.getByName( addr );
            } catch( UnknownHostException uhe ) {
                if( log.level > 0 ) {
                    log.println( "Ignoring jcifs.smb.client.laddr address: " + addr );
                    uhe.printStackTrace( log );
                }
            }
        }

        return null;
    }

    /**
     * Retrieve a boolean value. If the property is not found, the value of <code>def</code> is returned.
     */

    public static boolean getBoolean( String key, boolean def ) {
        String b = getProperty( key );
        if( b != null ) {
            def = b.toLowerCase().equals( "true" );
        }
        return def;
    }

    /**
     * Retrieve an array of <tt>InetAddress</tt> created from a property
     * value containting a <tt>delim</tt> separated list of hostnames and/or
     * ipaddresses.
     */

    public static InetAddress[] getInetAddressArray( String key, String delim, InetAddress[] def ) {
        String p = getProperty( key );
        if( p != null ) {
            StringTokenizer tok = new StringTokenizer( p, delim );
            int len = tok.countTokens();
            InetAddress[] arr = new InetAddress[len];
            for( int i = 0; i < len; i++ ) {
                String addr = tok.nextToken();
                try {
                    arr[i] = InetAddress.getByName( addr );
                } catch( UnknownHostException uhe ) {
                    if( log.level > 0 ) {
                        log.println( addr );
                        uhe.printStackTrace( log );
                    }
                    return def;
                }
            }
            return arr;
        }
        return def;
    }
}

