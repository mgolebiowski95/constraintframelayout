package com.mgsoftware.constraintframelayout.helper.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.mgsoftware.constraintframelayout.R

open class VirtualLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var referenceIds: String? = null

    init {
        val a =
            context.obtainStyledAttributes(attrs, R.styleable.VirtualLayout, defStyleAttr, 0)
        if (a.hasValue(R.styleable.VirtualLayout_referenceIds))
            referenceIds = a.getString(R.styleable.VirtualLayout_referenceIds)
        a.recycle()
    }

    @SuppressLint("MissingSuperCall")
    override fun draw(canvas: Canvas?) {
    }

    override fun onDraw(canvas: Canvas?) {
    }

    fun getReferencesIds(): List<Int> {
        val referenceIds = referenceIds
        return referenceIds?.split(',')?.map {
            getResourceId(context, it)
        } ?: emptyList()
    }

    fun getReferencesViews(): List<View> {
        val referenceIds = referenceIds
        return referenceIds?.split(',')?.map {
            val resourceId = getResourceId(context, it)
            (parent as ViewGroup).findViewById(resourceId)
        } ?: emptyList()
    }

    private fun getResourceId(context: Context, name: String): Int {
        return context.resources.getIdentifier(name, "id", context.packageName)
    }
}