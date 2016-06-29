package com.hitomi.sortricheditor.view.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * 可以存放Bitmap和Path的ImageView
 */
public class SEDataImageView extends ImageView {

	private String absolutePath;

	private Bitmap bitmap;

	public SEDataImageView(Context context) {
		this(context, null);
	}

	public SEDataImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SEDataImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = absolutePath;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

}
