package com.hitomi.sortricheditor.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.hitomi.sortricheditor.R;
import com.hitomi.sortricheditor.model.SortRichEditorData;
import com.hitomi.sortricheditor.view.editor.SortRichEditor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int REQUEST_CODE_PICK_IMAGE = 1023;
    public static final int REQUEST_CODE_CAPTURE_CAMEIA = 1022;

    private static final File PHOTO_DIR = new File(
            Environment.getExternalStorageDirectory() + "/DCIM/Camera");

    private TextView tvSort;

    private SortRichEditor editor;

    private ImageView ivGallery, ivCamera;

    private Button btnPosts;

    private File mCurrentPhotoFile;// 照相机拍照得到的图片

    public Intent getTakePickIntent(File f) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        tvSort = (TextView) findViewById(R.id.tv_sort);
        editor = (SortRichEditor) findViewById(R.id.richEditor);
        ivGallery = (ImageView) findViewById(R.id.iv_gallery);
        ivCamera = (ImageView) findViewById(R.id.iv_camera);
        btnPosts = (Button) findViewById(R.id.btn_posts);

        tvSort.setOnClickListener(this);
        ivGallery.setOnClickListener(this);
        ivCamera.setOnClickListener(this);
        btnPosts.setOnClickListener(this);
    }

    /**
     * 负责处理编辑数据提交等事宜，请自行实现
     */
    private void dealEditData(List<SortRichEditorData> editList) {
        for (SortRichEditorData itemData : editList) {
            if (itemData.getInputStr() != null) {
                Log.d("RichEditor", "commit inputStr=" + itemData.getInputStr());
            } else if (itemData.getImagePath() != null) {
                Log.d("RichEditor", "commit imgePath=" + itemData.getImagePath());
            }
        }
    }

    private void openCamera() {
        try {
            PHOTO_DIR.mkdirs();// 创建照片的存储目录
            mCurrentPhotoFile = new File(PHOTO_DIR, getPhotoFileName());// 给新照的照片文件命名
            final Intent intent = getTakePickIntent(mCurrentPhotoFile);
            startActivityForResult(intent, REQUEST_CODE_CAPTURE_CAMEIA);
        } catch (ActivityNotFoundException e) {
        }
    }

    /**
     * 用当前时间给取得的图片命名
     */
    private String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "'IMG'_yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date) + ".jpg";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        if (editor.isSort()) tvSort.setText("排序");
        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            String[] photoPaths = data.getStringArrayExtra(PhotoPickerActivity.INTENT_PHOTO_PATHS);
            editor.addImageArray(photoPaths);
        } else if (requestCode == REQUEST_CODE_CAPTURE_CAMEIA) {
            editor.addImage(mCurrentPhotoFile.getAbsolutePath());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_sort:
                if (editor.sort()) {
                    tvSort.setText("完成");
                } else {
                    tvSort.setText("排序");
                }
                break;
            case R.id.iv_gallery:
                startActivityForResult(new Intent(this, PhotoPickerActivity.class), REQUEST_CODE_PICK_IMAGE);
                break;
            case R.id.iv_camera:
                openCamera();
                break;
            case R.id.btn_posts:
                List<SortRichEditorData> editList = editor.buildEditData();
                // 下面的代码可以上传、或者保存，请自行实现
                dealEditData(editList);
                break;
        }
    }


}
