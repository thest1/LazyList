package com.fedorvlasov.lazylist;

import java.io.File;

import com.fedorvlasov.lazylist.Utils;

import android.content.Context;
import android.os.Environment;

public class FileCache {
    
    private File cacheDir;
    private File tempDir;
    
    public FileCache(Context context, boolean allowInternal, boolean allowExternal){
        //Find the dir to save cached images
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) &&
        		allowExternal)  {
        	//external cache
            cacheDir=new File(Utils.getExternalFilesDir(context),"cache");
            tempDir = cacheDir;
        } else if (allowInternal) {
        	//internal cache
            cacheDir = context.getCacheDir();
            tempDir = cacheDir;
        }
        else {
        	//no file cache
        	cacheDir = null;
        	tempDir = context.getCacheDir();
        }	
        
        if(cacheDir != null) {
	        if(!cacheDir.exists())
	            cacheDir.mkdirs();
        }
    }
    
    public File getFile(String url){
    	if(cacheDir == null) {
    		return null;
    	}
    	
        //I identify images by md5 hash of url
        String filename = Utils.md5(url);
        //Another possible solution (thanks to grantland)
        //String filename = URLEncoder.encode(url);
        File f = new File(cacheDir, filename);
        return f;
    }
    
    public File getTempFile(String url) {
    	File dir = cacheDir;
    	if(dir == null) {
    		dir = tempDir;
    	}
    	
        //I identify images by md5 hash of url
        String filename = Utils.md5(url) + ".tmp";
        //Another possible solution (thanks to grantland)
        //String filename = URLEncoder.encode(url);
        File f = new File(dir, filename);
        return f;
    }
    
    public void clear(){
    	if(cacheDir == null) 
    		return;
    	
        File[] files=cacheDir.listFiles();
        if(files==null)
            return;
        for(File f:files)
            f.delete();
    }

	public void clear(long deleteOlderThen) {
		if(cacheDir == null) {
			return;
		}
		
		File[] files = cacheDir.listFiles();
        if(files==null)
            return;
        for(File f:files) {
        	if(f.lastModified() < deleteOlderThen) {
        		f.delete();        		
        	}
        }
	}

}
