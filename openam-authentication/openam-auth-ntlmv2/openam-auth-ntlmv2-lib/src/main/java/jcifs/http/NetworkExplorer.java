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

package jcifs.http;

import jcifs.Config;
import jcifs.UniAddress;
import jcifs.netbios.NbtAddress;
import jcifs.smb.DfsReferral;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbSession;
import jcifs.util.Base64;
import jcifs.util.LogStream;
import jcifs.util.MimeMap;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This servlet may be used to "browse" the entire hierarchy of resources
 * on an SMB network like one might with Network Neighborhood or Windows
 * Explorer. The users credentials with be negotiated using NTLM SSP if
 * the client is Microsoft Internet Explorer.
 */

public class NetworkExplorer extends HttpServlet {

    private static LogStream log = LogStream.getInstance();

    private MimeMap mimeMap;
    private String style;
    private NtlmSsp ntlmSsp;
    private boolean credentialsSupplied;
    private boolean enableBasic;
    private boolean insecureBasic;
    private String realm, defaultDomain;

    public void init() throws ServletException {
        InputStream is;
        StringBuffer sb = new StringBuffer();
        byte[] buf = new byte[1024];
        int n, level;
        String name;

        Config.setProperty( "jcifs.smb.client.soTimeout", "600000" );
        Config.setProperty( "jcifs.smb.client.attrExpirationPeriod", "300000" );

        Enumeration e = getInitParameterNames();
        while( e.hasMoreElements() ) {
            name = (String)e.nextElement();
            if( name.startsWith( "jcifs." )) {
                Config.setProperty( name, getInitParameter( name ));
            }
        }

        if( Config.getProperty( "jcifs.smb.client.username" ) == null ) {
            ntlmSsp = new NtlmSsp();
        } else {
            credentialsSupplied = true;
        }

        try {
            mimeMap = new MimeMap();
            is = getClass().getClassLoader().getResourceAsStream( "jcifs/http/ne.css" );
            while(( n = is.read( buf )) != -1 ) {
                sb.append( new String( buf, 0, n, "ISO8859_1" ));
            }
            style = sb.toString();
        } catch( IOException ioe ) {
            throw new ServletException( ioe.getMessage() );
        }

        enableBasic = Config.getBoolean("jcifs.http.enableBasic", false );
        insecureBasic = Config.getBoolean("jcifs.http.insecureBasic", false );
        realm = Config.getProperty("jcifs.http.basicRealm");
        if (realm == null) realm = "jCIFS";
        defaultDomain = Config.getProperty("jcifs.smb.client.domain");

        if(( level = Config.getInt( "jcifs.util.loglevel", -1 )) != -1 ) {
            LogStream.setLevel( level );
        }
        if( log.level > 2 ) {
            try {
                Config.store( log, "JCIFS PROPERTIES" );
            } catch( IOException ioe ) {
            }
        }
    }

    protected void doFile( HttpServletRequest req,
                HttpServletResponse resp, SmbFile file ) throws IOException {
        byte[] buf = new byte[8192];
        SmbFileInputStream in;
        ServletOutputStream out;
        String url, type;
        int n;

        in = new SmbFileInputStream( file );
        out = resp.getOutputStream();
        url = file.getPath();

        resp.setContentType( "text/plain" );

        if(( n = url.lastIndexOf( '.' )) > 0 &&
                ( type = url.substring( n + 1 )) != null &&
                type.length() > 1 && type.length() < 6 ) {
            resp.setContentType( mimeMap.getMimeType( type ));
        }
        resp.setHeader( "Content-Length", file.length() + "" );
        resp.setHeader( "Accept-Ranges", "Bytes" );

        while(( n = in.read( buf )) != -1 ) {
            out.write( buf, 0, n );
        }
    }
    protected int compareNames( SmbFile f1, String f1name, SmbFile f2 ) throws IOException {
        if( f1.isDirectory() != f2.isDirectory() ) {
            return f1.isDirectory() ? -1 : 1;
        }
        return f1name.compareToIgnoreCase( f2.getName() );
    }
    protected int compareSizes( SmbFile f1, String f1name, SmbFile f2 ) throws IOException {
        long diff;

        if( f1.isDirectory() != f2.isDirectory() ) {
            return f1.isDirectory() ? -1 : 1;
        }
        if( f1.isDirectory() ) {
            return f1name.compareToIgnoreCase( f2.getName() );
        }
        diff = f1.length() - f2.length();
        if( diff == 0 ) {
            return f1name.compareToIgnoreCase( f2.getName() );
        }
        return diff > 0 ? -1 : 1;
    }
    protected int compareTypes( SmbFile f1, String f1name, SmbFile f2 ) throws IOException {
        String f2name, t1, t2;
        int i;

        if( f1.isDirectory() != f2.isDirectory() ) {
            return f1.isDirectory() ? -1 : 1;
        }
        f2name = f2.getName();
        if( f1.isDirectory() ) {
            return f1name.compareToIgnoreCase( f2name );
        }
        i = f1name.lastIndexOf( '.' );
        t1 = i == -1 ? "" : f1name.substring( i + 1 );
        i = f2name.lastIndexOf( '.' );
        t2 = i == -1 ? "" : f2name.substring( i + 1 );

        i = t1.compareToIgnoreCase( t2 );
        if( i == 0 ) {
            return f1name.compareToIgnoreCase( f2name );
        }
        return i;
    }
    protected int compareDates( SmbFile f1, String f1name, SmbFile f2 ) throws IOException {
        if( f1.isDirectory() != f2.isDirectory() ) {
            return f1.isDirectory() ? -1 : 1;
        }
        if( f1.isDirectory() ) {
            return f1name.compareToIgnoreCase( f2.getName() );
        }
        return f1.lastModified() > f2.lastModified() ? -1 : 1;
    }
    protected void doDirectory( HttpServletRequest req, HttpServletResponse resp, SmbFile dir ) throws IOException {
        PrintWriter out;
        SmbFile[] dirents;
        SmbFile f;
        int i, j, len, maxLen, dirCount, fileCount, sort;
        String str, name, path, fmt;
        LinkedList sorted;
        ListIterator iter;
        SimpleDateFormat sdf = new SimpleDateFormat( "MM/d/yy h:mm a" );
        GregorianCalendar cal = new GregorianCalendar();

        sdf.setCalendar( cal );

        dirents = dir.listFiles();
        if( log.level > 2 )
            log.println( dirents.length + " items listed" );
        sorted = new LinkedList();
        if(( fmt = req.getParameter( "fmt" )) == null ) {
            fmt = "col";
        }
        sort = 0;
        if(( str = req.getParameter( "sort" )) == null || str.equals( "name" )) {
            sort = 0;
        } else if( str.equals( "size" )) {
            sort = 1;
        } else if( str.equals( "type" )) {
            sort = 2;
        } else if( str.equals( "date" )) {
            sort = 3;
        }
        dirCount = fileCount = 0;
        maxLen = 28;
        for( i = 0; i < dirents.length; i++ ) {
            try {
                if( dirents[i].getType() == SmbFile.TYPE_NAMED_PIPE ) {
                    continue;
                }
            } catch( SmbAuthException sae ) {
                if( log.level > 2 )
                    sae.printStackTrace( log );
            } catch( SmbException se ) {
                if( log.level > 2 )
                    se.printStackTrace( log );
                if( se.getNtStatus() != se.NT_STATUS_UNSUCCESSFUL ) {
                    throw se;
                }
            }
            if( dirents[i].isDirectory() ) {
                dirCount++;
            } else {
                fileCount++;
            }

            name = dirents[i].getName();
            if( log.level > 3 )
                log.println( i + ": " + name );
            len = name.length(); 
            if( len > maxLen ) {
                maxLen = len;
            }

            iter = sorted.listIterator();
            for( j = 0; iter.hasNext(); j++ ) {
                if( sort == 0 ) {
                    if( compareNames( dirents[i], name, (SmbFile)iter.next() ) < 0 ) {
                        break;
                    }
                } else if( sort == 1 ) {
                    if( compareSizes( dirents[i], name, (SmbFile)iter.next() ) < 0 ) {
                        break;
                    }
                } else if( sort == 2 ) {
                    if( compareTypes( dirents[i], name, (SmbFile)iter.next() ) < 0 ) {
                        break;
                    }
                } else if( sort == 3 ) {
                    if( compareDates( dirents[i], name, (SmbFile)iter.next() ) < 0 ) {
                        break;
                    }
                }
            }
            sorted.add( j, dirents[i] );
        }
        if( maxLen > 50 ) {
            maxLen = 50;
        }
        maxLen *= 9; /* convert to px */

        out = resp.getWriter();

        resp.setContentType( "text/html" );

        out.println( "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">" );
        out.println( "<html><head><title>Network Explorer</title>" );
        out.println( "<meta HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">" );
        out.println( "<style TYPE=\"text/css\">" );

        out.println( style );

        if( dirents.length < 200 ) {
            out.println( "    a:hover {" );
            out.println( "        background: #a2ff01;" );
            out.println( "    }" );
        }

        out.println( "</STYLE>" );
        out.println( "</head><body>" );

        out.print( "<a class=\"sort\" style=\"width: " + maxLen + ";\" href=\"?fmt=detail&sort=name\">Name</a>" );
        out.println( "<a class=\"sort\" href=\"?fmt=detail&sort=size\">Size</a>" );
        out.println( "<a class=\"sort\" href=\"?fmt=detail&sort=type\">Type</a>" );
        out.println( "<a class=\"sort\" style=\"width: 180\" href=\"?fmt=detail&sort=date\">Modified</a><br clear='all'><p>" );

        path = dir.getCanonicalPath();

        if( path.length() < 7 ) {
            out.println( "<b><big>smb://</big></b><br>" );
            path = ".";
        } else {
            out.println( "<b><big>" + path + "</big></b><br>" );
            path = "../";
        }
        out.println( (dirCount + fileCount) + " objects (" + dirCount + " directories, " + fileCount + " files)<br>" );
        out.println( "<b><a class=\"plain\" href=\".\">normal</a> | <a class=\"plain\" href=\"?fmt=detail\">detailed</a></b>" );
        out.println( "<p><table border='0' cellspacing='0' cellpadding='0'><tr><td>" );

        out.print( "<A style=\"width: " + maxLen );
        out.print( "; height: 18;\" HREF=\"" );
        out.print( path );
        out.println( "\"><b>&uarr;</b></a>" );
        if( fmt.equals( "detail" )) {
            out.println( "<br clear='all'>" );
        }

        if( path.length() == 1 || dir.getType() != SmbFile.TYPE_WORKGROUP ) {
            path = "";
        }

        iter = sorted.listIterator();
        while( iter.hasNext() ) {
            f = (SmbFile)iter.next();
            name = f.getName();

            if( fmt.equals( "detail" )) {
                out.print( "<A style=\"width: " + maxLen );
                out.print( "; height: 18;\" HREF=\"" );
                out.print( path );
                out.print( name );

                if( f.isDirectory() ) {
                    out.print( "?fmt=detail\"><b>" );
                    out.print( name );
                    out.print( "</b></a>" );
                } else {
                    out.print( "\"><b>" );
                    out.print( name );
                    out.print( "</b></a><div align='right'>" );
                    out.print( (f.length() / 1024) + " KB </div><div>" );
                    i = name.lastIndexOf( '.' ) + 1;
                    if( i > 1 && (name.length() - i) < 6 ) {
                        out.print( name.substring( i ).toUpperCase() + "</div class='ext'>" );
                    } else {
                        out.print( "&nbsp;</div>" );
                    }
                    out.print( "<div style='width: 180'>" );
                    out.print( sdf.format( new Date( f.lastModified() )));
                    out.print( "</div>" );
                }
                out.println( "<br clear='all'>" );
            } else {
                out.print( "<A style=\"width: " + maxLen );
                if( f.isDirectory() ) {
                    out.print( "; height: 18;\" HREF=\"" );
                    out.print( path );
                    out.print( name );
                    out.print( "\"><b>" );
                    out.print( name );
                    out.print( "</b></a>" );
                } else {
                    out.print( ";\" HREF=\"" );
                    out.print( path );
                    out.print( name );
                    out.print( "\"><b>" );
                    out.print( name );
                    out.print( "</b><br><small>" );
                    out.print( (f.length() / 1024) + "KB <br>" );
                    out.print( sdf.format( new Date( f.lastModified() )));
                    out.print( "</small>" );
                    out.println( "</a>" );
                }
            }
        }

        out.println( "</td></tr></table>" );
        out.println( "</BODY></HTML>" );
        out.close();
    }
    private String parseServerAndShare( String pathInfo ) {
        char[] out = new char[256];
        char ch;
        int len, p, i;

        if( pathInfo == null ) {
            return null;
        }
        len = pathInfo.length();

        p = i = 0;
        while( p < len && pathInfo.charAt( p ) == '/' ) {
            p++;
        }
        if( p == len ) {
            return null;
        }

                                               /* collect server name */
        while ( p < len && ( ch = pathInfo.charAt( p )) != '/' ) {
            out[i++] = ch;
            p++;
        }
        while( p < len && pathInfo.charAt( p ) == '/' ) {
            p++;
        }
        if( p < len ) {                 /* then there must be a share */
            out[i++] = '/';
            do {                            /* collect the share name */
                out[i++] = (ch = pathInfo.charAt( p++ ));
            } while( p < len && ch != '/' );
        }
        return new String( out, 0, i );
    }
    public void doGet( HttpServletRequest req,
                HttpServletResponse resp ) throws IOException, ServletException {
        UniAddress dc;
        String msg, pathInfo, server = null;
        boolean offerBasic, possibleWorkgroup = true;
        NtlmPasswordAuthentication ntlm = null;
        HttpSession ssn = req.getSession( false );

        if(( pathInfo = req.getPathInfo() ) != null ) {
            int i;
            server = parseServerAndShare( pathInfo );
            if( server != null && ( i = server.indexOf( '/' )) > 0 ) {
                server = server.substring( 0, i ).toLowerCase();
                possibleWorkgroup = false;
            } 
        }

        msg = req.getHeader( "Authorization" );
        offerBasic = enableBasic && (insecureBasic || req.isSecure());

        if( msg != null && (msg.startsWith( "NTLM " ) ||
                    (offerBasic && msg.startsWith("Basic ")))) {

            if( msg.startsWith("NTLM ")) {
                byte[] challenge;

                if( pathInfo == null || server == null ) {
                    String mb = NbtAddress.getByName( NbtAddress.MASTER_BROWSER_NAME, 0x01, null ).getHostAddress();
                    dc = UniAddress.getByName( mb );
                } else {
                    dc = UniAddress.getByName( server, possibleWorkgroup );
                }

                req.getSession(); /* ensure session id is set for cluster env. */
                challenge = SmbSession.getChallenge( dc );
                if(( ntlm = NtlmSsp.authenticate( req, resp, challenge )) == null ) {
                    return;
                }
            } else { /* Basic */
                String auth = new String( Base64.decode( msg.substring(6) ), "US-ASCII" );
                int index = auth.indexOf( ':' );
                String user = (index != -1) ? auth.substring(0, index) : auth;
                String password = (index != -1) ? auth.substring(index + 1) : "";
                index = user.indexOf('\\');
                if (index == -1) index = user.indexOf('/');
                String domain = (index != -1) ? user.substring(0, index) : defaultDomain;
                user = (index != -1) ? user.substring(index + 1) : user;
                ntlm = new NtlmPasswordAuthentication(domain, user, password);
            }

            req.getSession().setAttribute( "npa-" + server, ntlm );

        } else if( !credentialsSupplied ) {
            if( ssn != null ) {
                ntlm = (NtlmPasswordAuthentication)ssn.getAttribute( "npa-" + server );
            }
            if( ntlm == null ) {
                resp.setHeader( "WWW-Authenticate", "NTLM" );
                if (offerBasic) {
                    resp.addHeader( "WWW-Authenticate", "Basic realm=\"" + realm + "\"");
                }
                resp.setHeader( "Connection", "close" );
                resp.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
                resp.flushBuffer();
                return;
            }
        }

        try {
            SmbFile file;

            if( ntlm != null ) {
                file = new SmbFile( "smb:/" + pathInfo , ntlm );
            } else if( server == null ) {
                file = new SmbFile( "smb://" );
            } else {
                file = new SmbFile( "smb:/" + pathInfo );
            }

            if( file.isDirectory() ) {
                doDirectory( req, resp, file );
            } else {
                doFile( req, resp, file );
            }
        } catch( SmbAuthException sae ) {
            if( ssn != null ) {
                ssn.removeAttribute( "npa-" + server );
            }
            if( sae.getNtStatus() == sae.NT_STATUS_ACCESS_VIOLATION ) {
                /* Server challenge no longer valid for
                 * externally supplied password hashes.
                 */
                resp.sendRedirect( req.getRequestURL().toString() );
                return;
            }
            resp.setHeader( "WWW-Authenticate", "NTLM" );
            if (offerBasic) {
                resp.addHeader( "WWW-Authenticate", "Basic realm=\"" + realm + "\"");
            }
            resp.setHeader( "Connection", "close" );
            resp.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
            resp.flushBuffer();
            return;
        } catch( DfsReferral dr ) {
            StringBuffer redir = req.getRequestURL();
            String qs = req.getQueryString();
            redir = new StringBuffer( redir.substring( 0, redir.length() - req.getPathInfo().length() ));
            redir.append( '/' );
            redir.append(dr.server);
            redir.append( '/' );
            redir.append(dr.share);
            redir.append( '/' );
            if( qs != null ) {
                redir.append( req.getQueryString() );
            }
            resp.sendRedirect( redir.toString() );
            resp.flushBuffer();
            return;
        }
    }
}

