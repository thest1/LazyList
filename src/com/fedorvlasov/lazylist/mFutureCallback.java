package com.fedorvlasov.lazylist;

import java.util.concurrent.Callable;

/**
 * Created by Administrator on 2015-03-24.
 */
public class mFutureCallback implements Callable<String> {
    String name;
    public mFutureCallback(String name){
        this.name = name;
    }
    @Override
    public String call() throws Exception {
        return name;
    }
}

