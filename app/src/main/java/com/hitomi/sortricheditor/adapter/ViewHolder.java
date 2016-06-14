package com.hitomi.sortricheditor.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewHolder {

    /**
     * 缓存item视图以便复用item view的容器
     */
    private final SparseArray<View> views;

    private View convertView;

    private ViewHolder(Context context, ViewGroup parent, int layoutID) {
        this.views = new SparseArray<View>();
        this.convertView = LayoutInflater.from(context).inflate(layoutID, parent, false);
        convertView.setTag(this);
    }

    /**
     * 获取ViewHolder对象
     *
     * @param context
     * @param convertView
     * @param parent
     * @param layoutID    item视图 布局ID
     * @return
     */
    public static ViewHolder get(Context context, View convertView, ViewGroup parent, int layoutID) {
        if (convertView == null) {
            return new ViewHolder(context, parent, layoutID);
        } else {
            return (ViewHolder) convertView.getTag();
        }
    }

    /**
     * 通过控件ID获取对应的控件，如果没有再加入views
     *
     * @param viewID
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T getView(int viewID) {
        View view = views.get(viewID);
        if (view == null) {
            view = convertView.findViewById(viewID);
            views.put(viewID, view);
        }
        return (T) view;
    }

    public View getConvertView() {
        return convertView;
    }

    /**
     * 为TextView设置字符串
     *
     * @param viewId
     * @param text
     * @return
     */
    public ViewHolder setText(int viewId, String text) {
        TextView view = getView(viewId);
        view.setText(text);
        return this;
    }

    /**
     * 设置TextView文本颜色
     *
     * @param viewId
     * @param color
     * @return
     */
    public ViewHolder setTextColor(int viewId, int color) {
        TextView view = getView(viewId);
        view.setTextColor(color);
        return this;
    }

    /**
     *  设置ImageView资源图片
     * @param viewId
     * @param imageResource
     * @return
     */
    public ViewHolder setImageResource(int viewId, int imageResource) {
        ImageView view = getView(viewId);
        view.setImageResource(imageResource);
        return this;
    }
}
