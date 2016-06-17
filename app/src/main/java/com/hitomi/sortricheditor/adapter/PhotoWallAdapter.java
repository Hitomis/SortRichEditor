package com.hitomi.sortricheditor.adapter;

import android.content.Context;
import android.graphics.Color;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.hitomi.sortricheditor.R;
import com.hitomi.sortricheditor.components.ImageLoader;
import com.hitomi.sortricheditor.model.PhotoPack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hitomi on 2016/6/14.
 */
public class PhotoWallAdapter extends BaseAdapterHelper <String> {

    private ImageLoader imageLoader = ImageLoader.getInstance(3, ImageLoader.Type.LIFO);

    /**
     * 记录该相册中被选中checkbox的照片路径
     */
    private Map<Integer, String> selectMap;

    /**
     * 记录所有相册中所有被选中checkbox的照片路径
     */
    private Map<PhotoPack, Map<Integer, String>> selectionMap;

    public PhotoWallAdapter(Context context, List<String> dataList, int itemLayoutID, PhotoPack defaultPhotoPack) {
        super(context, dataList, itemLayoutID);

        selectMap = new LinkedHashMap<>();
        selectionMap = new HashMap<>();
        selectionMap.put(defaultPhotoPack, selectMap);
    }

    @Override
    public void convert(ViewHolder viewHolder, final String item, int position) {
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

                if (isChecked) {
                    selectMap.put(position, item);
                    photo.setColorFilter(Color.parseColor("#66000000"));
                } else {
                    selectMap.remove(position);
                    photo.setColorFilter(null);
                }
            }
        });
        checkBox.setChecked(item.equals(selectMap.get(position)));
    }

    public void cutoverSelectArray(PhotoPack selectPhotoPack) {
        selectMap = selectionMap.get(selectPhotoPack);
        if (selectMap == null) {
            selectMap = new HashMap<>();
            selectionMap.put(selectPhotoPack, selectMap);
        }
    }

    public Map<PhotoPack, Map<Integer, String>> getSelectPhotoPathMap() {
        return selectionMap;
    }

    /**
     * 获取选中照片的路径数组
     * @return
     */
    public String[] getSelectPhotoPathArray() {
        String [] photoPathArray = null;
        List<String> photoPathList = new ArrayList<>();
        Map<Integer, String> valueMap;

        for (Map.Entry<PhotoPack, Map<Integer, String>> entry : selectionMap.entrySet()) {
            valueMap = entry.getValue();
            for (Map.Entry<Integer, String> valueEntry : valueMap.entrySet()) {
                photoPathList.add(valueEntry.getValue());
            }
        }

        photoPathArray = new String[photoPathList.size()];
        photoPathList.toArray(photoPathArray);

        return photoPathArray;
    }

}
