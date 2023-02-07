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

import jcifs.util.transport.TransportException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

/**
 * This InputStream can read bytes from a file on an SMB file server. Offsets are 64 bits.
 */

public class SmbFileInputStream extends InputStream {

    private long fp;
    private int readSize, openFlags, access;
    private byte[] tmp = new byte[1];

    SmbFile file;

/**
 * Creates an {@link InputStream} for reading bytes from a file on
 * an SMB server addressed by the <code>url</code> parameter. See {@link
 * SmbFile} for a detailed description and examples of the smb
 * URL syntax.
 *
 * @param url An smb URL string representing the file to read from
 */

    public SmbFileInputStream( String url ) throws SmbException, MalformedURLException, UnknownHostException {
        this( new SmbFile( url ));
    }

/**
 * Creates an {@link InputStream} for reading bytes from a file on
 * an SMB server represented by the {@link SmbFile} parameter. See
 * {@link SmbFile} for a detailed description and examples of
 * the smb URL syntax.
 *
 * @param file An <code>SmbFile</code> specifying the file to read from
 */

    public SmbFileInputStream( SmbFile file ) throws SmbException, MalformedURLException, UnknownHostException {
        this( file, SmbFile.O_RDONLY );
    }

    SmbFileInputStream( SmbFile file, int openFlags ) throws SmbException, MalformedURLException, UnknownHostException {
        this.file = file;
        this.openFlags = openFlags & 0xFFFF;
        this.access = (openFlags >>> 16) & 0xFFFF;
        if (file.type != SmbFile.TYPE_NAMED_PIPE) {
            file.open( openFlags, access, SmbFile.ATTR_NORMAL, 0 );
            this.openFlags &= ~(SmbFile.O_CREAT | SmbFile.O_TRUNC);
        } else {
            file.connect0();
        }
        readSize = Math.min( file.tree.session.transport.rcv_buf_size - 70,
                            file.tree.session.transport.server.maxBufferSize - 70 );
    }

    protected IOException seToIoe(SmbException se) {
        IOException ioe = se;
        Throwable root = se.getRootCause();
        if (root instanceof TransportException) {
            ioe = (TransportException)root;
            root = ((TransportException)ioe).getRootCause();
        }
        if (root instanceof InterruptedException) {
            ioe = new InterruptedIOException(root.getMessage());
            ioe.initCause(root);
        }
        return ioe;
    }

/**
 * Closes this input stream and releases any system resources associated with the stream.
 *
 * @throws IOException if a network error occurs
 */

    public void close() throws IOException {
        try {
            file.close();
            tmp = null;
        } catch (SmbException se) {
            throw seToIoe(se);
        }
    }

/**
 * Reads a byte of data from this input stream.
 *
 * @throws IOException if a network error occurs
 */

    public int read() throws IOException {
        // need oplocks to cache otherwise use BufferedInputStream
        if( read( tmp, 0, 1 ) == -1 ) {
            return -1;
        }
        return tmp[0] & 0xFF;
    }

/**
 * Reads up to b.length bytes of data from this input stream into an array of bytes.
 *
 * @throws IOException if a network error occurs
 */

    public int read( byte[] b ) throws IOException {
        return read( b, 0, b.length );
    }

/**
 * Reads up to len bytes of data from this input stream into an array of bytes.
 *
 * @throws IOException if a network error occurs
 */

    public int read( byte[] b, int off, int len ) throws IOException {
        return readDirect(b, off, len);
    }
    public int readDirect( byte[] b, int off, int len ) throws IOException {
        if( len <= 0 ) {
            return 0;
        }
        long start = fp;

        if( tmp == null ) {
            throw new IOException( "Bad file descriptor" );
        }
        // ensure file is open
        file.open( openFlags, access, SmbFile.ATTR_NORMAL, 0 );

        /*
         * Read AndX Request / Response
         */

        if( file.log.level >= 4 )
            file.log.println( "read: fid=" + file.fid + ",off=" + off + ",len=" + len );

        SmbComReadAndXResponse response = new SmbComReadAndXResponse( b, off );

        if( file.type == SmbFile.TYPE_NAMED_PIPE ) {
            response.responseTimeout = 0;
        }

        int r, n;
        do {
            r = len > readSize ? readSize : len;

            if( file.log.level >= 4 )
                file.log.println( "read: len=" + len + ",r=" + r + ",fp=" + fp );

            try {
SmbComReadAndX request = new SmbComReadAndX( file.fid, fp, r, null );
if( file.type == SmbFile.TYPE_NAMED_PIPE ) {
    request.minCount = request.maxCount = request.remaining = 1024;
}
                file.send( request, response );
            } catch( SmbException se ) {
                if( file.type == SmbFile.TYPE_NAMED_PIPE &&
                        se.getNtStatus() == NtStatus.NT_STATUS_PIPE_BROKEN ) {
                    return -1;
                }
                throw seToIoe(se);
            }
            if(( n = response.dataLength ) <= 0 ) {
                return (int)((fp - start) > 0L ? fp - start : -1);
            }
            fp += n;
            len -= n;
            response.off += n;
        } while( len > 0 && n == r );

        return (int)(fp - start);
    }
/**
 * This stream class is unbuffered. Therefore this method will always
 * return 0 for streams connected to regular files. However, a 
 * stream created from a Named Pipe this method will query the server using a
 * "peek named pipe" operation and return the number of available bytes
 * on the server.
 */
    public int available() throws IOException {
        SmbNamedPipe pipe;
        TransPeekNamedPipe req;
        TransPeekNamedPipeResponse resp;

        if( file.type != SmbFile.TYPE_NAMED_PIPE ) {
            return 0;
        }

        try {
            pipe = (SmbNamedPipe)file;
            file.open(SmbFile.O_EXCL, pipe.pipeType & 0xFF0000, SmbFile.ATTR_NORMAL, 0 );
    
            req = new TransPeekNamedPipe( file.unc, file.fid );
            resp = new TransPeekNamedPipeResponse( pipe );
    
            pipe.send( req, resp );
            if( resp.status == TransPeekNamedPipeResponse.STATUS_DISCONNECTED ||
                    resp.status == TransPeekNamedPipeResponse.STATUS_SERVER_END_CLOSED ) {
                file.opened = false;
                return 0;
            }
            return resp.available;
        } catch (SmbException se) {
            throw seToIoe(se);
        }
    }
/**
 * Skip n bytes of data on this stream. This operation will not result
 * in any IO with the server. Unlink <tt>InputStream</tt> value less than
 * the one provided will not be returned if it exceeds the end of the file
 * (if this is a problem let us know).
 */
    public long skip( long n ) throws IOException {
        if (n > 0) {
            fp += n;
            return n;
        }
        return 0;
    }
}

