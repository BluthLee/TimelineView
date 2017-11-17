package com.bluthlee.timelineview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LC on 2017/11/13.
 */

public class TimelineView extends ViewGroup {

    public static final int VERTICAL = 0, HORIZONTAL = 1;

    private List<Integer> pointPositionY;

    /**
     * 时间线与边缘距离
     */
    private int lineLeftMargin = dp2px(20);
    private int lineRightMargin = dp2px(20);

    private int circleRadius = dp2px(5);
    private int lineStrokeWidth = dp2px(2);
    private int circleColor;
    private int lineColor;

    private Paint circlePaint;
    private Paint linePaint;
    private int orientation = VERTICAL;

    private int lastX;
    private int lastY;
    private OverScroller overScroller;
    private VelocityTracker velocityTracker;

    public TimelineView(Context context) {
        this(context, null);
    }

    public TimelineView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TimelineView);
        lineLeftMargin = (int) typedArray.getDimension(R.styleable.TimelineView_line_left_margin, lineLeftMargin);
        lineRightMargin = (int) typedArray.getDimension(R.styleable.TimelineView_line_right_margin, lineRightMargin);
        circleRadius = (int) typedArray.getDimension(R.styleable.TimelineView_circle_radius, circleRadius);
        lineStrokeWidth = (int) typedArray.getDimension(R.styleable.TimelineView_line_stroke_width, lineStrokeWidth);
        circleColor = typedArray.getColor(R.styleable.TimelineView_circle_color, 0xff000000);
        lineColor = typedArray.getColor(R.styleable.TimelineView_line_color, 0xff000000);
        typedArray.recycle();

        init();
    }

    private void init() {
        setWillNotDraw(false);
        pointPositionY = new ArrayList<>();
        circlePaint = new Paint();
        circlePaint.setColor(circleColor);
        circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        linePaint = new Paint();
        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(lineStrokeWidth);

        overScroller = new OverScroller(getContext());
        velocityTracker = VelocityTracker.obtain();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = 0;
        int measuredHeight = 0;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        //测量所有子view的尺寸
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        if (getChildCount() == 0) {
            setMeasuredDimension(measuredWidth, measuredHeight);
            return;
        }

        if (orientation == VERTICAL) {
            //计算width
            switch (widthMode) {
                case MeasureSpec.EXACTLY:
                    measuredWidth = widthSize;
                    break;
                case MeasureSpec.AT_MOST:

                case MeasureSpec.UNSPECIFIED:
                    measuredWidth = getWidthFromChildren();
                    break;
            }

            //计算height
            switch (heightMode) {
                case MeasureSpec.EXACTLY:
                    measuredHeight = heightSize;
                    break;
                case MeasureSpec.AT_MOST:
                    int childrenHeight = getHeightFromChildren();
                    measuredHeight = heightSize < childrenHeight ? heightSize : childrenHeight;
                    break;
                case MeasureSpec.UNSPECIFIED:
                    measuredHeight = getHeightFromChildren();
                    break;
            }
        }

        setMeasuredDimension(measuredWidth, measuredHeight);

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = lineLeftMargin + lineRightMargin + Math.max(circleRadius * 2, lineStrokeWidth);
        int heightLength = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                child.layout(left, heightLength, left + child.getMeasuredWidth(), heightLength + child.getMeasuredHeight());
                heightLength += child.getMeasuredHeight();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setPointPositionY();
        drawLines(canvas);
        drawPoints(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                //如果在滑动则中断滑动
                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                scrollBy(0, lastY - y);
                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_UP:
                velocityTracker.addMovement();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:

                break;
            case MotionEvent.ACTION_POINTER_UP:

                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
    }

    private int getWidthFromChildren() {
        int width;
        int largestChildWidth = getChildAt(0).getMeasuredWidth();
        for (int i = 1; i < getChildCount(); i++) {
            if (getChildAt(i).getMeasuredWidth() > largestChildWidth) {
                largestChildWidth = getChildAt(i).getMeasuredWidth();
            }
        }
        width = Math.max(circleRadius * 2, lineStrokeWidth) + largestChildWidth +
                lineLeftMargin + lineRightMargin;
        return width;
    }

    private int getHeightFromChildren() {
        int height = 0;
        for (int i = 0; i < getChildCount(); i++) {
            height += getChildAt(i).getMeasuredHeight();
        }
        return height;
    }

    private void drawLines(Canvas canvas) {
        for (int i = 0; i < pointPositionY.size() - 1; i++) {
            canvas.drawLine(lineLeftMargin, pointPositionY.get(i), lineLeftMargin, pointPositionY.get(i + 1), linePaint);
        }
    }

    private void setPointPositionY() {
        pointPositionY.clear();
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            pointPositionY.add((int) (view.getY() + view.getHeight() / 2));
        }
    }

    private void drawPoints(Canvas canvas) {
        for (int i = 0; i < pointPositionY.size(); i++) {
            canvas.drawCircle(lineLeftMargin, pointPositionY.get(i), circleRadius, circlePaint);
        }
    }


    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
