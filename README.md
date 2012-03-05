# LazyList

A simple library to display images in Android ListView. Images are being downloaded asynchronously in the background. Images are being cached on SD card and in memory. Can also be used for GridView and just to display images into an ImageView.

<img src="http://img718.imageshack.us/img718/9149/screen1sx.png" />

Originally published <a href="http://stackoverflow.com/questions/541966/android-how-do-i-do-a-lazy-load-of-images-in-listview/3068012#3068012">here</a>.

## Basic Usage
``` java
ImageLoader imageLoader=new ImageLoader(context);
...
imageLoader.DisplayImage(url, imageView);
```
Don't forget to add the following permissions to your AndroidManifest.xml:

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

    
Please create only one instance of ImageLoader and reuse it all around your application. This way image caching will be much more efficient.

## License

LazyList is released under the <a href="https://github.com/thest1/LazyList/blob/master/LICENSE">MIT license</a>.