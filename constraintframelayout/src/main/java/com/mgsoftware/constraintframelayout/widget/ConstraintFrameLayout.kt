package com.mgsoftware.constraintframelayout.widget

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.math.MathUtils
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import com.mgsoftware.constraintframelayout.Graph
import com.mgsoftware.constraintframelayout.R
import com.mgsoftware.constraintframelayout.helper.widget.Flow
import com.mgsoftware.constraintframelayout.helper.widget.Group
import com.mgsoftware.constraintframelayout.helper.widget.VirtualLayout
import com.mgsoftware.constraintframelayout.widget.ConstraintFrameLayout.LayoutParams.Companion.HORIZONTAL
import com.mgsoftware.constraintframelayout.widget.ConstraintFrameLayout.LayoutParams.Companion.UNSET
import com.mgsoftware.constraintframelayout.widget.ConstraintFrameLayout.LayoutParams.Companion.VERTICAL
import org.mini2Dx.gdx.math.Rectangle
import org.mini2Dx.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.max

class ConstraintFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val graph = Graph()
    private val children = mutableListOf<View>()
    private val childrenNotMeasuredByHorizontalWeightConstraint = mutableListOf<View>()
    private val childrenNotMeasuredByVerticalWeightConstraint = mutableListOf<View>()

    // for circle constraint
    private val position = Vector2()
    private val viewBounds = Rectangle()
    private val viewCenter = Vector2()

    override fun onFinishInflate() {
        super.onFinishInflate()
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            when (child) {
                is Group -> {
                    assignAttributes(child)
                }
                is Flow -> {
                    assignAttributes(child)
                }
            }
        }
    }

    private fun assignAttributes(group: Group) {
        if (group.id == View.NO_ID)
            group.id = generateViewId()

        group.getReferencesViews().forEach {
            val lp = it.layoutParams as LayoutParams
            lp.virtualParent = group.id
        }
    }

    private fun assignAttributes(flow: Flow) {
        if (flow.id == View.NO_ID)
            flow.id = generateViewId()
        val parentId = flow.id

        val children = flow.getReferencesViews()
        if (children.isEmpty()) return
        children.forEach {
            val lp = it.layoutParams as LayoutParams
            lp.virtualParent = parentId
        }

        val iterator = children.iterator()
        var current = iterator.next()
        val lp = current.layoutParams as LayoutParams
        when (flow.orientation) {
            HORIZONTAL -> {
                lp.leftToLeft = parentId
                lp.horizontalBias = 0f
                lp.topToTop = parentId
                lp.bottomToBottom = parentId
            }
            VERTICAL -> {
                lp.topToTop = parentId
                lp.verticalBias = 0f
                lp.leftToLeft = parentId
                lp.rightToRight = parentId
            }
        }
        while (iterator.hasNext()) {
            val next = iterator.next()
            val nextLp = next.layoutParams as LayoutParams
            when (flow.orientation) {
                HORIZONTAL -> {
                    nextLp.leftToRight = current.id
                    nextLp.horizontalBias = 0f
                    nextLp.topToTop = parentId
                    nextLp.bottomToBottom = parentId
                }
                VERTICAL -> {
                    nextLp.topToBottom = current.id
                    nextLp.verticalBias = 0f
                    nextLp.leftToLeft = parentId
                    nextLp.rightToRight = parentId
                }
            }
            current = next
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        sortForMeasurePass(children)

        val measureWidthMode = MeasureSpec.getMode(widthMeasureSpec)
        val measureHeightMode = MeasureSpec.getMode(heightMeasureSpec)
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (debug)
            println(
                "$TAG.onMeasure widthMeasureSpec = [${measureModeToString(measureWidthMode)} $measuredWidth], heightMeasureSpec = [${
                    measureModeToString(
                        measureHeightMode
                    )
                } $measuredHeight]"
            )

        var horizontalWeightSum = 0f
        var verticalWeightSum = 0f

        for (index in 0 until children.size) {
            val child = children[index]
            val lp = child.layoutParams as LayoutParams
            if (lp.width == 0 && lp.horizontalWeight != UNSET)
                horizontalWeightSum += lp.horizontalWeight
            if (lp.height == 0 && lp.verticalWeight != UNSET)
                verticalWeightSum += lp.verticalWeight
        }

        var measuredWidthAtMost = 0
        var measuredHeightAtMost = 0
        for (index in 0 until children.size) {
            val child = children[index]
            val lp = child.layoutParams as LayoutParams

            var childWidthMeasureSpec = when (lp.width) {
                0 -> {
                    val availableWidth = if (lp.virtualParent != View.NO_ID) {
                        val view = findViewById<View>(lp.virtualParent)
                        view.measuredWidth - (view.paddingLeft + paddingRight)
                    } else {
                        measuredWidth - (paddingLeft + paddingRight)
                    }
                    val width: Float = when {
                        lp.widthPercent != UNSET -> lp.widthPercent * availableWidth
                        lp.horizontalWeight != UNSET -> {
                            if (lp.virtualParent != View.NO_ID) {
                                val view = findViewById<View>(lp.virtualParent)
                                if (view is VirtualLayout) {
                                    val (viewsMeasuredByWeight, viewsNotMeasuredByWeight) = view.getReferencesViews()
                                        .partition {
                                            val lp = it.layoutParams as LayoutParams
                                            lp.width == 0 && lp.horizontalWeight != UNSET
                                        }
                                    val horizontalWeightSum = viewsMeasuredByWeight
                                        .map {
                                            (it.layoutParams as LayoutParams).horizontalWeight
                                        }
                                        .sum()
                                    val widthOfViewsNotMeasuredByWeight =
                                        viewsNotMeasuredByWeight.sumOf {
                                            it.measuredWidth
                                        }
                                    (lp.horizontalWeight / horizontalWeightSum) * (availableWidth - widthOfViewsNotMeasuredByWeight)
                                } else {
                                    0f
                                }
                            } else {
                                val finalAvailableWidth =
                                    max(
                                        availableWidth - childrenNotMeasuredByHorizontalWeightConstraint.sumOf { it.measuredWidth },
                                        0
                                    )
                                (lp.horizontalWeight / horizontalWeightSum) * finalAvailableWidth
                            }
                        }
                        lp.widthToWidth != View.NO_ID -> {
                            val view = findViewById<View?>(lp.widthToWidth)
                            view?.measuredWidth?.toFloat() ?: 0f
                        }
                        lp.widthToHeight != View.NO_ID -> {
                            val view = findViewById<View?>(lp.widthToHeight)
                            view?.measuredHeight?.toFloat() ?: 0f
                        }
                        else -> {
                            0f
                        }
                    }
                    val finalWidth = width.toInt()
                    MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY)
                }
                ViewGroup.LayoutParams.WRAP_CONTENT -> {
                    when (child) {
                        is Group -> {
                            val finalWidth = child.getReferencesViews().maxOf {
                                val lp = it.layoutParams as LayoutParams
                                it.measuredWidth + lp.leftMargin + lp.rightMargin
                            }
                            MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY)
                        }
                        is Flow -> {
                            val finalWidth = when (child.orientation) {
                                HORIZONTAL -> child.getReferencesViews().sumOf {
                                    val lp = it.layoutParams as LayoutParams
                                    it.measuredWidth + lp.leftMargin + lp.rightMargin
                                }
                                VERTICAL -> child.getReferencesViews()
                                    .maxOf { it.measuredWidth } + lp.leftMargin + lp.rightMargin
                                else -> 0
                            }
                            MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY)
                        }
                        else -> {
                            ViewGroup.getChildMeasureSpec(
                                widthMeasureSpec,
                                paddingLeft + paddingRight,
                                lp.width
                            )
                        }
                    }
                }
                else -> {
                    ViewGroup.getChildMeasureSpec(
                        widthMeasureSpec,
                        paddingLeft + paddingRight,
                        lp.width
                    )
                }
            }

            var childHeightMeasureSpec = when (lp.height) {
                0 -> {
                    val availableHeight = if (lp.virtualParent != View.NO_ID) {
                        val view = findViewById<View>(lp.virtualParent)
                        view.measuredHeight - (view.paddingTop + paddingBottom)
                    } else {
                        measuredHeight - (paddingTop + paddingBottom)
                    }
                    val height: Float = when {
                        lp.heightPercent != UNSET -> lp.heightPercent * availableHeight
                        lp.verticalWeight != UNSET -> {
                            if (lp.virtualParent != View.NO_ID) {
                                val view = findViewById<View>(lp.virtualParent)
                                if (view is VirtualLayout) {
                                    val (viewsMeasuredByWeight, viewsNotMeasuredByWeight) = view.getReferencesViews()
                                        .partition {
                                            val lp = it.layoutParams as LayoutParams
                                            lp.height == 0 && lp.verticalWeight != UNSET
                                        }
                                    val verticalWeightSum = viewsMeasuredByWeight
                                        .map {
                                            (it.layoutParams as LayoutParams).verticalWeight
                                        }
                                        .sum()
                                    val heightOfViewsNotMeasuredByWeight =
                                        viewsNotMeasuredByWeight.sumOf {
                                            it.measuredHeight
                                        }
                                    (lp.verticalWeight / verticalWeightSum) * (availableHeight - heightOfViewsNotMeasuredByWeight)
                                } else {
                                    0f
                                }
                            } else {
                                val finalAvailableHeight =
                                    max(
                                        availableHeight - childrenNotMeasuredByVerticalWeightConstraint.sumOf { it.measuredHeight },
                                        0
                                    )
                                (lp.verticalWeight / verticalWeightSum) * finalAvailableHeight
                            }
                        }
                        lp.heightToHeight != View.NO_ID -> {
                            val view = findViewById<View?>(lp.heightToHeight)
                            view?.measuredHeight?.toFloat() ?: 0f
                        }
                        lp.heightToWidth != View.NO_ID -> {
                            val view = findViewById<View?>(lp.heightToWidth)
                            view?.measuredWidth?.toFloat() ?: 0f
                        }
                        else -> {
                            0f
                        }
                    }
                    val finalHeight = height.toInt()
                    MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY)
                }
                ViewGroup.LayoutParams.WRAP_CONTENT -> {
                    when (child) {
                        is Group -> {
                            val finalHeight = child.getReferencesViews().maxOf {
                                val lp = it.layoutParams as LayoutParams
                                it.measuredHeight + lp.topMargin + lp.bottomMargin
                            }
                            MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY)
                        }
                        is Flow -> {
                            val finalHeight = when (child.orientation) {
                                HORIZONTAL -> child.getReferencesViews().maxOf {
                                    it.measuredHeight
                                } + lp.topMargin + lp.bottomMargin
                                VERTICAL -> child.getReferencesViews().sumOf {
                                    val lp = it.layoutParams as LayoutParams
                                    it.measuredHeight + lp.topMargin + lp.bottomMargin
                                }
                                else -> 0
                            }
                            MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY)
                        }
                        else -> {
                            ViewGroup.getChildMeasureSpec(
                                heightMeasureSpec,
                                paddingTop + paddingBottom,
                                lp.height
                            )
                        }
                    }
                }
                else -> {
                    ViewGroup.getChildMeasureSpec(
                        heightMeasureSpec,
                        paddingTop + paddingBottom,
                        lp.height
                    )
                }
            }

            if (lp.dimensionRatioValue != -1f) {
                val childMeasuredWidthMode = MeasureSpec.getMode(childWidthMeasureSpec)
                val childMeasuredHeightMode = MeasureSpec.getMode(childHeightMeasureSpec)
                val childMeasuredWidth = MeasureSpec.getSize(childWidthMeasureSpec)
                val childMeasuredHeight = MeasureSpec.getSize(childHeightMeasureSpec)

                val boundedWidth = childMeasuredWidth.toFloat()
                val boundedHeight = childMeasuredHeight.toFloat()

                if (childMeasuredWidthMode == MeasureSpec.EXACTLY && childMeasuredHeightMode == MeasureSpec.EXACTLY) {
                    var finalWidth = boundedWidth
                    var finalHeight: Float

                    finalHeight = finalWidth / lp.dimensionRatioValue
                    if (finalHeight > boundedHeight) {
                        finalHeight = boundedHeight
                        finalWidth = finalHeight * lp.dimensionRatioValue
                    }

                    childWidthMeasureSpec =
                        MeasureSpec.makeMeasureSpec(finalWidth.toInt(), MeasureSpec.EXACTLY)
                    childHeightMeasureSpec =
                        MeasureSpec.makeMeasureSpec(finalHeight.toInt(), MeasureSpec.EXACTLY)
                }
            }
            if (lp.widthFactor != 1f || lp.heightFactor != 1f) {
                val childMeasuredWidthMode = MeasureSpec.getMode(childWidthMeasureSpec)
                val childMeasuredHeightMode = MeasureSpec.getMode(childHeightMeasureSpec)
                val childMeasuredWidth = MeasureSpec.getSize(childWidthMeasureSpec)
                val childMeasuredHeight = MeasureSpec.getSize(childHeightMeasureSpec)

                val finalWidth = childMeasuredWidth * lp.widthFactor
                val finalHeight = childMeasuredHeight * lp.heightFactor

                if (childMeasuredWidthMode == MeasureSpec.EXACTLY)
                    childWidthMeasureSpec =
                        MeasureSpec.makeMeasureSpec(finalWidth.toInt(), MeasureSpec.EXACTLY)

                if (childMeasuredHeightMode == MeasureSpec.EXACTLY)
                    childHeightMeasureSpec =
                        MeasureSpec.makeMeasureSpec(finalHeight.toInt(), MeasureSpec.EXACTLY)
            }

            if (debug) {
                val resourceName = getResourceName(child, "unknown")
                val childMeasureWidthMode = MeasureSpec.getMode(childWidthMeasureSpec)
                val childMeasureHeightMode = MeasureSpec.getMode(childHeightMeasureSpec)
                val childMeasuredWidth = MeasureSpec.getSize(childWidthMeasureSpec)
                val childMeasuredHeight = MeasureSpec.getSize(childHeightMeasureSpec)
                println(
                    "$TAG.onMeasure: resourceName=$resourceName, childMeasuredWidth=$childMeasuredWidth,childMeasuredHeight=$childMeasuredHeight, childWidthMeasureSpec = [${
                        measureModeToString(
                            childMeasureWidthMode
                        )
                    }, childHeightMeasureSpec = [${measureModeToString(childMeasureHeightMode)}"
                )
            }
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            val childMeasuredWidth = child.measuredWidth
            if (childMeasuredWidth > measuredWidthAtMost)
                measuredWidthAtMost = childMeasuredWidth
            val childMeasuredHeight = child.measuredHeight
            if (childMeasuredHeight > measuredHeightAtMost)
                measuredHeightAtMost = childMeasuredHeight
        }

        val finalMeasuredWidth: Int = when (measureWidthMode) {
            MeasureSpec.UNSPECIFIED -> measuredWidth
            MeasureSpec.EXACTLY -> measuredWidth
            MeasureSpec.AT_MOST -> measuredWidthAtMost
            else -> measuredWidth
        }
        val finalMeasuredHeight: Int = when (measureHeightMode) {
            MeasureSpec.UNSPECIFIED -> measuredHeight
            MeasureSpec.EXACTLY -> measuredHeight
            MeasureSpec.AT_MOST -> measuredHeightAtMost
            else -> measuredHeight
        }
        if (debug)
            println("$TAG.onMeasure: finalMeasuredWidth=$finalMeasuredWidth, finalMeasuredHeight=$finalMeasuredHeight")
        setMeasuredDimension(finalMeasuredWidth, finalMeasuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        sortForLayoutPass(children)

        for (index in 0 until children.size) {
            val child = children[index]
            val lp = child.layoutParams as LayoutParams

            if (lp.circleConstraint != View.NO_ID) {
                val view = findViewById<View?>(lp.circleConstraint)
                if (view != null) {
                    viewBounds.set(
                        view.left.toFloat(),
                        view.top.toFloat(),
                        view.measuredWidth.toFloat(),
                        view.measuredHeight.toFloat()
                    )
                    viewBounds.getCenter(viewCenter)

                    val circleAngle = lp.circleAngle
                    val circleRadius = lp.circleRadius
                    val childMeasuredWidth = child.measuredWidth
                    val childMeasuredHeight = child.measuredHeight
                    position.set(Vector2.X).setAngleDeg(circleAngle).scl(circleRadius)
                        .add(
                            viewCenter.x - childMeasuredWidth / 2f,
                            viewCenter.y - childMeasuredHeight / 2f
                        )
                    val cLeft = position.x.toInt()
                    val cTop = position.y.toInt()
                    child.layout(
                        cLeft,
                        cTop,
                        cLeft + childMeasuredWidth,
                        cTop + childMeasuredHeight
                    )
                }
            } else {
                val guidelineLeft: Float = when {
                    lp.guidePercent != UNSET && lp.orientation == HORIZONTAL -> {
                        measuredWidth * lp.guidePercent
                    }
                    lp.guideBegin != UNSET && lp.orientation == HORIZONTAL -> {
                        0f + lp.guideBegin
                    }
                    lp.guideEnd != UNSET && lp.orientation == HORIZONTAL -> {
                        measuredWidth - lp.guideEnd
                    }
                    lp.leftToLeft != View.NO_ID -> {
                        val view = findViewById<View?>(lp.leftToLeft)
                        view?.left?.toFloat()?.plus(lp.leftMargin) ?: 0f
                    }
                    lp.leftToRight != View.NO_ID -> {
                        val view = findViewById<View?>(lp.leftToRight)
                        view?.right?.toFloat()?.plus(lp.leftMargin) ?: measuredWidth.toFloat()
                    }
                    lp.centerToCenter != View.NO_ID -> {
                        val view = findViewById<View?>(lp.centerToCenter)
                        view?.left?.toFloat()?.plus(lp.leftMargin) ?: 0f
                    }
                    else -> {
                        if (lp.virtualParent != View.NO_ID) {
                            val parent = findViewById<View>(lp.virtualParent)
                            (parent.left + parent.marginLeft + parent.paddingLeft).toFloat()
                        } else {
                            0f + lp.leftMargin + paddingLeft
                        }
                    }
                }
                val guidelineRight: Float = when {
                    lp.guidePercent != UNSET && lp.orientation == HORIZONTAL -> {
                        measuredWidth * lp.guidePercent
                    }
                    lp.guideBegin != UNSET && lp.orientation == HORIZONTAL -> {
                        0f + lp.guideBegin
                    }
                    lp.guideEnd != UNSET && lp.orientation == HORIZONTAL -> {
                        measuredWidth - lp.guideEnd
                    }
                    lp.rightToRight != View.NO_ID -> {
                        val view = findViewById<View?>(lp.rightToRight)
                        view?.right?.toFloat()?.minus(lp.rightMargin) ?: measuredWidth.toFloat()
                    }
                    lp.rightToLeft != View.NO_ID -> {
                        val view = findViewById<View?>(lp.rightToLeft)
                        view?.left?.toFloat()?.minus(lp.rightMargin) ?: 0f
                    }
                    lp.centerToCenter != View.NO_ID -> {
                        val view = findViewById<View?>(lp.centerToCenter)
                        view?.right?.toFloat()?.minus(lp.rightMargin) ?: measuredWidth.toFloat()
                    }
                    else -> {
                        if (lp.virtualParent != View.NO_ID) {
                            val parent = findViewById<View>(lp.virtualParent)
                            (parent.right + parent.marginRight + parent.paddingRight).toFloat()
                        } else {
                            measuredWidth.toFloat() - lp.rightMargin - paddingRight
                        }
                    }
                }
                val guidelineTop: Float = when {
                    lp.guidePercent != UNSET && lp.orientation == VERTICAL -> {
                        measuredHeight * lp.guidePercent
                    }
                    lp.guideBegin != UNSET && lp.orientation == VERTICAL -> {
                        0f + lp.guideBegin
                    }
                    lp.guideEnd != UNSET && lp.orientation == VERTICAL -> {
                        measuredHeight - lp.guideEnd
                    }
                    lp.topToTop != View.NO_ID -> {
                        val view = findViewById<View?>(lp.topToTop)
                        view?.top?.toFloat()?.plus(lp.topMargin) ?: 0f
                    }
                    lp.topToBottom != View.NO_ID -> {
                        val view = findViewById<View?>(lp.topToBottom)
                        view?.bottom?.toFloat()?.plus(lp.topMargin) ?: measuredHeight.toFloat()
                    }
                    lp.centerToCenter != View.NO_ID -> {
                        val view = findViewById<View?>(lp.centerToCenter)
                        view?.top?.toFloat()?.plus(lp.bottomMargin) ?: 0f
                    }
                    else -> {
                        if (lp.virtualParent != View.NO_ID) {
                            val parent = findViewById<View>(lp.virtualParent)
                            (parent.top + parent.marginTop + paddingTop).toFloat()
                        } else {
                            0f + lp.topMargin + paddingTop
                        }
                    }
                }
                val guidelineBottom: Float = when {
                    lp.guidePercent != UNSET && lp.orientation == VERTICAL -> {
                        measuredHeight * lp.guidePercent
                    }
                    lp.guideBegin != UNSET && lp.orientation == VERTICAL -> {
                        0f + lp.guideBegin
                    }
                    lp.guideEnd != UNSET && lp.orientation == VERTICAL -> {
                        measuredHeight - lp.guideEnd
                    }
                    lp.bottomToBottom != View.NO_ID -> {
                        val view = findViewById<View?>(lp.bottomToBottom)
                        view?.bottom?.toFloat()?.minus(lp.bottomMargin) ?: measuredHeight.toFloat()
                    }
                    lp.bottomToTop != View.NO_ID -> {
                        val view = findViewById<View?>(lp.bottomToTop)
                        view?.top?.toFloat()?.minus(lp.bottomMargin) ?: 0f
                    }
                    lp.centerToCenter != View.NO_ID -> {
                        val view = findViewById<View?>(lp.centerToCenter)
                        view?.bottom?.toFloat()?.minus(lp.topMargin) ?: measuredHeight.toFloat()
                    }
                    else -> {
                        if (lp.virtualParent != View.NO_ID) {
                            val parent = findViewById<View>(lp.virtualParent)
                            (parent.bottom + parent.marginBottom + paddingBottom).toFloat()
                        } else {
                            measuredHeight.toFloat() - lp.bottomMargin - paddingBottom
                        }
                    }
                }

                val cLeft =
                    ((lp.horizontalBias * ((guidelineRight - guidelineLeft) - child.measuredWidth)) + guidelineLeft).toInt()
                val cTop =
                    ((lp.verticalBias * ((guidelineBottom - guidelineTop) - child.measuredHeight)) + guidelineTop).toInt()
                if (debug) {
                    val resourceName = getResourceName(child, "unknown")
                    println("$TAG.onLayout: resourceName=$resourceName, guidelineLeft=$guidelineLeft, guidelineRight=$guidelineRight, guidelineTop=$guidelineTop, guidelineBottom=$guidelineBottom")
                    println("$TAG.onLayout: resourceName=$resourceName, cLeft=$cLeft, cTop=$cTop")
                }
                child.layout(cLeft, cTop, cLeft + child.measuredWidth, cTop + child.measuredHeight)
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun sortForMeasurePass(output: MutableList<View>) {
        if (debug)
            Log.d("echo", "sortForMeasurePass")

        output.clear()
        graph.reset(childCount)

        childrenNotMeasuredByHorizontalWeightConstraint.clear()
        childrenNotMeasuredByVerticalWeightConstraint.clear()
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val lp = child.layoutParams as LayoutParams
            if (child !is VirtualLayout) {
                if (lp.horizontalWeight == UNSET)
                    childrenNotMeasuredByHorizontalWeightConstraint.add(child)
                if (lp.verticalWeight == UNSET)
                    childrenNotMeasuredByVerticalWeightConstraint.add(child)
            }
        }

        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val lp = child.layoutParams as LayoutParams

            if (lp.horizontalWeight != UNSET) {
                childrenNotMeasuredByHorizontalWeightConstraint.forEach { child ->
                    val lp = child.layoutParams
                    if (child is VirtualLayout) {
                        if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                            child.getReferencesViews().forEach { virtualChild ->
                                addDependency(indexOfChild(child), indexOfChild(virtualChild))
                            }
                        } else {
                            child.getReferencesViews().forEach { virtualChild ->
                                addDependency(indexOfChild(virtualChild), indexOfChild(child))
                            }
                        }
                    }
                }
            }

            if (lp.verticalWeight != UNSET) {
                this.childrenNotMeasuredByVerticalWeightConstraint.forEach { child ->
                    val lp = child.layoutParams
                    if (child is VirtualLayout) {
                        if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                            child.getReferencesViews().forEach { virtualChild ->
                                addDependency(indexOfChild(child), indexOfChild(virtualChild))
                            }
                        } else {
                            child.getReferencesViews().forEach { virtualChild ->
                                addDependency(indexOfChild(virtualChild), indexOfChild(child))
                            }
                        }
                    }
                }
            }

            if (lp.widthToWidth != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.widthToWidth)))

            if (lp.widthToHeight != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.widthToHeight)))

            if (lp.heightToHeight != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.heightToHeight)))

            if (lp.heightToWidth != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.heightToWidth)))

            if (child is VirtualLayout) {
                if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    child.getReferencesViews().forEach { virtualChild ->
                        addDependency(index, indexOfChild(virtualChild))
                    }
                } else {
                    child.getReferencesViews().forEach { virtualChild ->
                        addDependency(indexOfChild(virtualChild), index)
                    }
                }
            }
        }
        if (debug) {
            graph.getAdj().forEachIndexed { index, value ->
                val text = "${getResourceName(index)} => ${value.map { getResourceName(it) }}"
                Log.d("echo", text)
            }
        }
        val sortedIndexes = graph.sort().toList()
        sortedIndexes.forEach { index -> output.add(getChildAt(index)) }
    }

    private fun sortForLayoutPass(output: MutableList<View>) {
        if (debug)
            Log.d("echo", "sortForLayoutPass")

        output.clear()
        graph.reset(childCount)
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val lp = child.layoutParams as LayoutParams

            if (lp.leftToLeft != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.leftToLeft)))

            if (lp.leftToRight != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.leftToRight)))

            if (lp.rightToRight != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.rightToRight)))

            if (lp.rightToLeft != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.rightToLeft)))

            if (lp.topToTop != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.topToTop)))

            if (lp.topToBottom != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.topToBottom)))

            if (lp.bottomToBottom != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.bottomToBottom)))

            if (lp.bottomToTop != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.bottomToTop)))

            if (lp.circleConstraint != View.NO_ID)
                addDependency(index, indexOfChild(findViewById(lp.circleConstraint)))

            if (child is VirtualLayout) {
                child.getReferencesViews().forEach {
                    addDependency(indexOfChild(it), index)
                }
            }
        }
        val sortedIndexes = graph.sort()
        sortedIndexes.forEach { index -> output.add(getChildAt(index)) }
    }

    private fun addDependency(indexOfView: Int, indexOfDependsOf: Int) {
        if (debug)
            Log.d(
                "echo",
                "${getResourceName(indexOfView)} depends of ${getResourceName(indexOfDependsOf)}"
            )
        graph.addEdge(indexOfView, indexOfDependsOf)
    }

    class LayoutParams : FrameLayout.LayoutParams {
        var widthPercent: Float = UNSET
        var heightPercent: Float = UNSET

        var horizontalWeight: Float = UNSET
        var verticalWeight: Float = UNSET

        var widthToWidth = View.NO_ID
        var widthToHeight = View.NO_ID
        var heightToHeight = View.NO_ID
        var heightToWidth = View.NO_ID

        var dimensionRatio = UNSET.toString()
        var dimensionRatioValue = UNSET

        var widthFactor = 1f
        var heightFactor = 1f

        var guideBegin = UNSET
        var guideEnd = UNSET
        var guidePercent = UNSET

        var orientation = UNSET.toInt()

        var leftToLeft = View.NO_ID
        var leftToRight = View.NO_ID
        var rightToRight = View.NO_ID
        var rightToLeft = View.NO_ID
        var topToTop = View.NO_ID
        var topToBottom = View.NO_ID
        var bottomToBottom = View.NO_ID
        var bottomToTop = View.NO_ID

        var centerToCenter = View.NO_ID

        var horizontalBias = 0.5f
        var verticalBias = 0.5f

        var circleConstraint = View.NO_ID
        var circleAngle = 0f
        var circleRadius = 0f

        var virtualParent = View.NO_ID

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a =
                context.obtainStyledAttributes(attrs, R.styleable.ConstraintFrameLayout_Layout)

            //region measure constraints
            if (a.hasValue(R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintWidth_percent))
                widthPercent = MathUtils.clamp(
                    abs(
                        a.getFloat(
                            R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintWidth_percent,
                            widthPercent
                        )
                    ), 0f, Float.MAX_VALUE
                )
            if (a.hasValue(R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintHeight_percent))
                heightPercent = MathUtils.clamp(
                    abs(
                        a.getFloat(
                            R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintHeight_percent,
                            heightPercent
                        )
                    ), 0f, Float.MAX_VALUE
                )

            if (a.hasValue(R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintHorizontal_weight))
                horizontalWeight = MathUtils.clamp(
                    abs(
                        a.getFloat(
                            R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintHorizontal_weight,
                            0f
                        )
                    ), 0f, Float.MAX_VALUE
                )
            if (a.hasValue(R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintVertical_weight))
                verticalWeight = MathUtils.clamp(
                    abs(
                        a.getFloat(
                            R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintVertical_weight,
                            0f
                        )
                    ), 0f, Float.MAX_VALUE
                )

            if (a.hasValue(R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintDimensionRatio)) {
                val dimensionRatio =
                    a.getString(R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintDimensionRatio)
                if (dimensionRatio != null)
                    dimensionRatioValue = parseDimensionRatio(dimensionRatio)
            }

            widthFactor = a.getFloat(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintWidthFactor,
                widthFactor
            )
            heightFactor = a.getFloat(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintHeightFactor,
                heightFactor
            )

            widthToWidth = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintWidth_toWidthOf,
                widthToWidth
            )
            widthToHeight = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintWidth_toHeightOf,
                widthToHeight
            )
            heightToHeight = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintHeight_toHeightOf,
                heightToHeight
            )
            heightToWidth = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_measure_constraintHeight_toWidthOf,
                heightToWidth
            )
            //endregion

            //region guide constraints
            guideBegin = a.getDimension(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintGuide_begin,
                guideBegin
            )
            guideEnd = a.getDimension(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintGuide_end,
                guideEnd
            )
            guidePercent = a.getFloat(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintGuide_percent,
                guidePercent
            )

            orientation = a.getInt(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintGuide_orientation,
                orientation
            )
            //endregion

            //region side layout constraints
            leftToLeft = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintLeft_toLeftOf,
                leftToLeft
            )
            leftToRight = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintLeft_toRightOf,
                leftToRight
            )
            rightToRight = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintRight_toRightOf,
                rightToRight
            )
            rightToLeft = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintRight_toLeftOf,
                rightToLeft
            )
            topToTop = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintTop_toTopOf,
                topToTop
            )
            topToBottom = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintTop_toBottomOf,
                topToBottom
            )
            bottomToBottom = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintBottom_toBottomOf,
                bottomToBottom
            )
            bottomToTop = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintBottom_toTopOf,
                bottomToTop
            )
            //endregion

            //region center layout constraints
            centerToCenter = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintCenter_toCenterOf,
                centerToCenter
            )
            //endregion

            //region bias layout constraints
            horizontalBias = MathUtils.clamp(
                abs(
                    a.getFloat(
                        R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintHorizontal_bias,
                        horizontalBias
                    )
                ), 0f, 1f
            )
            verticalBias = MathUtils.clamp(
                abs(
                    a.getFloat(
                        R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintVertical_bias,
                        verticalBias
                    )
                ), 0f, 1f
            )
            //endregion

            //region circle layout constraints
            circleConstraint = a.getResourceId(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintCircle,
                circleConstraint
            )
            circleAngle = a.getFloat(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintCircleAngle,
                circleAngle
            )
            circleRadius = a.getDimension(
                R.styleable.ConstraintFrameLayout_Layout_ConstraintFrameLayout_layout_constraintCircleRadius,
                circleRadius
            )
            //endregion

            a.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height) {}


        private fun parseDimensionRatio(ratio: String): Float {
            var returnValue = 0.0f
            if (ratio.isNotEmpty()) {
                var dimensionRatio = 0.0f
                val len = ratio.length

                val colonIndex = ratio.indexOf(':')
                val r: String
                if (colonIndex >= 0 && colonIndex < len - 1) {
                    r = ratio.substring(0, colonIndex)
                    val denominator = ratio.substring(colonIndex + 1)
                    if (r.isNotEmpty() && denominator.isNotEmpty()) {
                        try {
                            val nominatorValue = r.toFloat()
                            val denominatorValue = denominator.toFloat()
                            dimensionRatio = nominatorValue / denominatorValue
                        } catch (var12: NumberFormatException) {
                        }
                    }
                } else {
                    r = ratio.substring(0)
                    if (r.isNotEmpty()) {
                        try {
                            dimensionRatio = r.toFloat()
                        } catch (var11: NumberFormatException) {
                        }
                    }
                }
                if (dimensionRatio > 0.0f)
                    returnValue = dimensionRatio
            } else {
                returnValue = 0.0f
            }
            return returnValue
        }

        companion object {
            const val UNSET = -1f
            const val HORIZONTAL = 0
            const val VERTICAL = 1
        }
    }

    private fun measureModeToString(mode: Int): String {
        return when (mode) {
            MeasureSpec.UNSPECIFIED -> "UNSPECIFIED"
            MeasureSpec.EXACTLY -> "EXACTLY"
            MeasureSpec.AT_MOST -> "AT_MOST"
            else -> "NONE"
        }
    }

    private fun getResourceName(child: View, default: String = "unknown"): String {
        val id = child.id
        return if (id != View.NO_ID)
            try {
                resources.getResourceEntryName(id)
            } catch (e: Resources.NotFoundException) {
                default
            }
        else
            default
    }

    private fun getResourceName(index: Int, default: String = "unknown"): String {
        val child = getChildAt(index)
        val id = child.id
        return if (id != View.NO_ID)
            try {
                resources.getResourceEntryName(id)
            } catch (e: Resources.NotFoundException) {
                default
            }
        else
            default
    }

    companion object {
        const val TAG = "ConstraintFrameLayout"

        var debug = false
    }
}