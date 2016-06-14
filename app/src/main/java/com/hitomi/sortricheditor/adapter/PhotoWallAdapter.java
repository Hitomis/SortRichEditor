package com.hitomi.sortricheditor.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.hitomi.sortricheditor.R;
import com.hitomi.sortricheditor.components.ImageLoader;

import java.util.List;

/**
 * Created by hitomi on 2016/6/14.
 */
public class PhotoWallAdapter extends BaseAdapterHelper <String> {

    private ImageLoader imageLoader = ImageLoader.getInstance(3, ImageLoader.Type.LIFO);

    /**
     * 记录checkbox是否被选中
     */
    private SparseBooleanArray selectionMap;

    public PhotoWallAdapter(Context context, List<String> dataList, int itemLayoutID) {
        super(context, dataList, itemLayoutID);

        selectionMap = new SparseBooleanArray();
    }

    @Override
    public void convert(ViewHolder viewHolder, String item, int position) {
        ImageView ivPhoto = viewHolder.getView(R.id.iv_photo);
        CheckBox checkBox = viewHolder.getView(R.id.checkbox);

        // 先设置为默认图片
        ivPhoto.setImageResource(R.mipmap.icon_empty_photo);
        // 再根据路径异步加载相册中的照片
        imageLoader.loadImage(item, ivPhoto);

        checkBox.setTag(R.id.tag_position, position);
        checkBox.setTag(R.id.tag_photo, ivPhoto);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int position = (Integer) buttonView.getTag(R.id.tag_position);
                ImageView photo = (ImageView) buttonView.getTag(R.id.tag_photo);

                selectionMap.put(position, isChecked);
                if (isChecked) {
                    photo.setColorFilter(Color.parseColor("#66000000"));
                } else {
                    photo.setColorFilter(null);
                }
            }
        });
        checkBox.setChecked(selectionMap.get(position));
    }
}
