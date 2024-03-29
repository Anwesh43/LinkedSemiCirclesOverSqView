package com.anwesh.uiprojects.semicirclesoversqview

/**
 * Created by anweshmishra on 14/05/19.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF

val nodes : Int = 5
val lines : Int = 2
val circles : Int = 3
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#673AB7")
val backColor : Int = Color.parseColor("#BDBDBD")

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int,  n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawSemiCircle(i : Int, sc : Float, y: Float,  r : Float, paint : Paint) {
    save()
    translate(-2 * r + 2 * r * i, y)
    drawArc(RectF(-r, -r, r, r), 180f, 180f * sc.divideScale(i, circles), false, paint)
    restore()
}

fun Canvas.drawSqLine(i : Int, sc : Float, y : Float, size : Float, paint : Paint) {
    save()
    translate((1f - 2 * i) * size * sc.divideScale(i, lines), y)
    drawLine(-size / 2, 0f, size / 2, 0f, paint)
    restore()
}

fun Canvas.drawSCONode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val rSize : Float = 2 * size / circles
    paint.style = Paint.Style.STROKE
    save()
    translate(w / 2, gap * (i + 1))
    drawRect(RectF(-rSize / 2, -rSize / 2, rSize / 2, rSize / 2), paint)
    for (j in 0..(lines - 1)) {
        drawSqLine(j, sc1, -rSize / 2, rSize, paint)
    }
    for (j in 0..(circles - 1)) {
        drawSemiCircle(j, sc2, -rSize / 2, rSize / 2, paint)
    }
    restore()

}

class SemiCirclesOverSqView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, circles)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class SCONode(var i : Int, val state : State = State()) {

        private var next : SCONode? = null
        private var prev : SCONode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = SCONode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawSCONode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : SCONode {
            var curr : SCONode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class SemiCirclesOverSq(var i : Int) {

        private val root : SCONode = SCONode(0)
        private var curr : SCONode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : SemiCirclesOverSqView) {

        private val animator : Animator = Animator(view)
        private val scos : SemiCirclesOverSq = SemiCirclesOverSq(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            scos.draw(canvas, paint)
            animator.animate {
                scos.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            scos.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : SemiCirclesOverSqView {
            var view : SemiCirclesOverSqView = SemiCirclesOverSqView(activity)
            activity.setContentView(view)
            return view
        }
    }
}