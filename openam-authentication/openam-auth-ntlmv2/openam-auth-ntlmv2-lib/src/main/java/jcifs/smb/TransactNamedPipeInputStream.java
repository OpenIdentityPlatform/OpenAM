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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

class TransactNamedPipeInputStream extends SmbFileInputStream {

    private static final int INIT_PIPE_SIZE = 4096;

    private byte[] pipe_buf = new byte[INIT_PIPE_SIZE];
    private int beg_idx, nxt_idx, used;
    private boolean dcePipe;

    Object lock;

    TransactNamedPipeInputStream( SmbNamedPipe pipe ) throws SmbException,
                MalformedURLException, UnknownHostException {
        super( pipe, ( pipe.pipeType & 0xFFFF00FF ) | SmbFile.O_EXCL );
        this.dcePipe = ( pipe.pipeType & SmbNamedPipe.PIPE_TYPE_DCE_TRANSACT ) != SmbNamedPipe.PIPE_TYPE_DCE_TRANSACT;
        lock = new Object();
    }
    public int read() throws IOException {
        int result = -1;

        synchronized( lock ) {
            try {
                while( used == 0 ) {
                    lock.wait();
                }
            } catch( InterruptedException ie ) {
                throw new IOException( ie.getMessage() );
            }
            result = pipe_buf[beg_idx] & 0xFF;
            beg_idx = ( beg_idx + 1 ) % pipe_buf.length;
        }
        return result;
    }
    public int read( byte[] b ) throws IOException {
        return read( b, 0, b.length );
    }
    public int read( byte[] b, int off, int len ) throws IOException {
        int result = -1;
        int i;

        if( len <= 0 ) {
            return 0;
        }
        synchronized( lock ) {
            try {
                while( used == 0 ) {
                    lock.wait();
                }
            } catch( InterruptedException ie ) {
                throw new IOException( ie.getMessage() );
            }
            i = pipe_buf.length - beg_idx;
            result = len > used ? used : len;
            if( used > i && result > i ) {
                System.arraycopy( pipe_buf, beg_idx, b, off, i );
                off += i;
                System.arraycopy( pipe_buf, 0, b, off, result - i );
            } else {
                System.arraycopy( pipe_buf, beg_idx, b, off, result );
            }
            used -= result;
            beg_idx = ( beg_idx + result ) % pipe_buf.length;
        }

        return result;
    }
    public int available() throws IOException {
        if( file.log.level >= 3 )
            file.log.println( "Named Pipe available() does not apply to TRANSACT Named Pipes" );
        return 0;
    }
    int receive( byte[] b, int off, int len ) {
        int i;

        if( len > ( pipe_buf.length - used )) {
            byte[] tmp;
            int new_size;

            new_size = pipe_buf.length * 2;
            if( len > ( new_size - used )) {
                new_size = len + used;
            }
            tmp = pipe_buf;
            pipe_buf = new byte[new_size];
            i = tmp.length - beg_idx;
            if( used > i ) { /* 2 chunks */
                System.arraycopy( tmp, beg_idx, pipe_buf, 0, i );
                System.arraycopy( tmp, 0, pipe_buf, i, used - i ); 
            } else {
                System.arraycopy( tmp, beg_idx, pipe_buf, 0, used );
            }
            beg_idx = 0;
            nxt_idx = used;
            tmp = null;
        }

        i = pipe_buf.length - nxt_idx;
        if( len > i ) {
            System.arraycopy( b, off, pipe_buf, nxt_idx, i );
            off += i;
            System.arraycopy( b, off, pipe_buf, 0, len - i ); 
        } else {
            System.arraycopy( b, off, pipe_buf, nxt_idx, len );
        }
        nxt_idx = ( nxt_idx + len ) % pipe_buf.length;
        used += len;
        return len;
    }
    public int dce_read( byte[] b, int off, int len ) throws IOException {
        return super.read(b, off, len);
    }
}
