package com.fedorvlasov.lazylist;

import java.util.HashMap;
import android.graphics.Bitmap;

public class MemoryCache {
    private HashMap<String, Bitmap> cache=new HashMap<String, Bitmap>();
    
    public Bitmap get(String id){
        if(!cache.containsKey(id))
            return null;
        return cache.get(id);
    }
    
    public void put(String id, Bitmap bitmap){
        cache.put(id, bitmap);
    }

    public void clear() {
        cache.clear();
    }

}