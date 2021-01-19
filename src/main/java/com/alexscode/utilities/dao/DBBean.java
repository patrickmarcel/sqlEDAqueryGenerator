package com.alexscode.utilities.dao;

public interface DBBean {
    String insertToString();
    default DBBean asBean(){
        return (DBBean) this;
    }
}
