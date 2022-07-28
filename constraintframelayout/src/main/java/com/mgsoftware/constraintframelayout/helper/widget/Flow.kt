package com.mgsoftware.constraintframelayout.helper.widget

import android.content.Context
import android.util.AttributeSet
import com.mgsoftware.constraintframelayout.R
import com.mgsoftware.constraintframelayout.widget.ConstraintFrameLayout

/**
 * referenceIds
 * Flow_orientation
 */
class Flow @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : VirtualLayout(context, attrs, defStyleAttr) {
    var orientation = ConstraintFrameLayout.LayoutParams.HORIZONTAL

    init {
        val a =
            context.obtainStyledAttributes(attrs, R.styleable.Flow, defStyleAttr, 0)
        if(a.hasValue(R.styleable.Flow_Flow_orientation))
            orientation = a.getInt(R.styleable.Flow_Flow_orientation, orientation)
        a.recycle()
    }
}