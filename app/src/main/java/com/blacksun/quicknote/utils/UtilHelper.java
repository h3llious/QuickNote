package com.blacksun.quicknote.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import static com.blacksun.quicknote.activities.MainActivity.DIRECTORY;
import static com.blacksun.quicknote.activities.MainActivity.PACKAGE_NAME;
import static com.blacksun.quicknote.utils.DatabaseHelper.DATABASE_NAME;

public class UtilHelper {
    public final static String DATABASE_PATH = Environment.getDataDirectory() + "/data/" + PACKAGE_NAME + "/databases/" + DATABASE_NAME;
    public final static File FILE_DATABASE = new File(DATABASE_PATH);
    public final static String MIME_TYPE_DB = "application/x-sqlite-3";
    public final static String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
    public final static String FOLDER_NAME = "files";

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap circleBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setShader(shader);
        paint.setAntiAlias(true);
        Canvas c = new Canvas(circleBitmap);
        c.drawCircle((float) (bitmap.getWidth()) / 2, (float) bitmap.getHeight() / 2, (float) bitmap.getWidth() / 2, paint);


        return circleBitmap;
    }

    //    public static void copy(File src, File dst) {
    public static void copy(Uri uri, File dst, Context context) {
//        try (InputStream in = new FileInputStream(src);
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dst)) {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            //You can replace it with your name
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }

    public static void removeTempFiles() {
        File[] files = DIRECTORY.listFiles();
        Log.d("Files", "Temp file: ");
        for (File child : files) {
            String name = child.getName();
            if (name.length() >= 7) {
                String ext = name.substring(name.length() - 7);
                if (ext.equals("(.temp)")) {
//                                    allFilesPath.add(name);
                    child.delete();
                }
            }
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromFile(String filePath,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static Bitmap createThumbnail(String path, int width, int height) {
        Bitmap imageFile = decodeSampledBitmapFromFile(path, width, height);
        return ThumbnailUtils.extractThumbnail(imageFile, width, height);
    }
}
