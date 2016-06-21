package com.hitomi.sortricheditor.view.editor;

import com.hitomi.sortricheditor.model.SortRichEditorData;

import java.util.List;

/**
 * Created by hitomi on 2016/6/21.
 */
public interface EditorDataI {

    /**
     * 根据绝对路径添加一张图片
     *
     * @param imagePath
     */
    public void addImage(String imagePath);

    /**
     * 根据图片绝对路径数组批量添加一组图片
     *
     * @param imagePaths
     */
    public void addImageArray(String[] imagePaths);

    /**
     * 根据图片绝对路径集合批量添加一组图片
     *
     * @param imageList
     */
    public void addImageList(List<String> imageList);

    /**
     * 获取标题
     *
     * @return
     */
    public String getTitleData();

    /**
     * 生成编辑数据
     */
    public List<SortRichEditorData> buildEditData();

    /**
     * 编辑器内容是否为空
     *
     * @return
     */
    public boolean isContentEmpty();

}
