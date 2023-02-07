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

class SmbTree {

    private static int tree_conn_counter;

    /* 0 - not connected
     * 1 - connecting
     * 2 - connected
     * 3 - disconnecting
     */
    int connectionState;
    int tid;

    String share;
    String service = "?????";
    String service0;
    SmbSession session;
    boolean inDfs, inDomainDfs;
    int tree_num; // used by SmbFile.isOpen

    SmbTree( SmbSession session, String share, String service ) {
        this.session = session;
        this.share = share.toUpperCase();
        if( service != null && service.startsWith( "??" ) == false ) {
            this.service = service;
        }
        this.service0 = this.service;
        this.connectionState = 0;
    }

    boolean matches( String share, String service ) {
        return this.share.equalsIgnoreCase( share ) &&
                ( service == null || service.startsWith( "??" ) ||
                this.service.equalsIgnoreCase( service ));
    }
    public boolean equals(Object obj) {
        if (obj instanceof SmbTree) {
            SmbTree tree = (SmbTree)obj;
            return matches(tree.share, tree.service);
        }
        return false;
    }
    void send( ServerMessageBlock request,
                            ServerMessageBlock response ) throws SmbException {
synchronized (session.transport()) {
        if( response != null ) {
            response.received = false;
        }
        treeConnect( request, response );
        if( request == null || (response != null && response.received )) {
            return;
        }
        if( service.equals( "A:" ) == false ) {
            switch( request.command ) {
                case ServerMessageBlock.SMB_COM_OPEN_ANDX:
                case ServerMessageBlock.SMB_COM_NT_CREATE_ANDX:
                case ServerMessageBlock.SMB_COM_READ_ANDX:
                case ServerMessageBlock.SMB_COM_WRITE_ANDX:
                case ServerMessageBlock.SMB_COM_CLOSE:
                case ServerMessageBlock.SMB_COM_TREE_DISCONNECT:
                    break;
                case ServerMessageBlock.SMB_COM_TRANSACTION:
                case ServerMessageBlock.SMB_COM_TRANSACTION2:
                    switch( ((SmbComTransaction)request).subCommand & 0xFF ) {
                        case SmbComTransaction.NET_SHARE_ENUM:
                        case SmbComTransaction.NET_SERVER_ENUM2:
                        case SmbComTransaction.NET_SERVER_ENUM3:
                        case SmbComTransaction.TRANS_PEEK_NAMED_PIPE:
                        case SmbComTransaction.TRANS_WAIT_NAMED_PIPE:
                        case SmbComTransaction.TRANS_CALL_NAMED_PIPE:
                        case SmbComTransaction.TRANS_TRANSACT_NAMED_PIPE:
                        case SmbComTransaction.TRANS2_GET_DFS_REFERRAL:
                            break;
                        default:
                            throw new SmbException( "Invalid operation for " + service + " service" );
                    }
                    break;
                default:
                    throw new SmbException( "Invalid operation for " + service + " service" + request );
            }
        }
        request.tid = tid;
        if( inDfs && !service.equals("IPC") && request.path != null && request.path.length() > 0 ) {
            /* When DFS is in action all request paths are
             * full UNC paths minus the first backslash like
             *   \server\share\path\to\file
             * as opposed to normally
             *   \path\to\file
             */
            request.flags2 = ServerMessageBlock.FLAGS2_RESOLVE_PATHS_IN_DFS;
            request.path = '\\' + session.transport().tconHostName + '\\' + share + request.path;
        }
        try {
            session.send( request, response );
        } catch( SmbException se ) {
            if (se.getNtStatus() == se.NT_STATUS_NETWORK_NAME_DELETED) {
                /* Someone removed the share while we were
                 * connected. Bastards! Disconnect this tree
                 * so that it reconnects cleanly should the share
                 * reappear in this client's lifetime.
                 */
                treeDisconnect( true );
            }
            throw se;
        }
}
    }
    void treeConnect( ServerMessageBlock andx,
                            ServerMessageBlock andxResponse ) throws SmbException {

synchronized (session.transport()) {
        String unc;

        while (connectionState != 0) {
            if (connectionState == 2 || connectionState == 3) // connected or disconnecting
                return;
            try {
                session.transport.wait();
            } catch (InterruptedException ie) {
                throw new SmbException(ie.getMessage(), ie);
            }
        }
        connectionState = 1; // trying ...

        try {
            /* The hostname to use in the path is only known for
             * sure if the NetBIOS session has been successfully
             * established.
             */
    
            session.transport.connect();

            unc = "\\\\" + session.transport.tconHostName + '\\' + share;
    
            /* IBM iSeries doesn't like specifying a service. Always reset
             * the service to whatever was determined in the constructor.
             */
            service = service0;
    
            /*
             * Tree Connect And X Request / Response
             */
    
            if( session.transport.log.level >= 4 )
                session.transport.log.println( "treeConnect: unc=" + unc + ",service=" + service );
    
            SmbComTreeConnectAndXResponse response =
                    new SmbComTreeConnectAndXResponse( andxResponse );
            SmbComTreeConnectAndX request =
                    new SmbComTreeConnectAndX( session, unc, service, andx );
            session.send( request, response );
    
            tid = response.tid;
            service = response.service;
            inDfs = response.shareIsInDfs;
            tree_num = tree_conn_counter++;
    
            connectionState = 2; // connected
        } catch (SmbException se) {
            treeDisconnect(true);
            connectionState = 0;
            throw se;
        }
}
    }
    void treeDisconnect( boolean inError ) {
synchronized (session.transport()) {

        if (connectionState != 2) // not-connected
            return;
        connectionState = 3; // disconnecting

        if (!inError && tid != 0) {
            try {
                send( new SmbComTreeDisconnect(), null );
            } catch( SmbException se ) {
                if (session.transport.log.level > 1) {
                    se.printStackTrace( session.transport.log );
                }
            }
        }
        inDfs = false;
        inDomainDfs = false;

        connectionState = 0;

        session.transport.notifyAll();
}
    }

    public String toString() {
        return "SmbTree[share=" + share +
            ",service=" + service +
            ",tid=" + tid +
            ",inDfs=" + inDfs +
            ",inDomainDfs=" + inDomainDfs +
            ",connectionState=" + connectionState + "]";
    }
}
