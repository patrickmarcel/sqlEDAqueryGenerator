package com.alexscode.utilities.context;

import java.util.Hashtable;

public class ContextManager {

    private static final ContextManager theManager;
    private static ContextFactory defaultFactory;

    private final Hashtable<String, Context> contexts;

    // Init global context manager
    static {
        //TODO
        theManager = new ContextManager();
    }

    private ContextManager(){
        contexts = new Hashtable<>();
    }
}
