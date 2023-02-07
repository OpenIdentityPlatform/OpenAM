/* jcifs smb client library in Java
 * Copyright (C) 2000  "Michael B. Allen" <jcifs at samba dot org>
 *                     "Christopher R. Hertel" <jcifs at samba dot org>
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

import java.io.UnsupportedEncodingException;

public class Name {

    private static final int TYPE_OFFSET = 31;
    private static final int SCOPE_OFFSET = 33;
    private static final String DEFAULT_SCOPE = Config.getProperty( "jcifs.netbios.scope" );

    static final String OEM_ENCODING =
                Config.getProperty( "jcifs.encoding",
                        System.getProperty( "file.encoding" ));

    public String name, scope;
    public int hexCode;
    int srcHashCode; /* srcHashCode must be set by name resolution
                      * routines before entry into addressCache
                      */

    Name() {
    }
    public Name( String name, int hexCode, String scope ) {
        if( name.length() > 15 ) {
            name = name.substring( 0, 15 );
        }
        this.name = name.toUpperCase();
        this.hexCode = hexCode;
        this.scope = scope != null && scope.length() > 0 ? scope : DEFAULT_SCOPE;
        this.srcHashCode = 0;
    }

    int writeWireFormat( byte[] dst, int dstIndex ) {
        // write 0x20 in first byte
        dst[dstIndex] = 0x20;

        // write name
        try {
            byte tmp[] = name.getBytes( Name.OEM_ENCODING );
            int i;
            for( i = 0; i < tmp.length; i++ ) {
                dst[dstIndex + ( 2 * i + 1 )] = (byte)((( tmp[i] & 0xF0 ) >> 4 ) + 0x41 );
                dst[dstIndex + ( 2 * i + 2 )] = (byte)(( tmp[i] & 0x0F ) + 0x41 );
            }
            for( ; i < 15; i++ ) {
                dst[dstIndex + ( 2 * i + 1 )] = (byte)0x43;
                dst[dstIndex + ( 2 * i + 2 )] = (byte)0x41;
            }
            dst[dstIndex + TYPE_OFFSET] = (byte)((( hexCode & 0xF0 ) >> 4 ) + 0x41 );
            dst[dstIndex + TYPE_OFFSET + 1] = (byte)(( hexCode & 0x0F ) + 0x41 );
        } catch( UnsupportedEncodingException uee ) {
        }
        return SCOPE_OFFSET + writeScopeWireFormat( dst, dstIndex + SCOPE_OFFSET );
    }

    int readWireFormat( byte[] src, int srcIndex ) {

        byte tmp[] = new byte[SCOPE_OFFSET];
        int length = 15;
        for( int i = 0; i < 15; i++ ) {
            tmp[i] = (byte)((( src[srcIndex + ( 2 * i + 1 )] & 0xFF ) - 0x41 ) << 4 );
            tmp[i] |= (byte)((( src[srcIndex + ( 2 * i + 2 )] & 0xFF ) - 0x41 ) & 0x0F );
            if( tmp[i] != (byte)' ' ) {
                length = i + 1;
            }
        }
        try {
            name = new String( tmp, 0, length, Name.OEM_ENCODING );
        } catch( UnsupportedEncodingException uee ) {
        }
        hexCode = (( src[srcIndex + TYPE_OFFSET] & 0xFF ) - 0x41 ) << 4;
        hexCode |= (( src[srcIndex + TYPE_OFFSET + 1] & 0xFF ) - 0x41 ) & 0x0F;
        return SCOPE_OFFSET + readScopeWireFormat( src, srcIndex + SCOPE_OFFSET );
    }
    int writeScopeWireFormat( byte[] dst, int dstIndex ) {
        if( scope == null ) {
            dst[dstIndex] = (byte)0x00;
            return 1;
        }

        // copy new scope in
        dst[dstIndex++] = (byte)'.';
        try {
            System.arraycopy( scope.getBytes( Name.OEM_ENCODING ), 0, dst, dstIndex, scope.length() );
        } catch( UnsupportedEncodingException uee ) {
        }
        dstIndex += scope.length();

        dst[dstIndex++] = (byte)0x00;

        // now go over scope backwards converting '.' to label length

        int i = dstIndex - 2;
        int e = i - scope.length();
        int c = 0;

        do {
            if( dst[i] == '.' ) {
                dst[i] = (byte)c;
                c = 0;
            } else {
                c++;
            }
        } while( i-- > e );
        return scope.length() + 2;
    }
    int readScopeWireFormat( byte[] src, int srcIndex ) {
        int start = srcIndex;
        int n;
        StringBuffer sb;

        if(( n = src[srcIndex++] & 0xFF ) == 0 ) {
            scope = null;
            return 1;
        }

        try {
            sb = new StringBuffer( new String( src, srcIndex, n, Name.OEM_ENCODING ));
            srcIndex += n;
            while(( n = src[srcIndex++] & 0xFF ) != 0 ) {
                sb.append( '.' ).append( new String( src, srcIndex, n, Name.OEM_ENCODING ));
                srcIndex += n;
            }
            scope = sb.toString();
        } catch( UnsupportedEncodingException uee ) {
        }

        return srcIndex - start;
    }

    public int hashCode() {
        int result;

        result = name.hashCode();
        result += 65599 * hexCode;
        result += 65599 * srcHashCode; /* hashCode is different depending
                                        * on where it came from
                                        */
        if( scope != null && scope.length() != 0 ) {
            result += scope.hashCode();
        }
        return result;
    }
    public boolean equals( Object obj ) {
        Name n;

        if( !( obj instanceof Name )) {
            return false;
        }
        n = (Name)obj;
        if( scope == null && n.scope == null ) {
            return name.equals( n.name ) && hexCode == n.hexCode;
        }
        return name.equals( n.name ) && hexCode == n.hexCode && scope.equals( n.scope );
    }
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String n = name;

        // fix MSBROWSE name
        if( n == null ) {
            n = "null";
        } else if( n.charAt( 0 ) == 0x01 ) {
            char c[] = n.toCharArray();
            c[0] = '.';
            c[1] = '.';
            c[14] = '.';
            n = new String( c );
        }

        sb.append( n ).append( "<" ).append( Hexdump.toHexString( hexCode, 2 )).append( ">" );
        if( scope != null ) {
            sb.append( "." ).append( scope );
        }
        return sb.toString();
    }
}

