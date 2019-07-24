package com.blacksun.quicknote.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

        BitmapShader shader = new BitmapShader(bitmap,  Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setShader(shader);
        paint.setAntiAlias(true);
        Canvas c = new Canvas(circleBitmap);
        c.drawCircle((float) (bitmap.getWidth())/2, (float)bitmap.getHeight()/2, (float)bitmap.getWidth()/2, paint);


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
}
