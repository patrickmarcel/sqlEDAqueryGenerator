package com.alexscode.utilities.http;

public interface Handler {
    /**
     * A method to handle an incoming request, should not close the output stream
     * @param req the request to handle
     */
    HttpAns handle(HttpReq req);
}
