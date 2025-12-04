package com.example.kidsdrawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var drawingPath: CustomPath? = null
    private var paths = ArrayList<CustomPath>()
    private var undoPaths = ArrayList<CustomPath>()
    private var drawingPaint: Paint? = null
    private var canvasPaint: Paint? = null
    private var canvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null
    private var brushSize: Float = 0.toFloat()
    private var color = Color.BLACK

    init {
        setUpDrawing()
    }

    private fun setUpDrawing() {
        drawingPaint = Paint()
        drawingPath = CustomPath(color, brushSize)
        drawingPaint!!.color = color
        drawingPaint!!.style = Paint.Style.STROKE
        drawingPaint!!.strokeJoin = Paint.Join.ROUND
        drawingPaint!!.strokeCap = Paint.Cap.ROUND
        canvasPaint = Paint(Paint.DITHER_FLAG)

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)

        for(p in paths){
            drawingPaint!!.strokeWidth = p.brushThickness
            drawingPaint!!.color = p.color
            canvas.drawPath(p, drawingPaint!!)
        }

        if (!drawingPath!!.isEmpty) {
            drawingPaint!!.strokeWidth = drawingPath!!.brushThickness
            drawingPaint!!.color = drawingPath!!.color
            canvas.drawPath(drawingPath!!, drawingPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y

        when(event?.action) {
            MotionEvent.ACTION_DOWN -> {
                drawingPath!!.color = color
                drawingPath!!.brushThickness = brushSize

                drawingPath!!.reset()
                drawingPath!!.moveTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_MOVE ->{
                drawingPath!!.lineTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_UP ->{
                paths.add(drawingPath!!)
                drawingPath = CustomPath(color, brushSize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setSizeForBrush(newSize: Float){
        brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize, resources.displayMetrics)
        drawingPaint!!.strokeWidth = brushSize
    }

    fun setColor(newColor: String){
        color = Color.parseColor(newColor)
        drawingPaint!!.color = color
    }

    fun undoPath(){
        if(paths.isNotEmpty()){
            undoPaths.add(paths.removeAt(paths.size - 1))
            invalidate()
        }
    }

    fun redoPath(){
        if(undoPaths.isNotEmpty()){
            paths.add(undoPaths.removeAt(undoPaths.size - 1))
            invalidate()
        }
    }
    fun erasePath(){
        paths.clear()
        undoPaths.clear()
        drawingPath!!.reset()
        invalidate()
    }


    class CustomPath(var color: Int, var brushThickness: Float): android.graphics.Path() {

    }

}