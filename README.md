# LazyList

A simple library to display images in Android ListView. Images are being downloaded asynchronously in the background. Images are being cached on SD card and in memory. Can also be used for GridView and just to display images into an ImageView.

<img src="http://img718.imageshack.us/img718/9149/screen1sx.png" />

Originally published <a href="http://stackoverflow.com/questions/541966/android-how-do-i-do-a-lazy-load-of-images-in-listview/3068012#3068012">here</a>.

## Basic Usage

    ImageLoader imageLoader=new ImageLoader(context);
    ...
    imageLoader.DisplayImage(url, imageView);

## License

LazyList is released under the MIT license.