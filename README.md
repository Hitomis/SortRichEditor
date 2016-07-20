# SortRichEditor

支持图片文字混合编辑、排序的富文本编辑器

目前暂时支持的功能：

 - 支持图片文字混合添加、修改、删除
 - 支持文字中间随意插入一张或多张图片
 - 支持图片文字任意排序


# Preview

由于 gif 图片较大，网速不好的童鞋请耐心等待... 如果发现图片里面卡，那是还在缓冲。真实项目是很流畅的

<img src="preview/SortRichEditor.gif"/>


# Usage

目前没有做很好的封装，如果需要使用SortRichEditor，请复制 editor 包中全部文件到您的项目当中 <br/>
以及以下的资源文件 <br/>
- shape_dash_edit.xml
- icon_add_text.png (xhdpi)
- icon_delete.png (xhdpi)
- icon_empty_photo (xhdpi)

copy完成后，在布局文件中使用
```
    <com.hitomi.sortricheditor.view.editor.SortRichEditor
        android:id="@+id/richEditor"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#fff" />
```
SortRichEditor不包含照片墙、选择照片插入照片、拍照插入等功能，SortRichEditor只提供可插入图片的方法
如果需要以上功能，可以参照本项目其他代码。以后会将这些功能组件封装在里面。


# TODO

- [x] 图片压缩问题防止OOM
- [x] 优化插入图片的速度
- [x] 优化软键盘的显示和隐藏
- [ ] 重构SortRichEditor类

# Thanks
- [@xmuSistone][1]
- [@张鸿洋][2]


[1]: https://github.com/xmuSistone/android-animate-RichEditor
[2]: https://github.com/hongyangAndroid
