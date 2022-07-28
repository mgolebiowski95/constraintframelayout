package com.example.app.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View

class LogView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val resourceEntryName = resources.getResourceEntryName(id)
        println(" \n LogView.onMeasure $resourceEntryName")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val resourceEntryName = resources.getResourceEntryName(id)
        println(" \n LogView.onLayout $resourceEntryName")
    }

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        val resourceEntryName = resources.getResourceEntryName(id)
        super.layout(l, t, r, b)
        println(" \n LogView.layout $resourceEntryName")
    }
}
