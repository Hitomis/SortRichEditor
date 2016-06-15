package com.hitomi.sortricheditor.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.hitomi.sortricheditor.R;
import com.hitomi.sortricheditor.adapter.PhotoWallAdapter;
import com.hitomi.sortricheditor.components.DensityUtil;
import com.hitomi.sortricheditor.model.PhotoPack;
import com.hitomi.sortricheditor.view.SelectPopupWindow;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;

public class PhotoPickerActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String INTENT_PHOTO_PATHS = "photo_paths";;

    private static final int LATEST_PHOTO_NUM = 100;

    private static final String LATEST_PHOTO_STR = "最近照片";

    private View titleView;

    private TextView tvCacel, tvComplete, tvPicker;

    private GridView gvPhotoWall;

    private List<String> photoPathList;

    private List<String> latestPhotoPathList;

    private List<PhotoPack> photoPackList;

    private PhotoWallAdapter photoWallAdapter;

    private SelectPopupWindow photoPackSelectPopup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_picker);

        initView();

        setViewListener();

        inflateViewData();
    }

    private void initView() {
        titleView = findViewById(R.id.relay_title);

        tvCacel = (TextView) findViewById(R.id.tv_cancel);
        tvComplete = (TextView) findViewById(R.id.tv_complete);
        tvPicker = (TextView) findViewById(R.id.tv_picker);

        gvPhotoWall = (GridView) findViewById(R.id.gv_photo_wall);
    }

    private void setViewListener() {
        tvCacel.setOnClickListener(this);
        tvComplete.setOnClickListener(this);
        tvPicker.setOnClickListener(this);
    }


    private void inflateViewData() {

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                photoPathList = getLatestPhotoPaths(LATEST_PHOTO_NUM);
                latestPhotoPathList = new ArrayList<>(photoPathList);
                photoPackList = getAllPhotoPaths();

                PhotoPack LatestPhotoPack = new PhotoPack();
                LatestPhotoPack.setFileCount(photoPathList.size());
                LatestPhotoPack.setPathName(LATEST_PHOTO_STR);
                LatestPhotoPack.setTitle(LATEST_PHOTO_STR);
                LatestPhotoPack.setFirstPhotoPath(photoPathList.get(0));

                photoPackList.add(0, LatestPhotoPack);
                handler.sendEmptyMessage(0);
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_cancel:
                finish();
                break;

            case R.id.tv_complete:
                Intent dataIntent = new Intent();
                dataIntent.putExtra(INTENT_PHOTO_PATHS, photoWallAdapter.getSelectPhotoPathArray());
                setResult(RESULT_OK, dataIntent);
                finish();
                break;

            case R.id.tv_picker:
                tvPicker.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.icon_arrow_up, 0);
                if (photoPackSelectPopup == null) {
                    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
                    ListView photoPackListView = new ListView(this);
                    photoPackListView.setLayoutParams(layoutParams);
                    photoPackListView.setDivider(getResources().getDrawable(R.color.photoPackDivider));
                    photoPackListView.setDividerHeight(1);

                    photoPackSelectPopup = new SelectPopupWindow(photoPackListView,
                            DensityUtil.getScreenWidth(this),
                            (int) (DensityUtil.getScreenHeight(this) * .5f), photoPackList);

                    photoPackSelectPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            photoPackSelectPopup.transformWindowNormal(PhotoPickerActivity.this);
                            tvPicker.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.icon_arrow_down, 0);
                        }
                    });
                }

                photoPackSelectPopup.showDropDownAsView(titleView, new SelectPopupWindow.OnPhotoPackSelectListener() {
                    @Override
                    public void photoPackSelected(PhotoPack photoPack) {
                        tvPicker.setText(photoPack.getTitle());
                        tvPicker.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.icon_arrow_down, 0);
                        if (LATEST_PHOTO_STR.equals(photoPack.getPathName())) { // 最近照片
                            photoPathList = latestPhotoPathList;
                        } else {
                            photoPathList = getPhotosByFolder(photoPack.getPathName());
                        }

                        photoWallAdapter.cutoverSelectArray(photoPack);
                        photoWallAdapter.setDataList(photoPathList);
                        photoWallAdapter.notifyDataSetChanged();
                    }
                });
                break;
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            photoWallAdapter = new PhotoWallAdapter(PhotoPickerActivity.this, photoPathList,
                    R.layout.item_photo_picker, photoPackList.get(0));
            gvPhotoWall.setAdapter(photoWallAdapter);

            tvPicker.setText(photoPackList.get(0).getTitle());
        }
    };

    /**
     * 使用ContentProvider读取SD卡最近图片
     * @param maxCount 读取的最大张数
     * @return
     */
    private List<String> getLatestPhotoPaths(int maxCount) {
        Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String key_MIME_TYPE = MediaStore.Images.Media.MIME_TYPE;
        String key_DATA = MediaStore.Images.Media.DATA;

        ContentResolver mContentResolver = getContentResolver();

        // 只查询jpg和png的图片,按最新修改排序
        Cursor cursor = mContentResolver.query(mImageUri, new String[]{key_DATA},
                key_MIME_TYPE + "=? or " + key_MIME_TYPE + "=? or " + key_MIME_TYPE + "=?",
                new String[]{"image/jpg", "image/jpeg", "image/png"},
                MediaStore.Images.Media.DATE_MODIFIED);

        List<String> latestImagePaths = null;
        if (cursor != null) {
            //从最新的图片开始读取.
            //当cursor中没有数据时，cursor.moveToLast()将返回false
            if (cursor.moveToLast()) {
                latestImagePaths = new ArrayList<String>();

                while (true) {
                    // 获取图片的路径
                    String path = cursor.getString(0);
                    latestImagePaths.add(path);

                    if (latestImagePaths.size() >= maxCount || !cursor.moveToPrevious()) {
                        break;
                    }
                }
            }
            cursor.close();
        }

        return latestImagePaths;
    }

    /**
     * 获取指定路径下的所有图片文件
     * @param folderPath
     * @return
     */
    private List<String> getPhotosByFolder(String folderPath) {
        File folder = new File(folderPath);
        String[] allFileNames = folder.list();
        if (allFileNames == null || allFileNames.length == 0) {
            return null;
        }

        List<String> photoFilePaths = new ArrayList<String>();
        for (int i = allFileNames.length - 1; i >= 0; i--) {
            if (isImage(allFileNames[i])) {
                photoFilePaths.add(folderPath + File.separator + allFileNames[i]);
            }
        }

        return photoFilePaths;
    }

    /**
     * 使用ContentProvider读取SD卡所有图片。
     */
    private List<PhotoPack> getAllPhotoPaths() {
        Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String key_MIME_TYPE = MediaStore.Images.Media.MIME_TYPE;
        String key_DATA = MediaStore.Images.Media.DATA;

        ContentResolver mContentResolver = getContentResolver();

        // 只查询jpg和png的图片
        Cursor cursor = mContentResolver.query(mImageUri, new String[]{key_DATA},
                key_MIME_TYPE + "=? or " + key_MIME_TYPE + "=? or " + key_MIME_TYPE + "=?",
                new String[]{"image/jpg", "image/jpeg", "image/png"},
                MediaStore.Images.Media.DATE_MODIFIED);

        ArrayList<PhotoPack> list = null;
        if (cursor != null) {
            if (cursor.moveToLast()) {
                //路径缓存，防止多次扫描同一目录
                HashSet<String> cachePath = new HashSet<String>();
                list = new ArrayList<PhotoPack>();
                PhotoPack photoPack;
                while (true) {
                    // 获取图片的路径
                    String imagePath = cursor.getString(0);

                    File parentFile = new File(imagePath).getParentFile();
                    String parentPath = parentFile.getAbsolutePath();

                    //不扫描重复路径
                    if (!cachePath.contains(parentPath)) {
                        photoPack = new PhotoPack();
                        photoPack.setFileCount(getPhotoCount(parentFile));
                        photoPack.setFirstPhotoPath(getCoverPhotoPath(parentFile));
                        photoPack.setPathName(parentPath);
                        photoPack.setTitle(getPhotoPackTitle(parentPath));
                        list.add(photoPack);
                        cachePath.add(parentPath);
                    }

                    if (!cursor.moveToPrevious()) {
                        break;
                    }
                }
            }

            cursor.close();
        }

        return list;
    }

    private String getPhotoPackTitle(String parentPath) {
        int lastSeparator = parentPath.lastIndexOf(File.separator);
        return parentPath.substring(lastSeparator + 1);
    }

    /**
     * 获取目录中图片的个数。
     */
    private int getPhotoCount(File folder) {
        int count = 0;
        File[] files = folder.listFiles();
        for (File file : files) {
            if (isImage(file.getName())) {
                count++;
            }
        }

        return count;
    }

    /**
     * 获取目录中最新的一张图片的绝对路径
     * @param folder
     * @return
     */
    private String getCoverPhotoPath(File folder) {
        File[] files = folder.listFiles();
        for (int i = files.length - 1; i >= 0; i--) {
            File file = files[i];
            if (isImage(file.getName())) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * 判断该文件是否是一个图片
     * @param fileName
     * @return
     */
    private boolean isImage(String fileName) {
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png");
    }
}
