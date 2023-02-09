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

package jcifs.smb;

import jcifs.Config;

public class BufferCache {

    private static final int MAX_BUFFERS = Config.getInt( "jcifs.smb.maxBuffers", 16 );

    static Object[] cache = new Object[MAX_BUFFERS];
    private static int freeBuffers = 0;

    private static byte[] getBuffer0() {
        byte[] buf;

        if (freeBuffers > 0) {
            for (int i = 0; i < MAX_BUFFERS; i++) {
                if( cache[i] != null ) {
                    buf = (byte[])cache[i];
                    cache[i] = null;
                    freeBuffers--;
                    return buf;
                }
            }
        }

        buf = new byte[SmbComTransaction.TRANSACTION_BUF_SIZE];

        return buf;
    }

    static void getBuffers( SmbComTransaction req,
                    SmbComTransactionResponse rsp ) throws InterruptedException {
        synchronized( cache ) {
            if (freeBuffers < 2) {
                /* The first time this is called we always wait because freeBuffers
                 * will be 0. But after a few calls to releaseBuffer, threads will
                 * no longer wait.
                 */
                cache.wait(100);
            }
            req.txn_buf = getBuffer0();
            rsp.txn_buf = getBuffer0();
        }
    }
    static public byte[] getBuffer() throws InterruptedException {
        synchronized( cache ) {
            if (freeBuffers < 1) {
                cache.wait(100);
            }
            return getBuffer0();
        }
    }
    static public void releaseBuffer( byte[] buf ) {
        synchronized( cache ) {
            for (int i = 0; i < MAX_BUFFERS; i++) {
                if (cache[i] == null) {
                    cache[i] = buf;
                    freeBuffers++;
                    cache.notify();
                    return;
                }
            }
        }
    }
}
