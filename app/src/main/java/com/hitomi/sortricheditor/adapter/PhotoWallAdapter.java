package com.hitomi.sortricheditor.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseArray;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.hitomi.sortricheditor.R;
import com.hitomi.sortricheditor.components.ImageLoader;
import com.hitomi.sortricheditor.model.PhotoPack;

import java.util.ArrayList;
import java.util.HashMap;
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
    private SparseArray<String> selectionSparse;

    /**
     * 记录所有相册中所有被选中checkbox的照片路径
     */
    private Map<PhotoPack, SparseArray<String>> selectionMap;

    public PhotoWallAdapter(Context context, List<String> dataList, int itemLayoutID, PhotoPack defaultPhotoPack) {
        super(context, dataList, itemLayoutID);

        selectionSparse = new SparseArray<>();
        selectionMap = new HashMap<>();
        selectionMap.put(defaultPhotoPack, selectionSparse);
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
                    selectionSparse.put(position, item);
                    photo.setColorFilter(Color.parseColor("#66000000"));
                } else {
                    selectionSparse.remove(position);
                    photo.setColorFilter(null);
                }
            }
        });
        checkBox.setChecked(item.equals(selectionSparse.get(position)));
    }

    public void cutoverSelectArray(PhotoPack selectPhotoPack) {
        selectionSparse = selectionMap.get(selectPhotoPack);
        if (selectionSparse == null) {
            selectionSparse = new SparseArray<>();
            selectionMap.put(selectPhotoPack, selectionSparse);
        }
    }

    public Map<PhotoPack, SparseArray<String>> getSelectPhotoPathMap() {
        return selectionMap;
    }

    /**
     * 获取选中照片的路径数组
     * @return
     */
    public String[] getSelectPhotoPathArray() {
        String [] photoPathArray = null;
        List<String> photoPathList = new ArrayList<>();
        SparseArray<String> valueArray;

        for (Map.Entry<PhotoPack, SparseArray<String>> entry : selectionMap.entrySet()) {
            valueArray = entry.getValue();
            for (int i = 0; i < valueArray.size(); i++) {
                photoPathList.add(valueArray.valueAt(i));
            }
        }

        photoPathArray = new String[photoPathList.size()];
        photoPathList.toArray(photoPathArray);

        return photoPathArray;
    }

}
