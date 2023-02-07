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

public class UnicodeString extends rpc.unicode_string {

    boolean zterm;

    public UnicodeString(boolean zterm) {
        this.zterm = zterm;
    }
    public UnicodeString(rpc.unicode_string rus, boolean zterm) {
        this.length = rus.length;
        this.maximum_length = rus.maximum_length;
        this.buffer = rus.buffer;
        this.zterm = zterm;
    }

    public UnicodeString(String str, boolean zterm) {
        this.zterm = zterm;

        int len = str.length();
        int zt = zterm ? 1 : 0;

        length = maximum_length = (short)((len + zt) * 2);
        buffer = new short[len + zt];

        int i;
        for (i = 0; i < len; i++) {
            buffer[i] = (short)str.charAt(i);
        }
        if (zterm) {
            buffer[i] = (short)0;
        }
    }

    public String toString() {
        int len = length / 2 - (zterm ? 1 : 0);
        char[] ca = new char[len];
        for (int i = 0; i < len; i++) {
            ca[i] = (char)buffer[i];
        }
        return new String(ca, 0, len);
    }
}
