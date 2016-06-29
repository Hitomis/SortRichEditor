package com.hitomi.sortricheditor.view.editor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;

/**
 * Created by hitomi on 2016/6/29.
 */
public class SEImageHelper {

    public static SEImageHelper getInstance() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        public final static SEImageHelper instance = new SEImageHelper();
    }

    /**
     * @param path    原图片路径
     * @param maxSize 图片最大尺寸
     * @return 压缩后的图片
     */
    public Bitmap compressBitmap(String path, PointF maxSize) {
        Bitmap compressBitmap = null;
        // 限定-尺寸
        float wMax = maxSize.x;
        float hMax = maxSize.y;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opt);
        if (!opt.mCancel && opt.outWidth != -1 && opt.outHeight != -1) {
            float wIn = opt.outWidth < opt.outHeight ? opt.outWidth : opt.outHeight;
            float hIn = opt.outHeight > opt.outWidth ? opt.outHeight : opt.outWidth;

            float scaleIn = wIn / hIn;
            float scaleMax = wMax / hMax;

            // 若较宽 则限制宽
            opt.inJustDecodeBounds = false;
            // "1/opt.inSampleSize"表缩放比率
            if (scaleIn > scaleMax) {
                opt.inSampleSize = (int) Math.ceil(wIn / wMax);
            } else {
                opt.inSampleSize = (int) Math.ceil(hIn / hMax);
            }

            compressBitmap = BitmapFactory.decodeFile(path, opt);
        }
        return compressBitmap;
    }

}
