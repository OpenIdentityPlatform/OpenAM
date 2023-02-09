/* jcifs smb client library in Java
 * Copyright (C) 2002  "Michael B. Allen" <jcifs at samba dot org>
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

package jcifs.util;

import java.io.IOException;
import java.io.InputStream;

public class MimeMap {

    private static final int IN_SIZE = 7000;

    private static final int ST_START = 1;
    private static final int ST_COMM = 2;
    private static final int ST_TYPE = 3;
    private static final int ST_GAP = 4;
    private static final int ST_EXT = 5;

    private byte[] in;
    private int inLen;

    public MimeMap() throws IOException {
        int n;

        in = new byte[IN_SIZE];
        InputStream is = getClass().getClassLoader().getResourceAsStream( "jcifs/util/mime.map" );

        inLen = 0;
        while(( n = is.read( in, inLen, IN_SIZE - inLen )) != -1 ) {
            inLen += n;
        }
        if( inLen < 100 || inLen == IN_SIZE ) {
            throw new IOException( "Error reading jcifs/util/mime.map resource" );
        }
        is.close();
    }

    public String getMimeType( String extension ) throws IOException {
        return getMimeType( extension, "application/octet-stream" );
    }
    public String getMimeType( String extension, String def ) throws IOException {
        int state, t, x, i, off;
        byte ch;
        byte[] type = new byte[128];
        byte[] buf = new byte[16];
        byte[] ext = extension.toLowerCase().getBytes( "ASCII" );

        state = ST_START;
        t = x = i = 0;
        for( off = 0; off < inLen; off++ ) {
            ch = in[off];
            switch( state ) {
                case ST_START:
                    if( ch == ' ' || ch == '\t' ) {
                        break;
                    } else if( ch == '#' ) {
                        state = ST_COMM;
                        break;
                    }
                    state = ST_TYPE;
                case ST_TYPE:
                    if( ch == ' ' || ch == '\t' ) {
                        state = ST_GAP;
                    } else {
                        type[t++] = ch;
                    }
                    break;
                case ST_COMM:
                    if( ch == '\n' ) {
                        t = x = i = 0;
                        state = ST_START;
                    }
                    break;
                case ST_GAP:
                    if( ch == ' ' || ch == '\t' ) {
                        break;
                    }
                    state = ST_EXT;
                case ST_EXT:
                    switch( ch ) {
                        case ' ':
                        case '\t':
                        case '\n':
                        case '#':
                            for( i = 0; i < x && x == ext.length && buf[i] == ext[i]; i++ ) {
                                ;
                            }
                            if( i == ext.length ) {
                                return new String( type, 0, t, "ASCII" );
                            }
                            if( ch == '#' ) {
                                state = ST_COMM;
                            } else if( ch == '\n' ) {
                                t = x = i = 0;
                                state = ST_START;
                            }
                            x = 0;
                            break;
                        default:
                            buf[x++] = ch;
                    }
                    break;
            }
        }
        return def;
    }
}

