package com.hitomi.sortricheditor.view;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.hitomi.sortricheditor.R;
import com.hitomi.sortricheditor.adapter.BaseAdapterHelper;
import com.hitomi.sortricheditor.adapter.ViewHolder;
import com.hitomi.sortricheditor.components.ImageLoader;
import com.hitomi.sortricheditor.model.PhotoPack;

import java.io.File;
import java.util.List;

/**
 * Created by hitomi on 2016/6/14.
 */
public class SelectPopupWindow extends PopupWindow {

    private Activity activity;

    private ListView popupView;

    private List<PhotoPack> dataList;

    private int currSelectPos;

    private OnPhotoPackSelectListener photoPackSelectListener;

    public interface OnPhotoPackSelectListener {
        void photoPackSelected(PhotoPack photoPack);
    }

    public SelectPopupWindow(ListView contentView, int width, int height, List<PhotoPack> photoPacks) {
        super(contentView, width, height);

        popupView = contentView;
        activity = (Activity) contentView.getContext();
        dataList = photoPacks;

        initPopupWindow();

        initView();
    }

    private void initPopupWindow() {
        // 设置 PopupWindow 可以获取焦点[在失去焦点的时候,会自动关闭PopupWindow]
        setFocusable(true);
        // 设置 PopupWindow 背景色
        setBackgroundDrawable(activity.getResources().getDrawable(android.R.color.white));
        setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {

                transformWindowNormal(activity);
            }
        });
    }

    private void initView() {
        BaseAdapterHelper<PhotoPack> baseAdapterHelper = new BaseAdapterHelper<PhotoPack>(activity, dataList, R.layout.item_photopack_picker) {

            private ImageLoader imageLoader = ImageLoader.getInstance(3, ImageLoader.Type.LIFO);

            @Override
            public void convert(ViewHolder viewHolder, PhotoPack item, int position) {

                ImageView ivCoverPhoto = viewHolder.getView(R.id.iv_cover_photo);
                ImageView ivCheck = viewHolder.getView(R.id.iv_check);

                // 加载相册封面
                ivCoverPhoto.setImageResource(R.mipmap.icon_empty_photo);
                imageLoader.loadImage(item.getFirstPhotoPath(), ivCoverPhoto);

                // 设置相册标题
                viewHolder.setText(R.id.tv_photopack_name, String.format("%s ( %d )", item.getTitle(), item.getFileCount()));

                // 设置当前是否选中相册
                ivCheck.setVisibility(position == currSelectPos ? View.VISIBLE : View.GONE);
            }
        };

        popupView.setAdapter(baseAdapterHelper);
        popupView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currSelectPos = position;
                photoPackSelectListener.photoPackSelected(dataList.get(position));
                dismiss();
            }
        });
    }

    /**
     * 在窗口的 gravity 方位显示 SelectPopupWindow
     * @param gravity
     */
    public void showAtLocaiton(int gravity, OnPhotoPackSelectListener listener) {
        transformWindowShadow(activity);
        photoPackSelectListener = listener;
        showAtLocation(activity.getWindow().getDecorView(), gravity, 0, 0);
    }

    /**
     * 在 view 的下方显示 SelectPopupWindow
     * @param view
     */
    public void showDropDownAsView(View view, OnPhotoPackSelectListener listener) {
        transformWindowShadow(activity);
        photoPackSelectListener = listener;
        showAsDropDown(view);
    }

    /**
     * 窗口背景色变半透明
     * @param activity
     */
    public void transformWindowShadow(Activity activity) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(1.f, .5f);
        final Window window = activity.getWindow();
        transformWindow(valueAnimator, window);
    }

    /**
     * 窗口背景色变为正常
     * @param activity
     */
    public void transformWindowNormal(Activity activity) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(.5f, 1.f);
        final Window window = activity.getWindow();
        transformWindow(valueAnimator, window);
    }

    /**
     * 转变窗口背景色
     * @param valueAnimator
     * @param window
     */
    private void transformWindow(ValueAnimator valueAnimator, final Window window) {
        valueAnimator.setDuration(300);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.alpha = (float) animation.getAnimatedValue();
                window.setAttributes(attributes);
            }
        });
        valueAnimator.start();
    }

}
