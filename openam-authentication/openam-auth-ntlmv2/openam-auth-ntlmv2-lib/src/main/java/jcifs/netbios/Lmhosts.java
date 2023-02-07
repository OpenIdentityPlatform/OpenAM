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
import jcifs.smb.SmbFileInputStream;
import jcifs.util.LogStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.UnknownHostException;
import java.util.Hashtable;

public class Lmhosts {

    private static final String FILENAME = Config.getProperty( "jcifs.netbios.lmhosts" );
    private static final Hashtable TAB = new Hashtable();
    private static long lastModified = 1L;
    private static int alt;
    private static LogStream log = LogStream.getInstance();

    /**
     * This is really just for {@link jcifs.UniAddress}. It does
     * not throw an {@link UnknownHostException} because this
     * is queried frequently and exceptions would be rather costly to
     * throw on a regular basis here.
     */

    public synchronized static NbtAddress getByName( String host ) {
        return getByName( new Name( host, 0x20, null ));
    }

    synchronized static NbtAddress getByName( Name name ) {
        NbtAddress result = null;

        try {
            if( FILENAME != null ) {
                File f = new File( FILENAME );
                long lm;

                if(( lm = f.lastModified() ) > lastModified ) {
                    lastModified = lm;
                    TAB.clear();
                    alt = 0;
                    populate( new FileReader( f ));
                }
                result = (NbtAddress)TAB.get( name );
            }
        } catch( FileNotFoundException fnfe ) {
            if( log.level > 1 ) {
                log.println( "lmhosts file: " + FILENAME );
                fnfe.printStackTrace( log );
            }
        } catch( IOException ioe ) {
            if( log.level > 0 )
                ioe.printStackTrace( log );
        }
        return result;
    }

    static void populate( Reader r ) throws IOException {
        String line;
        BufferedReader br = new BufferedReader( r );

        while(( line = br.readLine() ) != null ) {
            line = line.toUpperCase().trim();
            if( line.length() == 0 ) {
                continue;
            } else if( line.charAt( 0 ) == '#' ) {
                if( line.startsWith( "#INCLUDE " )) {
                    line = line.substring( line.indexOf( '\\' ));
                    String url = "smb:" + line.replace( '\\', '/' );

                    if( alt > 0 ) {
                        try {
                            populate( new InputStreamReader( new SmbFileInputStream( url )));
                        } catch( IOException ioe ) {
                            log.println( "lmhosts URL: " + url );
                            ioe.printStackTrace( log );
                            continue;
                        }

                        /* An include was loaded successfully. We can skip
                         * all other includes up to the #END_ALTERNATE tag.
                         */

                        alt--;
                        while(( line = br.readLine() ) != null ) {
                            line = line.toUpperCase().trim();
                            if( line.startsWith( "#END_ALTERNATE" )) {
                                break;
                            }
                        }
                    } else {
                        populate( new InputStreamReader( new SmbFileInputStream( url )));
                    }
                } else if( line.startsWith( "#BEGIN_ALTERNATE" )) {
                    alt++;
                } else if( line.startsWith( "#END_ALTERNATE" ) && alt > 0 ) {
                    alt--;
                    throw new IOException( "no lmhosts alternate includes loaded" );
                }
            } else if( Character.isDigit( line.charAt( 0 ))) {
                char[] data = line.toCharArray();
                int ip, i, j;
                Name name;
                NbtAddress addr;
                char c;

                c = '.';
                ip = i = 0;
                for( ; i < data.length && c == '.'; i++ ) {
                    int b = 0x00;

                    for( ; i < data.length && ( c = data[i] ) >= 48 && c <= 57; i++ ) {
                        b = b * 10 + c - '0';
                    }
                    ip = ( ip << 8 ) + b;
                }
                while( i < data.length && Character.isWhitespace( data[i] )) {
                    i++;
                }
                j = i;
                while( j < data.length && Character.isWhitespace( data[j] ) == false ) {
                    j++;
                }

                name = new Name( line.substring( i, j ), 0x20, null );
                addr = new NbtAddress( name, ip, false, NbtAddress.B_NODE,
                                    false, false, true, true,
                                    NbtAddress.UNKNOWN_MAC_ADDRESS );
                TAB.put( name, addr );
            }
        }
    }
}
