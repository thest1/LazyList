package com.fedorvlasov.lazylist;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.os.Environment;

public class Utils {
	
	private static final String CHARSET = "utf-8";
	private static final String MD5 = "MD5";
	
	private static File externalDirectory;
	
    public static void copyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1)
                  break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }
    
    public static String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance(MD5);
            digest.update(s.getBytes(CHARSET));
            byte messageDigest[] = digest.digest();
            
            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
        	throw new RuntimeException(e);
		}
    }
    
	public static File getExternalFilesDir(Context context) {
		if(externalDirectory == null) {
			try {
				//for api 8 and above
				externalDirectory = context.getExternalFilesDir(null); 
			} catch(Throwable e) {
				//api less than 8
				String packageName = context.getPackageName();
				externalDirectory = new File(Environment.getExternalStorageDirectory(), "/Android/data/" + packageName + "/files/");
			}
		}

		return externalDirectory;
	}
}