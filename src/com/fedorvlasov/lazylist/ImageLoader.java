package com.fedorvlasov.lazylist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

public class ImageLoader {
    
	private static final String TAG = "ImageLoader";
	
	/**
	 * Default time, after which is checked if image was changed on the server.
	 */
	private static final int TIME_TO_CHECK_CHANGES = 5 * 60 * 1000;
	
	/**
	 * Default time, after which unused files will be deleted.
	 */
	private static final int TIME_TO_CLEAN_UNUSED_FILES = 5 * 60 * 60 * 1000;
	
	private static final String LAST_CLEAN_UP_TIME_KEY = "image-loader-last-clean-up";
	
    private MemoryCache memoryCache=new MemoryCache();
    private FileCache fileCache;
    private Map<ImageView, String> imageViews=Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    private ExecutorService executorService; 
    private int stubDrawableId;
    private int timeInMilisToCheckChanges = TIME_TO_CHECK_CHANGES;
    private int timeInMilisToCleanUnusedFiles = TIME_TO_CLEAN_UNUSED_FILES;
    private int imageWidth;
    private int imageHeight;
    private int originalImageDensity = DisplayMetrics.DENSITY_HIGH;
    
    private Handler handler;
    
    /**
     * Creates new image loader. Pleas set imageWidth and imageHeight parameters to use memory more efficiently.
     * @param context context
     * @param stubDrawableId id of default placeholder image
     * @param allowInternal enables internal memory caching
     * @param allowExternal enables external memory (usually SD card) caching
     * @param imageWidth required width of image, you can set it to -1 to disable down-sampling
     * @param imageHeight required height of image, you can set it to -1 to disable down-sampling
     */
    public ImageLoader(Context context, 
    		int stubDrawableId, 
    		boolean allowInternal, 
    		boolean allowExternal,
    		int imageWidth,
    		int imageHeight) {
        fileCache = new FileCache(context, allowInternal, allowExternal);
        executorService = Executors.newFixedThreadPool(5, new ThreadFactory() {
        	private int threadCount = 0;
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "ImageLoader-" + threadCount ++);
				t.setDaemon(true);
				t.setPriority(Thread.NORM_PRIORITY - 1);
				return t;
			}
		});
        this.stubDrawableId = stubDrawableId;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }
    
    public void setMemoryCacheEnabled(boolean enabled) {
    	if(enabled && memoryCache == null) {
    		memoryCache = new MemoryCache();
    	}
    	
    	if(!enabled) {
    		memoryCache = null;
    	}
    }
    
    /**
     * Sets the period in which unused files will be cleared. 
     * Unused file is file, which was not loaded from cache for longer then period given as parameter
     * @param timeToClean time in milliseconds
     */
    public void setTimeToCleanUnusedFiles(int timeToClean) {
    	timeInMilisToCleanUnusedFiles = timeToClean;
    }
    
    /**
     * Time to check changes on server. After this time we check if the image change on the server.
     * @param timeToCheck time in millis.
     */
    public void setTimeToCheckChanges(int timeToCheck) {
    	timeInMilisToCheckChanges = timeToCheck;
    }

    /**
     * Here you can set for what the density your images are on server. Default is high density.
     * Set it to 0 to don't scale images for particular density.
     * @param density density of images on server, or zero to don't care about densities
     */
    public void setOriginalImageDensity(int density) {
    	originalImageDensity = density;
    }
    
    public void displayImage(String url, ImageView imageView, Context context)
    {
    	if(handler == null) {
    		handler = new Handler();
    	}
    	
        imageViews.put(imageView, url);
        
        Bitmap bitmap = getBitmapFromMemoryCache(url);
        
        if(bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
        	imageView.setTag(url);
            queuePhoto(url, imageView, context);
            imageView.setImageResource(stubDrawableId);
        }
    }

	private Bitmap getBitmapFromMemoryCache(String url) {
		Bitmap bitmap;
        if(memoryCache != null) {
        	bitmap = memoryCache.get(url);
        } else {
        	bitmap = null;
        }
		return bitmap;
	}
        
    private void queuePhoto(String url, ImageView imageView, Context context)
    {
        PhotoToLoad p=new PhotoToLoad(url, imageView, context);
        executorService.submit(new PhotosLoader(p));
    }
    
    private Bitmap getBitmapFromFileCache(String url, int targetDensity) 
    {
        File f = fileCache.getFile(url);
        
        if(f == null) {
        	return null;
        }
        
        //from SD cache
        Bitmap b = decodeFile(f, targetDensity);
        if(b != null) {        	
            return b;
        } else {
        	f.delete();//maybe file is corrupted
        	return null;
        }
    }

	private Bitmap getBitmapFromWeb(String url, int targetDensity) {
		File f = fileCache.getFile(url);
		File tempFile = fileCache.getTempFile(url);
		
		//from web
        try {
            Bitmap bitmap=null;
            URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)imageUrl.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            
            //not modified?
            if(f != null) {
	            conn.setIfModifiedSince(f.lastModified());
	            if(conn.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
	            	conn.disconnect();
	            	f.setLastModified(System.currentTimeMillis());//touch the file
	            	return null;
	            }
            }
            
            downloadImage(tempFile, conn, url);
            
            bitmap = decodeFile(tempFile, targetDensity);
            
            if(f != null) {
            	//if file cache is enabled, store the file there
            	f.delete();
            	tempFile.renameTo(f);
            }
            
            return bitmap;
        } catch (Exception ex){
        	Log.w(TAG, "Error when downloading image.", ex);
        	return null;
        } finally {
        	tempFile.delete();
        }
	}

	/**
	 * Downloads image to file f.
	 * @param f file to be downloaded image ins
	 * @param conn connection to download from
	 * @param url file url
	 * @throws IOException
	 */
	private void downloadImage(File f, HttpURLConnection conn, String url) throws IOException {
		OutputStream os = null;
		InputStream is = null;
		
		try {
	        is = conn.getInputStream();
	        os = new FileOutputStream(f);//use tempfile to avoid half-downloaded images

			Utils.copyStream(is, os);
			os.close();
			
			f.setLastModified(System.currentTimeMillis());
		} finally {
        	if(is != null) {
        		try {
        			is.close();
        		} catch (Exception e) {
					// never mind
				}
        	}
        	if(os != null) {
        		try {
        			os.close();
        		} catch (Exception e) {
        			// never mind
        		}
        	}
		}
	}

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f, int targetDensity){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);
            
            //Find the correct scale value. It should be the power of 2.
            int requiredImageWidth = imageWidth > 0 ? imageWidth : Integer.MAX_VALUE;
            int requiredImageHeight = imageHeight > 0 ? imageHeight : Integer.MAX_VALUE;
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;
            while(true){
                if(width_tmp/2 < requiredImageWidth || height_tmp/2 < requiredImageHeight)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }
            scale = 1;
            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inScaled = true; //scale to target density
            o2.inSampleSize = scale;
            o2.inDensity = originalImageDensity;
            o2.inTargetDensity = targetDensity;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }
    
    //Task for the queue
    private class PhotoToLoad
    {
        public String url;
        public ImageView imageView;
        public Context context;
        public PhotoToLoad(String u, ImageView i, Context activity) {
            url=u; 
            imageView=i;
        }
    }
    
    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;
        PhotosLoader(PhotoToLoad photoToLoad){
            this.photoToLoad=photoToLoad;
        }
        
        @Override
        public void run() {
        	int targetDensity = photoToLoad.imageView.getResources().getDisplayMetrics().densityDpi;

            if(imageViewReused(photoToLoad))
                return;
            Bitmap bmp=getBitmapFromFileCache(photoToLoad.url, targetDensity);
            if(bmp == null) {
            	bmp = getBitmapFromWeb(photoToLoad.url, targetDensity);
            }
            if(memoryCache != null) {
            	memoryCache.put(photoToLoad.url, bmp);
            }
            if(imageViewReused(photoToLoad))
                return;
            displayBitmap(bmp);
            
            //refresh cache after showing image
            askServerFoChanges(targetDensity);
            
            //maybe cleanup?
            cacheCleanup();
        }

        /**
         * Time to time cleans the cache up.
         */
		private void cacheCleanup() {
			SharedPreferences prefs = photoToLoad.context.getSharedPreferences("image-loader-dkd43l", Context.MODE_PRIVATE);
            long lastCleanupTime = prefs.getLong(LAST_CLEAN_UP_TIME_KEY, 0);
            if(lastCleanupTime == 0) {
            	prefs.edit().putLong(LAST_CLEAN_UP_TIME_KEY, System.currentTimeMillis()).commit();
            } else if(lastCleanupTime < System.currentTimeMillis() - timeInMilisToCleanUnusedFiles) {
            	fileCache.clear(System.currentTimeMillis() - timeInMilisToCleanUnusedFiles);
            	prefs.edit().putLong(LAST_CLEAN_UP_TIME_KEY, System.currentTimeMillis()).commit();
            }
		}

		/**
		 * After {@link #timeInMilisToCheckChanges} ask server for changes in this image.
		 */
		private void askServerFoChanges(int targetDensity) {
			Bitmap bmp;
			File f = fileCache.getFile(photoToLoad.url);
            if(f != null) {
	            long fLastModified = f.lastModified();
	            //time to ask server for changes?
	            if(fLastModified < System.currentTimeMillis() - timeInMilisToCheckChanges) {
	            	bmp = getBitmapFromWeb(photoToLoad.url, targetDensity);
	            	if(bmp != null && memoryCache != null) {
	            		memoryCache.put(photoToLoad.url, bmp);
	            	}
	            }
            }
		}

		private void displayBitmap(Bitmap bmp) {
			BitmapDisplayer bd=new BitmapDisplayer(bmp, photoToLoad);
            handler.post(bd);
		}
    }
    
    boolean imageViewReused(PhotoToLoad photoToLoad){
    	Object tag = photoToLoad.imageView.getTag();
    	if(!photoToLoad.url.equals(tag)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap;
        PhotoToLoad photoToLoad;
        
        public BitmapDisplayer(Bitmap b, PhotoToLoad p){
        	bitmap=b;
        	photoToLoad=p;
        }
        
        public void run()
        {
            if(imageViewReused(photoToLoad))
                return;
            if(bitmap!=null)
                photoToLoad.imageView.setImageBitmap(bitmap);
            else
                photoToLoad.imageView.setImageResource(stubDrawableId);
        }
    }

    public void clearCache() {
    	if(memoryCache != null) {
    		memoryCache.clear();
    	}
        fileCache.clear();
    }

}
