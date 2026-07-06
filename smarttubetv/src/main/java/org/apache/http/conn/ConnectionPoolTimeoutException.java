package org.apache.http.conn;

import java.io.InterruptedIOException;

public class ConnectionPoolTimeoutException extends InterruptedIOException {
    private static final long serialVersionUID = 1L;

    public ConnectionPoolTimeoutException() {
        super();
    }

    public ConnectionPoolTimeoutException(String message) {
        super(message);
    }
}