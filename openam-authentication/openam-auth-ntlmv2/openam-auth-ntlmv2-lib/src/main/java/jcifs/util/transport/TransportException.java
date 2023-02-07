package jcifs.util.transport;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class TransportException extends IOException {

    private Throwable rootCause;

    public TransportException() {
    }
    public TransportException( String msg ) {
        super( msg );
    }
    public TransportException( Throwable rootCause ) {
        this.rootCause = rootCause;
    }
    public TransportException( String msg, Throwable rootCause ) {
        super( msg );
        this.rootCause = rootCause;
    }

    public Throwable getRootCause() {
        return rootCause;
    }
    public String toString() {
        if( rootCause != null ) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter( sw );
            rootCause.printStackTrace( pw );
            return super.toString() + "\n" + sw;
        } else {
            return super.toString();
        }
    }
}

