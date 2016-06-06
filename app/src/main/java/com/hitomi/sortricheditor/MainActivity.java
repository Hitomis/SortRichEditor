package com.hitomi.sortricheditor;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import com.hitomi.sortricheditor.view.SortRichEditor;
import com.hitomi.sortricheditor.view.SortRichEditorData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int REQUEST_CODE_PICK_IMAGE = 1023;
    private static final int REQUEST_CODE_CAPTURE_CAMEIA = 1022;
    private static final File PHOTO_DIR = new File(
            Environment.getExternalStorageDirectory() + "/DCIM/Camera");

    private SortRichEditor editor;

    private ImageView ivGallery, ivCamera;

    private Button btnPosts;

    private String str;

    private File mCurrentPhotoFile;// 照相机拍照得到的图片

    public static Intent getTakePickIntent(File f) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort:
                if (editor.sort()) {
                    dealInsertImage(false);
                    item.setTitle("完成");
                } else {
                    dealInsertImage(true);
                    item.setTitle("排序");
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void dealInsertImage(boolean isEnable) {
        ivGallery.setClickable(isEnable);
        ivCamera.setClickable(isEnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        editor = (SortRichEditor) findViewById(R.id.richEditor);
        ivGallery = (ImageView) findViewById(R.id.iv_gallery);
        ivCamera = (ImageView) findViewById(R.id.iv_camera);
        btnPosts = (Button) findViewById(R.id.btn_posts);

        ivGallery.setOnClickListener(this);
        ivCamera.setOnClickListener(this);
        btnPosts.setOnClickListener(this);
    }

    /**
     * 负责处理编辑数据提交等事宜，请自行实现
     */
    protected void dealEditData(List<SortRichEditorData> editList) {
        for (SortRichEditorData itemData : editList) {
            if (itemData.getInputStr() != null) {
                Log.d("RichEditor", "commit inputStr=" + itemData.getInputStr());
            } else if (itemData.getImagePath() != null) {
                Log.d("RichEditor", "commit imgePath=" + itemData.getImagePath());
            }

        }
    }

    protected void openCamera() {
        try {
            // Launch camera to take photo for selected contact
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
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            Uri uri = data.getData();
            insertBitmap(getRealFilePath(uri));
        } else if (requestCode == REQUEST_CODE_CAPTURE_CAMEIA) {
            insertBitmap(mCurrentPhotoFile.getAbsolutePath());
        }
    }

    /**
     * 添加图片到富文本剪辑器
     *
     * @param imagePath
     */
    private void insertBitmap(String imagePath) {
        editor.insertImage(imagePath);
    }

    /**
     * 根据Uri获取图片文件的绝对路径
     */
    public String getRealFilePath(final Uri uri) {
        if (null == uri) {
            return null;
        }

        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = getContentResolver().query(uri,
                    new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_gallery:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");// 相片类型
                startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
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
