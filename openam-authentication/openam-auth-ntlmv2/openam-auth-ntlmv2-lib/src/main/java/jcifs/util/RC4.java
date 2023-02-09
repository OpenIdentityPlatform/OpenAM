/* Copyright (C) 2009 "Michael B Allen" <jcifs at samba dot org>
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

public class RC4
{

    byte[] s;
    int i, j;

    public RC4()
    {
    }
    public RC4(byte[] key)
    {
        init(key, 0, key.length);
    }

    public void init(byte[] key, int ki, int klen)
    {
        s = new byte[256];

        for (i = 0; i < 256; i++)
            s[i] = (byte)i;

        for (i = j = 0; i < 256; i++) {
            j = (j + key[ki + i % klen] + s[i]) & 0xff;
            byte t = s[i];
            s[i] = s[j];
            s[j] = t;
        }

        i = j = 0;
    }
    public void update(byte[] src, int soff, int slen, byte[] dst, int doff)
    {
        int slim;

        slim = soff + slen;
        while (soff < slim) {
            i = (i + 1) & 0xff;
            j = (j + s[i]) & 0xff;
            byte t = s[i];
            s[i] = s[j];
            s[j] = t;
            dst[doff++] = (byte)(src[soff++] ^ s[(s[i] + s[j]) & 0xff]);
        }
    }
}
