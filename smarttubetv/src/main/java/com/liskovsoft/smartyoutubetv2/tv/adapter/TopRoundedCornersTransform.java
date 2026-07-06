package com.liskovsoft.smartyoutubetv2.tv.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.Charset;
import java.security.MessageDigest;

public  class TopRoundedCornersTransform extends BitmapTransformation {
    private final float radius;

    public TopRoundedCornersTransform(float radius) {
        this.radius = radius;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        int w = toTransform.getWidth();
        int h = toTransform.getHeight();
        if (w == 0 || h == 0) return toTransform;

        Bitmap result = pool.get(w, h, Bitmap.Config.ARGB_8888);
        if (result == null) {
            result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(toTransform, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        Path path = new Path();
        float r = Math.min(radius, Math.min(w, h) / 2f); // 防止半径过大
        path.moveTo(0, r);
        path.quadTo(0, 0, r, 0);
        path.lineTo(w - r, 0);
        path.quadTo(w, 0, w, r);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();

        canvas.drawPath(path, paint);
        return result;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(("topRounded" + radius).getBytes(Charset.forName("UTF-8")));
    }
}