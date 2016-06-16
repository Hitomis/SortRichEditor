package com.hitomi.sortricheditor.view;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.widget.ViewDragHelper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hitomi.sortricheditor.R;
import com.hitomi.sortricheditor.model.SortRichEditorData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 富文本编辑器
 * 1、支持图片文字添加、修改、删除
 * 2、支持图片文字混排
 * 3、支持文字中间随意插入图片
 * 4、支持图片文字任意排序
 */
public class SortRichEditor extends ScrollView {

    /**
     * 默认ImageView高度
     */
    public final int DEFAULT_IMAGE_HEIGHT = dip2px(170);

    /**
     * 图文排序的时候，view默认缩小的高度
     */
    public final int SIZE_REDUCE_VIEW = dip2px(75);

    /**
     * 出发ScrollView滚动时，顶部与底部的偏移量
     */
    private final int SCROLL_OFFSET = (int)(SIZE_REDUCE_VIEW * .3);

    /**
     * 默认Marging
     */
    private final int DEFAULT_MARGING = dip2px(15);

    /**
     * 拖动排序的时候，当在ScrollView边界拖动时默认自滚动速度
     */
    private final int DEFAULT_SCROLL_SPEED = dip2px(15);

    /**
     * 标题字数限制
     */
    private static final int TITLE_WORD_LIMIT_COUNT = 30;

    /**
     * 因为排序状态下会修改EditText的Background，所以这里保存默认EditText
     * 的Background, 当排序完成后用于还原EditText默认的Background
     */
    private Drawable editTextBackground;

    /**
     * 布局填充器
     */
    private LayoutInflater inflater;

    /**
     * 每创建一个child，为该child赋一个ID，该ID保存在view的tag属性中
     */
    private int viewTagID = 1;

    /**
     * 因为ScrollView的子view只能有一个，并且是ViewGroup
     * 所以这里指定为所有子view的容器为parentLayout(LinearLayout)
     * 即：布局层次为：
     * ScrollView{
     *      parentLayout{
     *          titleLayout{
     *              EditText,
     *              TExtView
     *          },
     *
     *          LineView,
     *
     *         containerLayout{
     *              child1,
     *              child2,
     *              child3,
     *              ...
     *          }
     *      }
     * }
     */
    private LinearLayout parentLayout;

    /**
     * 标题栏ViewGroup
     */
    private LinearLayout titleLayout;

    /**
     * 用于放置各种文本图片内容的容器
     */
    private LinearLayout containerLayout;

    /**
     * EditText的软键盘监听器
     */
    private OnKeyListener editTextKeyListener;

    /**
     * 图片右上角删除按钮监听器
     */
    private OnClickListener deleteListener;

    /**
     * EditText和ImageView的焦点监听listener
     */
    private OnFocusChangeListener focusListener;

    /**
     * 最近获取焦点的一个EditText
     */
    private EditText lastFocusEdit;

    /**
     * 标题栏EditText
     */
    private DeletableEditText etTitle;

    /**
     * 添加或者删除图片View时的Transition动画
     */
    private LayoutTransition mTransitioner;

    /**
     * 用于实现拖动效果的帮助类
     */
    private ViewDragHelper viewDragHelper;

    /**
     * 因为文字长短不一（过长换行让EditText高度增大），导致EditText高度不一，
     * 所以需要一个集合存储排序之前未缩小/放大的EditText高度
     */
    private SparseArray<Integer> editTextHeightArray = new SparseArray<>();

    /**
     * 准备排序时，缩小各个child，并存放缩小的child的top作为该child的position值
     */
    private SparseArray<Integer> preSortPositionArray;

    /**
     * 排序完成后，子child位置下标
     */
    private SparseIntArray indexArray = new SparseIntArray();

    /**
     * 当前是否为排序状态
     */
    private boolean isSort;

    /**
     * 容器相对于屏幕顶部和底部的长度值，用于排序拖动Child的时候判定ScrollView是否滚动
     */
    private int containerTopVal, containerBottomVal;

    /**
     * 循环线程执行器，用于拖动view到边缘时ScrollView自动滚动功能
     */
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * 是否正在自动滚动
     */
    private boolean isAutoScroll;

    /**
     * 自动滚动速度向量
     */
    private int scrollVector;

    private float currRawY;

    public SortRichEditor(Context context) {
        this(context, null);
    }

    public SortRichEditor(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SortRichEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflater = LayoutInflater.from(context);

        initListener();

        initParentLayout();

        initTitleLayout();

        initLineView();

        initContainerLayout();

        // 初始化ViewDragHelper
        viewDragHelper = ViewDragHelper.create(containerLayout, 1.5f, new ViewDragHelperCallBack());
    }

    /**
     * 初始化分割线（用来分开标题栏ViewGroup与内容容器ViewGroup）
     */
    private void initLineView() {
        // 父容器中中添加一条分割线用来分开标题栏ViewGroup与内容容器ViewGroup
        View lineView = new View(getContext());
        lineView.setBackgroundColor(Color.parseColor("#dddddd"));

        LinearLayout.LayoutParams lineLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 1);
        lineLayoutParams.leftMargin = DEFAULT_MARGING;
        lineLayoutParams.rightMargin = DEFAULT_MARGING;
        lineView.setLayoutParams(lineLayoutParams);
        parentLayout.addView(lineView);
    }

    /**
     * 创建标题栏ViewGroup以及标题栏中编辑标题的EditText和字数提醒的TextView
     */
    private void initTitleLayout() {

        // 创建标题栏的ViewGroup
        titleLayout = new LinearLayout(getContext());
        titleLayout.setOrientation(LinearLayout.VERTICAL);
        titleLayout.setPadding(0, DEFAULT_MARGING, 0, DEFAULT_MARGING);

        LinearLayout.LayoutParams titleLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleLayout.setLayoutParams(titleLayoutParams);

        parentLayout.addView(titleLayout);

        // 标题栏的ViewGroup中添加一个显示字数限制的提醒TextView(先创建，待先插入标题栏EditText之后再插入tvTextLimit)
        final TextView tvTextLimit = new TextView(getContext());
        tvTextLimit.setText(String.format("0/%d", TITLE_WORD_LIMIT_COUNT));
        tvTextLimit.setTextColor(Color.parseColor("#aaaaaa"));
        tvTextLimit.setTextSize(13);

        LinearLayout.LayoutParams textLimitLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        textLimitLayoutParams.rightMargin = DEFAULT_MARGING;
        textLimitLayoutParams.gravity = Gravity.RIGHT;
        tvTextLimit.setLayoutParams(textLimitLayoutParams);

        // 标题栏的ViewGroup中添加一个EditText，用来填写标题文本
        etTitle = new DeletableEditText(getContext());
        etTitle.setHint("请输入帖子标题");
        etTitle.setGravity(Gravity.TOP);
        etTitle.setCursorVisible(true);
        InputFilter[] filters = {new InputFilter.LengthFilter(TITLE_WORD_LIMIT_COUNT)};
        etTitle.setFilters(filters);
        etTitle.setBackgroundResource(android.R.color.transparent);
        etTitle.setTextColor(Color.parseColor("#333333"));
        etTitle.setTextSize(14);
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String titleStr = etTitle.getText().toString();
                tvTextLimit.setText(String.format("%d/%d", titleStr.length(), TITLE_WORD_LIMIT_COUNT));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        LinearLayout.LayoutParams editTitleLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        editTitleLayoutParams.leftMargin = DEFAULT_MARGING;
        editTitleLayoutParams.rightMargin = DEFAULT_MARGING;
        etTitle.setLayoutParams(editTitleLayoutParams);

        titleLayout.addView(etTitle);

        titleLayout.addView(tvTextLimit);
    }

    /**
     * 创建父容器LinearLayout，指定为ScrollView的子View
     */
    private void initParentLayout() {
        // 因为ScrollView的子view只能有一个，并且是ViewGroup,所以先创建一个Linearlayout父容器，用来放置所有其他ViewGroup
        parentLayout = new LinearLayout(getContext());
        parentLayout.setOrientation(LinearLayout.VERTICAL);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        parentLayout.setLayoutParams(layoutParams);

        addView(parentLayout);
    }

    /**
     * 初始化监听器
     */
    private void initListener() {
        // 初始化键盘退格监听
        // 主要用来处理点击回删按钮时，view的一些列合并操作
        editTextKeyListener = new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    EditText edit = (EditText) v;
                    onBackspacePress(edit);
                }
                return false;
            }
        };

        // 图片删除处理
        deleteListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                RelativeLayout parentView = (RelativeLayout) v.getParent();
                onImageDeleteClick(parentView);
            }
        };

        focusListener = new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (v instanceof RelativeLayout) { // 图片
                    processSoftKeyBoard(false);
                } else if (v instanceof EditText) {
                    if (hasFocus) {
                        lastFocusEdit = (EditText) v;
                    }
                }

            }
        };
    }

    /**
     * 初始化ContainerLayout文本内容容器
     */
    private void initContainerLayout() {
        containerLayout = createContaniner();
        parentLayout.addView(containerLayout);

        EditText firstEdit = createEditText("请输入帖子内容");
        editTextHeightArray.put(Integer.parseInt(firstEdit.getTag().toString()), ViewGroup.LayoutParams.WRAP_CONTENT);
        editTextBackground = firstEdit.getBackground();
        containerLayout.addView(firstEdit);
        lastFocusEdit = firstEdit;
    }

    /**
     * 停止ScrollView的自动滚动
     */
    private void stopOverEdgeAutoScroll() {
        if (isAutoScroll) {
            scheduledExecutorService.shutdownNow();
            isAutoScroll = false;
        }
    }

    /**
     * 拖动view到或者超出边缘时，ScrollView开始自动滚动
     */
    private void startOverEdgeAutoScroll() {
        if (!isAutoScroll) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    SortRichEditor.this.scrollBy(0, scrollVector);
                }
            }, 0, 15, TimeUnit.MILLISECONDS);
            isAutoScroll = true;
        }
    }

    /**
     * 创建图片文本内容容器
     * @return
     */
        @NonNull
        private LinearLayout createContaniner() {
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        final LinearLayout containerLayout = new LinearLayout(getContext()) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                return viewDragHelper.shouldInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                viewDragHelper.processTouchEvent(event);
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isSort) {
                            currRawY = event.getRawY();
                            if (currRawY > containerBottomVal) { // 内容上滚动
                                scrollVector = DEFAULT_SCROLL_SPEED;
                                startOverEdgeAutoScroll();
                            } else if (currRawY < containerTopVal) { // 内容下滚动
                                scrollVector = -DEFAULT_SCROLL_SPEED;
                                startOverEdgeAutoScroll();
                            }else {
                                stopOverEdgeAutoScroll();
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        stopOverEdgeAutoScroll();
                        break;
                }
                return true;
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (isSort) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return super.dispatchTouchEvent(ev);
            }


        };
        containerLayout.setPadding(0, DEFAULT_MARGING, 0, DEFAULT_MARGING);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        containerLayout.setBackgroundColor(Color.WHITE);
        containerLayout.setLayoutParams(layoutParams);
        setupLayoutTransitions(containerLayout);
        return containerLayout;
    }

    /**
     * 获取排序之前子View的LayoutParams用于还原子View大小
     * @param child
     * @return
     */
    private ViewGroup.LayoutParams resetChildLayoutParams(View child) {
        ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
        if (child instanceof RelativeLayout) { // 图片
            layoutParams.height = DEFAULT_IMAGE_HEIGHT;
        }
        if (child instanceof EditText) { // 文本编辑框
            requestTriggerFocus(child);
            child.setBackgroundDrawable(editTextBackground);
            layoutParams.height = editTextHeightArray.get(Integer.parseInt(child.getTag().toString()));
        }
        return layoutParams;
    }

    /**
     * 请求获取焦点
     * @param child
     */
    private void requestTriggerFocus(View child) {
        child.setFocusable(true);
        child.setFocusableInTouchMode(true);
        child.requestFocus();
        lastFocusEdit = (EditText) child;
    }

    public boolean sort() {
        isSort = !isSort;
        containerLayout.setLayoutTransition(null);
        if (isSort) {
            prepareSortUI();
            prepareSortConfig();
            processSoftKeyBoard(false);
        } else {
            endSortUI();
        }
        // 恢复transition动画
        containerLayout.setLayoutTransition(mTransitioner);
        return isSort;
    }

    public boolean isSort() {
        return isSort;
    }

    /**
     * 开始图文排序
     * 图片与文字段落高度缩小为默认高度{@link #SIZE_REDUCE_VIEW}
     * 且图片与文字可以上下拖动
     */
    private void prepareSortUI() {
        int childCount = containerLayout.getChildCount();

        if (childCount != 0) {
            if (preSortPositionArray == null) {
                preSortPositionArray = new SparseArray<>();
            } else {
                preSortPositionArray.clear();
            }
        }

        List<View> removeChildList = new ArrayList<>();

        View child;
        int pos, preIndex = 0;
        for (int i = 0; i < childCount; i++) {
            child = containerLayout.getChildAt(i);

            if (child instanceof ImageView) {
                removeChildList.add(child);
                continue;
            }

            if (child instanceof RelativeLayout) {
                ((RelativeLayout) child).getChildAt(1).setVisibility(View.GONE);
                setFocusOnView(child, false);
            }

            int tagID = Integer.parseInt(child.getTag().toString());
            ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
            if (child instanceof EditText) { // 文本编辑框
                EditText editText = ((EditText) child);
                editTextHeightArray.put(tagID, layoutParams.height);
                editText.setFocusable(false);
                editText.setBackgroundResource(R.drawable.shape_dash_edit);
            }

            layoutParams.height = SIZE_REDUCE_VIEW;
            child.setLayoutParams(layoutParams);
            if (i == 0) {
                preIndex = tagID;
                pos = DEFAULT_MARGING;
            } else {
                pos = SIZE_REDUCE_VIEW + DEFAULT_MARGING + preSortPositionArray.get(preIndex);

                preIndex = tagID;
            }
            preSortPositionArray.put(tagID, pos);
        }

        if (!removeChildList.isEmpty()) { // 移除所有的“可编辑文本”图标
            for (View removeChild : removeChildList) {
                containerLayout.removeView(removeChild);
            }
        }
    }

    /**
     * 结束图文排序，图片还原为默认高度{@link #DEFAULT_IMAGE_HEIGHT}，文字还原为原本高度
     * （其文字排序前的高度值保存在{@link #editTextHeightArray}中）
     * 且图片文字不再可以上下拖动
     */
    private void endSortUI() {
        int childCount = containerLayout.getChildCount();
        View child;
        if (indexArray.size() == childCount) { // 重新排列过
            int sortIndex;
            View[] childArray = new View[childCount];
            // 1、先按重新排列的顺序调整子View的位置，放入数组childArray中
            for (int i = 0; i < childCount; i++) {
                if (indexArray.size() != childCount) break;
                // 代表原先在i的位置上的view，换到了sortIndex位置上
                sortIndex = indexArray.get(i);
                child = containerLayout.getChildAt(i);
                childArray[sortIndex] = child;
            }

            //2、依据顺序已排列好的childArray，插入一个“将来用于编辑文字的图片”
            List<View> sortViewList = new ArrayList<>();
            View preChild = childArray[0];
            sortViewList.add(preChild);
            for (int i = 1; i < childCount; i++) {
                child = childArray[i];
                if (preChild instanceof RelativeLayout && child instanceof RelativeLayout) {
                    ImageView ivInsertEditText = createInsertEditTextImageView();
                    sortViewList.add(ivInsertEditText);
                }
                sortViewList.add(child);
                preChild = child;
            }

            // 3、依据顺序已排好并且“用于编辑文字的图片”也插入完毕的sortViewList，依次往containerLayout中添加子View
            containerLayout.removeAllViews();
            for (View sortChild : sortViewList) {
                if (sortChild instanceof RelativeLayout) {
                    ((RelativeLayout) sortChild).getChildAt(1).setVisibility(View.VISIBLE);
                    setFocusOnView(sortChild, true);
                }
                sortChild.setLayoutParams(resetChildLayoutParams(sortChild));
                containerLayout.addView(sortChild);
            }

        } else { // 没有重新排列
            View preChild = containerLayout.getChildAt(childCount - 1);
            preChild.setLayoutParams(resetChildLayoutParams(preChild));
            for (int i = childCount - 2; i >= 0; i--) {
                child = containerLayout.getChildAt(i);
                if (child instanceof RelativeLayout) {
                    ((RelativeLayout) child).getChildAt(1).setVisibility(View.VISIBLE);
                    setFocusOnView(child, true);
                }
                // 紧邻的两个View都是ImageView
                if (preChild instanceof RelativeLayout && child instanceof RelativeLayout) {
                    insertEditTextImageView(i + 1);
                }
                child.setLayoutParams(resetChildLayoutParams(child));
                preChild = child;
            }
        }

        // 如果最后一个View不是EditText,那么再添加一个EditText
        int lastIndex = containerLayout.getChildCount() - 1;
        View view = containerLayout.getChildAt(lastIndex);
        if (!(view instanceof EditText)) {
            insertEditTextAtIndex(lastIndex + 1, "");
        }
    }

    /**
     * 处理软键盘backSpace回退事件
     *
     * @param editTxt 光标所在的文本输入框
     */
    private void onBackspacePress(EditText editTxt) {

        int startSelection = editTxt.getSelectionStart();
        // 只有在光标已经顶到文本输入框的最前方，在判定是否删除之前的图片，或两个View合并
        if (startSelection == 0) {
            int editIndex = containerLayout.indexOfChild(editTxt);
            View preView = containerLayout.getChildAt(editIndex - 1); // 如果editIndex-1<0,
            // 则返回的是null
            if (null != preView) {
                if (preView instanceof RelativeLayout || preView instanceof ImageView) {
                    // 光标EditText的上一个view对应的是图片或者是一个“将来可编辑文本”的图标
                    onImageDeleteClick(preView);
                } else if (preView instanceof EditText) {
                    // 光标EditText的上一个view对应的还是文本框EditText
                    String str1 = editTxt.getText().toString();
                    EditText preEdit = (EditText) preView;
                    String str2 = preEdit.getText().toString();

                    // 合并文本view时，不需要transition动画
                    containerLayout.setLayoutTransition(null);
                    containerLayout.removeView(editTxt);
                    containerLayout.setLayoutTransition(mTransitioner); // 恢复transition动画

                    // 文本合并
                    preEdit.setText(str2 + str1);
                    preEdit.requestFocus();
                    preEdit.setSelection(str2.length(), str2.length());
                    lastFocusEdit = preEdit;
                }
            }
        }
    }

    /**
     * 处理图片删除击事件
     *
     * @param view 整个image对应的relativeLayout view
     */
    private void onImageDeleteClick(View view) {
        if (!mTransitioner.isRunning()) {
            int index = containerLayout.indexOfChild(view);
            int nextIndex = index + 1;
            int lastIndex = index - 1;

            View child;
            if (index == 0) { // 删除图片位于第一个位置，只检查下一个位置的View是否为“可编辑文本”的图标
                child = containerLayout.getChildAt(nextIndex);
            } else {
                // 先检查上一个位置的View是否为“可编辑文本”的图标，如果不是就检查下一个位置的View
                child = containerLayout.getChildAt(lastIndex);
                if (!(child instanceof ImageView)) {
                    child = containerLayout.getChildAt(nextIndex);
                }
            }

            if (child instanceof ImageView) {
                // 如果该View是“可编辑文本”的图标，则一并删除
                containerLayout.removeView(child);
            }

            containerLayout.removeView(view);
        }
    }

    /**
     *
     * 生成一个“将来用于编辑文字的图片”ImageView
     * @return ImageView
     */
    @NonNull
    private ImageView createInsertEditTextImageView() {
        final ImageView ivInsertEditText = new ImageView(getContext());
        ivInsertEditText.setTag(viewTagID++);
        ivInsertEditText.setImageResource(R.mipmap.icon_add_text);
        ivInsertEditText.setScaleType(ImageView.ScaleType.FIT_START);
        ivInsertEditText.setClickable(true);
        ivInsertEditText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = containerLayout.indexOfChild(ivInsertEditText);
                containerLayout.removeView(ivInsertEditText);
                EditText editText = insertEditTextAtIndex(index, "");
                requestTriggerFocus(editText);
                processSoftKeyBoard(true);
            }
        });

        // 调整ImageView的外边距
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = DEFAULT_MARGING;
        ivInsertEditText.setLayoutParams(lp);

        return ivInsertEditText;
    }

    /**
     * 生成文本输入框
     */
    private EditText createEditText(String hint) {
        EditText editText = new DeletableEditText(getContext());
        editText.setTag(viewTagID++);
        editText.setHint(hint);
        editText.setGravity(Gravity.TOP);
        editText.setCursorVisible(true);
        editText.setBackgroundResource(android.R.color.transparent);
        editText.setTextColor(Color.parseColor("#333333"));
        editText.setTextSize(14);
        editText.setOnKeyListener(editTextKeyListener);
        editText.setOnFocusChangeListener(focusListener);

        // 调整EditText的外边距
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = DEFAULT_MARGING;
        lp.leftMargin = DEFAULT_MARGING;
        lp.rightMargin= DEFAULT_MARGING;
        editText.setLayoutParams(lp);

        return editText;
    }

    /**
     * 生成图片Layout
     */
    private RelativeLayout createImageLayout() {
        RelativeLayout.LayoutParams contentImageLp = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        DataImageView dataImageView = new DataImageView(getContext());
        dataImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        dataImageView.setLayoutParams(contentImageLp);

        RelativeLayout.LayoutParams closeImageLp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        closeImageLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        closeImageLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        closeImageLp.setMargins(0, dip2px(10), dip2px(10), 0);
        ImageView closeImage = new ImageView(getContext());
        closeImage.setScaleType(ImageView.ScaleType.FIT_XY);
        closeImage.setImageResource(R.mipmap.icon_delete);
        closeImage.setLayoutParams(closeImageLp);

        RelativeLayout layout = new RelativeLayout(getContext());
        layout.addView(dataImageView);
        layout.addView(closeImage);
        layout.setTag(viewTagID++);
        setFocusOnView(layout, true);

        closeImage.setTag(layout.getTag());
        closeImage.setOnClickListener(deleteListener);

        // 调整imageView的外边距
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, DEFAULT_IMAGE_HEIGHT);
        lp.bottomMargin = DEFAULT_MARGING;
        lp.leftMargin = DEFAULT_MARGING;
        lp.rightMargin= DEFAULT_MARGING;
        layout.setLayoutParams(lp);

        return layout;
    }

    private void setFocusOnView(View view, boolean isFocusable) {
        view.setClickable(isFocusable);
        view.setFocusable(isFocusable);
        view.setFocusableInTouchMode(isFocusable);
        if (isFocusable) {
            view.setOnFocusChangeListener(focusListener);
        } else {
            view.setOnFocusChangeListener(null);
        }
    }

    /**
     * 插入图片前，如果是在排序状态下，需要先切换到非排序状态
     */
    private void prepareAddImage() {
        if (isSort) { // 如果是排序模式，需要退出排序模式
            isSort = false;
            endSortUI();
            containerLayout.setLayoutTransition(mTransitioner);
        }
    }

    /**
     * 根据图片绝对路径集合批量添加一组图片
     * @param imageList
     */
    public void addImageList(List<String> imageList) {
        prepareAddImage();
        for (String imagePath : imageList) {
            Bitmap bmp = getScaledBitmap(imagePath, getWidth());
            insertImage(bmp, imagePath, true);
        }
    }

    /**
     * 根据图片绝对路径数组批量添加一组图片
     * @param imagePaths
     */
    public void addImageArray(String[] imagePaths) {
        prepareAddImage();
        for (String imagePath : imagePaths) {
            Bitmap bmp = getScaledBitmap(imagePath, getWidth());
            insertImage(bmp, imagePath, true);
        }
    }

    /**
     * 根据绝对路径添加一张图片
     *
     * @param imagePath
     */
    public void addImage(String imagePath) {
        prepareAddImage();
        Bitmap bmp = getScaledBitmap(imagePath, getWidth());
        insertImage(bmp, imagePath, false);
    }

    /**
     * 插入一张图片
     */
    private void insertImage(Bitmap bitmap, String imagePath, boolean isBatch) {
        String lastEditStr = lastFocusEdit.getText().toString();
        int cursorIndex = lastFocusEdit.getSelectionStart();
        String lastStr = lastEditStr.substring(0, cursorIndex).trim();
        int lastEditIndex = containerLayout.indexOfChild(lastFocusEdit);

        View firstView = containerLayout.getChildAt(0);
        if (containerLayout.getChildCount() == 1 && firstView == lastFocusEdit) {
            lastFocusEdit = (EditText) firstView;
            lastFocusEdit.setHint("");
        }

        if (lastEditStr.length() == 0 || lastStr.length() == 0) {
            // 如果EditText为空，或者光标已经顶在了editText的最前面，则直接插入图片，并且EditText下移即可
            insertImageViewAtIndex(lastEditIndex, bitmap, imagePath, isBatch);
        } else {
            // 如果EditText非空且光标不在最顶端，则需要添加新的imageView和EditText
            lastFocusEdit.setText(lastStr);
            String editStr2 = lastEditStr.substring(cursorIndex).trim();
            if (containerLayout.getChildCount() - 1 == lastEditIndex
                    || editStr2.length() > 0) {
                insertEditTextAtIndex(lastEditIndex + 1, editStr2);
            }

            insertImageViewAtIndex(lastEditIndex + 1, bitmap, imagePath, isBatch);
            lastFocusEdit.requestFocus();
            lastFocusEdit.setSelection(lastStr.length(), lastStr.length());
        }
        processSoftKeyBoard(false);
    }

    /**
     * 隐藏或者显示软键盘
     * @param isShow true:显示，false:隐藏
     */
    public void processSoftKeyBoard(boolean isShow) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (isShow) {
            imm.showSoftInput(lastFocusEdit, InputMethodManager.SHOW_FORCED);
        } else {
            imm.hideSoftInputFromWindow(lastFocusEdit.getWindowToken(), 0);
        }
    }

    /**
     * 在指定位置添加一个“将来用于编辑文字的图片”
     * @param index
     */
    private void insertEditTextImageView(int index) {
        ImageView ivInsertEditText = createInsertEditTextImageView();
        containerLayout.addView(ivInsertEditText, index);
    }

    /**
     * 在指定位置插入EditText
     *
     * @param index   位置
     * @param editStr EditText显示的文字
     */
    private EditText insertEditTextAtIndex(final int index, String editStr) {
        EditText editText = createEditText("");
        editText.setText(editStr);

        // 请注意此处，EditText添加、或删除不触动Transition动画
        containerLayout.setLayoutTransition(null);
        containerLayout.addView(editText, index);
        containerLayout.setLayoutTransition(mTransitioner); // add之后恢复transition动画
        return editText;
    }

    /**
     * 在指定位置添加ImageView
     */
    private void insertImageViewAtIndex(int index, Bitmap bmp, String imagePath, boolean isBatch) {
        if (index > 0) {
            View currChild = containerLayout.getChildAt(index);
            // 当前index位置的child是ImageView，则在插入本ImageView的时候，多插入一个图标，用于将来可以插入EditText
            if (currChild instanceof RelativeLayout) {
                insertEditTextImageView(index);
            }

            int lastIndex = index - 1;
            View child = containerLayout.getChildAt(lastIndex);
            // index位置的上一个child是ImageView，则在插入本ImageView的时候，多插入一个图标，用于将来可以插入EditText
            if (child instanceof RelativeLayout) {
                insertEditTextImageView(index++);
            }
        }

        final RelativeLayout imageLayout = createImageLayout();

        DataImageView imageView = (DataImageView) imageLayout.getChildAt(0);
        imageView.setImageBitmap(bmp);
        imageView.setBitmap(bmp);
        imageView.setAbsolutePath(imagePath);

        // onActivityResult无法触发动画，此处post处理
        final int finalIndex = index;
        if (isBatch) {
            containerLayout.addView(imageLayout, finalIndex);
        } else {
            containerLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    containerLayout.addView(imageLayout, finalIndex);
                }
            }, 200);
        }

    }

    /**
     * 根据view的宽度，动态缩放bitmap尺寸
     *
     * @param width view的宽度
     */
    private Bitmap getScaledBitmap(String filePath, int width) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int sampleSize = options.outWidth > width ? options.outWidth / width
                + 1 : 1;
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(filePath, options);
    }

    /**
     * 初始化transition动画
     */
    private void setupLayoutTransitions(LinearLayout containerLayout) {
        mTransitioner = new LayoutTransition();
        containerLayout.setLayoutTransition(mTransitioner);
        mTransitioner.setDuration(300);
    }

    /**
     * dp和pixel转换
     *
     * @param dipValue dp值
     * @return 像素值
     */
    private int dip2px(float dipValue) {
        float m = getContext().getResources().getDisplayMetrics().density;
        return (int) (dipValue * m + 0.5f);
    }

    private void prepareSortConfig() {
        indexArray.clear();

        int[] position = new int[2];
        SortRichEditor.this.getLocationOnScreen(position);

        SortRichEditor sortRichEditor = SortRichEditor.this;
        containerTopVal = position[1] + sortRichEditor.getPaddingTop() + SCROLL_OFFSET;
        containerBottomVal = containerTopVal + sortRichEditor.getHeight() - sortRichEditor.getPaddingBottom() - SCROLL_OFFSET;

    }

    /**
     * 对外提供的接口, 生成编辑数据上传
     */
    public List<SortRichEditorData> buildEditData() {
        List<SortRichEditorData> dataList = new ArrayList<>();
        int num = containerLayout.getChildCount();
        for (int index = 0; index < num; index++) {
            View itemView = containerLayout.getChildAt(index);
            SortRichEditorData itemData = new SortRichEditorData();
            if (itemView instanceof EditText) {
                EditText item = (EditText) itemView;
                itemData.setInputStr(item.getText().toString());
            } else if (itemView instanceof RelativeLayout) {
                DataImageView item = (DataImageView) ((RelativeLayout) itemView).getChildAt(0);
                itemData.setImagePath(item.getAbsolutePath());
                itemData.setBitmap(item.getBitmap());
            }
            dataList.add(itemData);
        }

        return dataList;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
//        processSoftKeyBoard(false);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (viewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    /**
     * 重新排列Child的位置，更新{@link #indexArray} 中view的下标顺序
     */
    private void resetChildPostion() {
        indexArray.clear();
        View child;
        int tagID, sortIndex;
        int childCount = containerLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            child = containerLayout.getChildAt(i);
            tagID = Integer.parseInt(child.getTag().toString());
            sortIndex = (preSortPositionArray.get(tagID) - DEFAULT_MARGING) / (SIZE_REDUCE_VIEW + DEFAULT_MARGING);
            indexArray.put(i, sortIndex);
        }
    }

    private class ViewDragHelperCallBack extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return (child instanceof RelativeLayout) && isSort;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final int leftBound = getPaddingLeft() + DEFAULT_MARGING;
            final int rightBound = getWidth() - child.getWidth() - leftBound;
            final int newLeft = Math.min(Math.max(left, leftBound), rightBound);
            return newLeft;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return top;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return 0;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return 0;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int releasedViewID = Integer.parseInt(releasedChild.getTag().toString());
            int releasedViewPos = preSortPositionArray.get(releasedViewID);
            viewDragHelper.settleCapturedViewAt(releasedChild.getLeft(), releasedViewPos);
            invalidate();
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {

        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            int reduceChildCount = containerLayout.getChildCount();

            View sortChild;
            int changeViewTagID, sortViewTagID, changeViewPosition, sortViewPosition;
            for (int i = 0; i < reduceChildCount; i++) {
                sortChild = containerLayout.getChildAt(i);
                if (sortChild != changedView) {
                    changeViewTagID = Integer.parseInt(changedView.getTag().toString());
                    sortViewTagID = Integer.parseInt(sortChild.getTag().toString());

                    changeViewPosition = preSortPositionArray.get(changeViewTagID);
                    sortViewPosition = preSortPositionArray.get(sortViewTagID);

                    if ((changedView.getTop() > sortChild.getTop() && changeViewPosition < sortViewPosition) ||
                            (changedView.getTop() < sortChild.getTop() && changeViewPosition > sortViewPosition)) {

                        sortChild.setTop(changeViewPosition);
                        sortChild.setBottom(changeViewPosition + SIZE_REDUCE_VIEW);

                        preSortPositionArray.put(sortViewTagID, changeViewPosition);
                        preSortPositionArray.put(changeViewTagID, sortViewPosition);

                        resetChildPostion();
                        break;
                    }
                }
            }
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
        }

    }
}
