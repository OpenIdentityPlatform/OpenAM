/* jcifs msrpc client library in Java
 * Copyright (C) 2006  "Michael B. Allen" <jcifs at samba dot org>
 *                     "Eric Glass" <jcifs at samba dot org>
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

package jcifs.dcerpc;

public class UUID extends rpc.uuid_t {

    public static int hex_to_bin(char[] arr, int offset, int length) {
        int value = 0;
        int ai, count;

        count = 0;
        for (ai = offset; ai < arr.length && count < length; ai++) {
            value <<= 4;
            switch (arr[ai]) {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    value += arr[ai] - '0';
                    break;
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                    value += 10 + (arr[ai] - 'A');
                    break;
                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                    value += 10 + (arr[ai] - 'a');
                    break;
                default:
                    throw new IllegalArgumentException(new String(arr, offset, length));
            }
            count++;
        }

        return value;
    }
    static final char[] HEXCHARS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    public static String bin_to_hex(int value, int length) {
        char[] arr = new char[length];
        int ai = arr.length;
        while (ai-- > 0) {
            arr[ai] = HEXCHARS[value & 0xF];
            value >>>= 4;
        }
        return new String(arr);
    }
    private static byte B(int i) { return (byte)(i & 0xFF); }
    private static short S(int i) { return (short)(i & 0xFFFF); }

    public UUID(rpc.uuid_t uuid) {
        time_low = uuid.time_low;
        time_mid = uuid.time_mid;
        time_hi_and_version = uuid.time_hi_and_version;
        clock_seq_hi_and_reserved = uuid.clock_seq_hi_and_reserved;
        clock_seq_low = uuid.clock_seq_low;
        node = new byte[6];
        node[0] = uuid.node[0];
        node[1] = uuid.node[1];
        node[2] = uuid.node[2];
        node[3] = uuid.node[3];
        node[4] = uuid.node[4];
        node[5] = uuid.node[5];
    }
    public UUID(String str) {
        char[] arr = str.toCharArray();
        time_low = hex_to_bin(arr, 0, 8);
        time_mid = S(hex_to_bin(arr, 9, 4));
        time_hi_and_version = S(hex_to_bin(arr, 14, 4));
        clock_seq_hi_and_reserved = B(hex_to_bin(arr, 19, 2));
        clock_seq_low = B(hex_to_bin(arr, 21, 2));
        node = new byte[6];
        node[0] = B(hex_to_bin(arr, 24, 2));
        node[1] = B(hex_to_bin(arr, 26, 2));
        node[2] = B(hex_to_bin(arr, 28, 2));
        node[3] = B(hex_to_bin(arr, 30, 2));
        node[4] = B(hex_to_bin(arr, 32, 2));
        node[5] = B(hex_to_bin(arr, 34, 2));
    }

    public String toString() {
        return bin_to_hex(time_low, 8) + '-' +
                bin_to_hex(time_mid, 4) + '-' +
                bin_to_hex(time_hi_and_version, 4) + '-' +
                bin_to_hex(clock_seq_hi_and_reserved, 2) +
                bin_to_hex(clock_seq_low, 2) + '-' +
                bin_to_hex(node[0], 2) +
                bin_to_hex(node[1], 2) +
                bin_to_hex(node[2], 2) +
                bin_to_hex(node[3], 2) +
                bin_to_hex(node[4], 2) +
                bin_to_hex(node[5], 2);
    }
}
