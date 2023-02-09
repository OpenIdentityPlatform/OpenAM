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

package jcifs.util;

import java.io.PrintStream;

/**
 */

public class Hexdump {

    private static final String NL = System.getProperty( "line.separator" );
    private static final int NL_LENGTH = NL.length();

    private static final char[] SPACE_CHARS = {
        ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
        ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
        ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '
    };

    public static final char[] HEX_DIGITS = { 
        '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'A', 'B',
        'C', 'D', 'E', 'F'
    };

/** 
 * Generate "hexdump" output of the buffer at src like the following:
 *
 * <p><blockquote><pre>
 * 00000: 04 d2 29 00 00 01 00 00 00 00 00 01 20 45 47 46  |..)......... EGF|
 * 00010: 43 45 46 45 45 43 41 43 41 43 41 43 41 43 41 43  |CEFEECACACACACAC|
 * 00020: 41 43 41 43 41 43 41 43 41 43 41 41 44 00 00 20  |ACACACACACAAD.. |
 * 00030: 00 01 c0 0c 00 20 00 01 00 00 00 00 00 06 20 00  |..... ........ .|
 * 00040: ac 22 22 e1                                      |."".            |
 * </blockquote></pre>
 */

    public static void hexdump( PrintStream ps, byte[] src, int srcIndex, int length ) {
        if( length == 0 ) {
            return;
        }

        int s = length % 16;
        int r = ( s == 0 ) ? length / 16 : length / 16 + 1;
        char[] c = new char[r * (74 + NL_LENGTH)];
        char[] d = new char[16];
        int i;
        int si = 0;
        int ci = 0;

        do {
            toHexChars( si, c, ci, 5 );
            ci += 5;
            c[ci++] = ':';
            do {
                if( si == length ) {
                    int n = 16 - s;
                    System.arraycopy( SPACE_CHARS, 0, c, ci, n * 3 );
                    ci += n * 3;
                    System.arraycopy( SPACE_CHARS, 0, d, s, n );
                    break;
                }
                c[ci++] = ' ';
                i = src[srcIndex + si] & 0xFF;
                toHexChars( i, c, ci, 2 );
                ci += 2; 
                if( i < 0 || Character.isISOControl( (char)i )) {
                    d[si % 16] = '.';
                } else {
                    d[si % 16] = (char)i;
                }
            } while(( ++si % 16 ) != 0 );
            c[ci++] = ' ';
            c[ci++] = ' ';
            c[ci++] = '|';
            System.arraycopy( d, 0, c, ci, 16 );
            ci += 16;
            c[ci++] = '|';
            NL.getChars( 0, NL_LENGTH, c, ci );
            ci += NL_LENGTH;
        } while( si < length );

        ps.println( c );
    }

/** 
 * This is an alternative to the <code>java.lang.Integer.toHexString</cod>
 * method. It is an efficient relative that also will pad the left side so
 * that the result is <code>size</code> digits.
 */ 
    public static String toHexString( int val, int size ) {
        char[] c = new char[size];
        toHexChars( val, c, 0, size );
        return new String( c );
    }
    public static String toHexString( long val, int size ) {
        char[] c = new char[size];
        toHexChars( val, c, 0, size );
        return new String( c );
    }
    public static String toHexString( byte[] src, int srcIndex, int size ) {
        char[] c = new char[size];
        size = ( size % 2 == 0 ) ? size / 2 : size / 2 + 1;
        for( int i = 0, j = 0; i < size; i++ ) {
            c[j++] = HEX_DIGITS[(src[i] >> 4 ) & 0x0F];
            if( j == c.length ) {
                break;
            }
            c[j++] = HEX_DIGITS[src[i] & 0x0F];
        }
        return new String( c );
    }

/** 
 * This is the same as {@link Hexdump#toHexString(int val, int
 * size)} but provides a more practical form when trying to avoid {@link
 * String} concatenation and {@link StringBuffer}.
 */ 
    public static void toHexChars( int val, char dst[], int dstIndex, int size ) {
        while( size > 0 ) {
            int i = dstIndex + size - 1;
            if( i < dst.length ) {
                dst[i] = HEX_DIGITS[val & 0x000F];
            }
            if( val != 0 ) {
                val >>>= 4;
            }
            size--;
        }
    }
    public static void toHexChars( long val, char dst[], int dstIndex, int size ) {
        while( size > 0 ) {
            dst[dstIndex + size - 1] = HEX_DIGITS[(int)( val & 0x000FL )];
            if( val != 0 ) {
                val >>>= 4;
            }
            size--;
        }
    }
}

