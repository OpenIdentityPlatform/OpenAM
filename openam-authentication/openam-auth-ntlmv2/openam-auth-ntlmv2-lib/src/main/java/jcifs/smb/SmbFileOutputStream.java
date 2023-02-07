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
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

/**
 * This <code>OutputStream</code> can write bytes to a file on an SMB file server.
 */

public class SmbFileOutputStream extends OutputStream {

    private SmbFile file;
    private boolean append, useNTSmbs;
    private int openFlags, access, writeSize;
    private long fp;
    private byte[] tmp = new byte[1];
    private SmbComWriteAndX reqx;
    private SmbComWriteAndXResponse rspx;
    private SmbComWrite req;
    private SmbComWriteResponse rsp;

/**
 * Creates an {@link OutputStream} for writing to a file
 * on an SMB server addressed by the URL parameter. See {@link
 * SmbFile} for a detailed description and examples of
 * the smb URL syntax.
 *
 * @param url An smb URL string representing the file to write to
 */

    public SmbFileOutputStream( String url ) throws SmbException, MalformedURLException, UnknownHostException {
        this( url, false );
    }

/**
 * Creates an {@link OutputStream} for writing bytes to a file on
 * an SMB server represented by the {@link SmbFile} parameter. See
 * {@link SmbFile} for a detailed description and examples of
 * the smb URL syntax.
 *
 * @param file An <code>SmbFile</code> specifying the file to write to
 */

    public SmbFileOutputStream( SmbFile file ) throws SmbException, MalformedURLException, UnknownHostException {
        this( file, false );
    }

/**
 * Creates an {@link OutputStream} for writing bytes to a file on an
 * SMB server addressed by the URL parameter. See {@link SmbFile}
 * for a detailed description and examples of the smb URL syntax. If the
 * second argument is <code>true</code>, then bytes will be written to the
 * end of the file rather than the beginning.
 *
 * @param url An smb URL string representing the file to write to
 * @param append Append to the end of file
 */

    public SmbFileOutputStream( String url, boolean append ) throws SmbException, MalformedURLException, UnknownHostException {
        this( new SmbFile( url ), append );
    }

/**
 * Creates an {@link OutputStream} for writing bytes to a file
 * on an SMB server addressed by the <code>SmbFile</code> parameter. See
 * {@link SmbFile} for a detailed description and examples of
 * the smb URL syntax. If the second argument is <code>true</code>, then
 * bytes will be written to the end of the file rather than the beginning.
 * 
 * @param file An <code>SmbFile</code> representing the file to write to
 * @param append Append to the end of file
 */

    public SmbFileOutputStream( SmbFile file, boolean append ) throws SmbException, MalformedURLException, UnknownHostException {
        this( file, append, append ? SmbFile.O_CREAT | SmbFile.O_WRONLY | SmbFile.O_APPEND :
                                    SmbFile.O_CREAT | SmbFile.O_WRONLY | SmbFile.O_TRUNC );
    }
/**
 * Creates an {@link OutputStream} for writing bytes to a file
 * on an SMB server addressed by the <code>SmbFile</code> parameter. See
 * {@link SmbFile} for a detailed description and examples of
 * the smb URL syntax.
<p>
The second parameter specifies how the file should be shared. If
<code>SmbFile.FILE_NO_SHARE</code> is specified the client will
have exclusive access to the file. An additional open command
from jCIFS or another application will fail with the "file is being
accessed by another process" error. The <code>FILE_SHARE_READ</code>,
<code>FILE_SHARE_WRITE</code>, and <code>FILE_SHARE_DELETE</code> may be
combined with the bitwise OR '|' to specify that other peocesses may read,
write, and/or delete the file while the jCIFS user has the file open.
 * 
 * @param url An smb URL representing the file to write to
 * @param shareAccess File sharing flag: <code>SmbFile.FILE_NOSHARE</code> or any combination of <code>SmbFile.FILE_READ</code>, <code>SmbFile.FILE_WRITE</code>, and <code>SmbFile.FILE_DELETE</code>
 */

    public SmbFileOutputStream( String url, int shareAccess ) throws SmbException, MalformedURLException, UnknownHostException {
        this( new SmbFile( url, "", null, shareAccess ), false );
    }

    SmbFileOutputStream( SmbFile file, boolean append, int openFlags ) throws SmbException, MalformedURLException, UnknownHostException {
        this.file = file;
        this.append = append;
        this.openFlags = openFlags;
        this.access = (openFlags >>> 16) & 0xFFFF;
        if( append ) {
            try {
                fp = file.length();
            } catch( SmbAuthException sae ) {
                throw sae;
            } catch( SmbException se ) {
                fp = 0L;
            }
        }
        if( file instanceof SmbNamedPipe && file.unc.startsWith( "\\pipe\\" )) {
            file.unc = file.unc.substring( 5 );
            file.send( new TransWaitNamedPipe( "\\pipe" + file.unc ),
                                        new TransWaitNamedPipeResponse() );
        }
        file.open( openFlags, access | SmbConstants.FILE_WRITE_DATA, SmbFile.ATTR_NORMAL, 0 );
        this.openFlags &= ~(SmbFile.O_CREAT | SmbFile.O_TRUNC); /* in case close and reopen */
        writeSize = file.tree.session.transport.snd_buf_size - 70;

        useNTSmbs = file.tree.session.transport.hasCapability( ServerMessageBlock.CAP_NT_SMBS );
        if( useNTSmbs ) {
            reqx = new SmbComWriteAndX();
            rspx = new SmbComWriteAndXResponse();
        } else {
            req = new SmbComWrite();
            rsp = new SmbComWriteResponse();    
        }
    }

/**
 * Closes this output stream and releases any system resources associated
 * with it.
 *
 * @throws IOException if a network error occurs
 */

    public void close() throws IOException {
        file.close();
        tmp = null;
    }

/**
 * Writes the specified byte to this file output stream.
 *
 * @throws IOException if a network error occurs
 */

    public void write( int b ) throws IOException {
        tmp[0] = (byte)b;
        write( tmp, 0, 1 );
    }

/**
 * Writes b.length bytes from the specified byte array to this
 * file output stream.
 *
 * @throws IOException if a network error occurs
 */

    public void write( byte[] b ) throws IOException {
        write( b, 0, b.length );
    }

    public boolean isOpen()
    {
        return file.isOpen();
    }
    void ensureOpen() throws IOException {
        // ensure file is open
        if( file.isOpen() == false ) {
            file.open( openFlags, access | SmbConstants.FILE_WRITE_DATA, SmbFile.ATTR_NORMAL, 0 );
            if( append ) {
                fp = file.length();
            }
        }
    }
/**
 * Writes len bytes from the specified byte array starting at
 * offset off to this file output stream.
 *
 * @param b The array 
 * @throws IOException if a network error occurs
 */

    public void write( byte[] b, int off, int len ) throws IOException {
        if( file.isOpen() == false && file instanceof SmbNamedPipe ) {
            file.send( new TransWaitNamedPipe( "\\pipe" + file.unc ),
                                    new TransWaitNamedPipeResponse() );
        }
        writeDirect( b, off, len, 0 );
    }
/**
 * Just bypasses TransWaitNamedPipe - used by DCERPC bind.
 */
    public void writeDirect( byte[] b, int off, int len, int flags ) throws IOException {
        if( len <= 0 ) {
            return;
        }

        if( tmp == null ) {
            throw new IOException( "Bad file descriptor" );
        }
        ensureOpen();

        if( file.log.level >= 4 )
            file.log.println( "write: fid=" + file.fid + ",off=" + off + ",len=" + len );

        int w;
        do {
            w = len > writeSize ? writeSize : len;
            if( useNTSmbs ) {
                reqx.setParam( file.fid, fp, len - w, b, off, w );
if ((flags & 1) != 0) {
    reqx.setParam( file.fid, fp, len, b, off, w );
    reqx.writeMode = 0x8;
} else {
    reqx.writeMode = 0;
}
                file.send( reqx, rspx );
                fp += rspx.count;
                len -= rspx.count;
                off += rspx.count;
            } else {
                req.setParam( file.fid, fp, len - w, b, off, w );
                fp += rsp.count;
                len -= rsp.count;
                off += rsp.count;
                file.send( req, rsp );
            }
        } while( len > 0 );
    }
}

