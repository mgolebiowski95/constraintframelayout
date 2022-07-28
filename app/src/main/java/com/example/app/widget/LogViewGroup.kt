package com.example.app.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.system.measureTimeMillis

class LogViewGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val executionTime = measureTimeMillis {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
        val resourceEntryName = resources.getResourceEntryName(id)
        println(" \n $resourceEntryName.onMeasure ${executionTime}ms")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val executionTime = measureTimeMillis {
            super.onLayout(changed, left, top, right, bottom)
        }
        val resourceEntryName = resources.getResourceEntryName(id)
        println(" \n $resourceEntryName.onLayout ${executionTime}ms")
    }
}
