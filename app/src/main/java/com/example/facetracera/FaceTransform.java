package com.example.facetracera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

public class FaceTransform {

    static Bitmap processImage(Bitmap originalBitmap, Rect faceBound, int width, int height, float rotation){
        Bitmap boundBitmap = Bitmap.createBitmap(originalBitmap,
                faceBound.centerX() - (faceBound.width() / 2),
                faceBound.centerY() - (faceBound.height() / 2),
                faceBound.width(), faceBound.height());

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(boundBitmap, faceBound.width(), faceBound.height(), false); // 70x70
        Bitmap croppedBitmap = FaceTransform.getCroppedBitmap(scaledBitmap);
        Bitmap rotatedBitmap = FaceTransform.rotateBitmap(croppedBitmap, rotation);
        return Bitmap.createScaledBitmap(rotatedBitmap, width, height, false);
    }

    private static Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff000000;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(false);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, float rotationAngleDegree){

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int newW = w, newH = h;
        if (rotationAngleDegree == 90 || rotationAngleDegree == 270){
            newW = h;
            newH = w;
        }
        Bitmap rotatedBitmap = Bitmap.createBitmap(newW, newH, bitmap.getConfig());
        Canvas canvas = new Canvas(rotatedBitmap);

        Rect rect = new Rect(0,0, newW, newH);
        Matrix matrix = new Matrix();
        float px = rect.exactCenterX();
        float py = rect.exactCenterY();
        matrix.postTranslate(-bitmap.getWidth() / 2, -bitmap.getHeight() / 2);
        matrix.postRotate(rotationAngleDegree);
        matrix.postTranslate(px, py);
        canvas.drawBitmap(bitmap, matrix, new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG ));

        return rotatedBitmap;
    }
}
