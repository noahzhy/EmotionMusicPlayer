package kr.ac.hanyang.emotionmusic

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView


class RoundImageView : ImageView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {

        // 获取当前控件的 drawable
        val drawable = drawable ?: return

        // get 回来的宽度和高度是当前控件相对应的宽度和高度（在 xml 设置）
        if (width == 0 || height == 0) {
            return
        }

        // 画笔
        val paint = Paint()
        // 颜色设置
        paint.color = -0xbdbdbe
        // 抗锯齿
        paint.isAntiAlias = true
        //Paint 的 Xfermode，PorterDuff.Mode.SRC_IN 取两层图像的交集部门, 只显示上层图像。
        val xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        // 获取 bitmap，即传入 imageview 的 bitmap
        var bitmap = (drawable as BitmapDrawable).bitmap

        // 标志
        val saveFlags = Canvas.MATRIX_SAVE_FLAG or Canvas.CLIP_SAVE_FLAG or Canvas.HAS_ALPHA_LAYER_SAVE_FLAG or Canvas.FULL_COLOR_LAYER_SAVE_FLAG or Canvas.CLIP_TO_LAYER_SAVE_FLAG
        canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null, saveFlags)

        // 画遮罩，画出来就是一个和空间大小相匹配的圆
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), (width / 2).toFloat(), paint)
        paint.xfermode = xfermode

        // 空间的大小 /bitmap 的大小 =bitmap 缩放的倍数
        val scaleWidth = width.toFloat() / bitmap.width
        val scaleHeight = height.toFloat() / bitmap.height

        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)

        //bitmap 缩放
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        //draw 上去
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        //		paint.setXfermode(null);
        canvas.restore()

    }

}