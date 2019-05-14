package com.anwesh.uiprojects.linkedsemicirclesoversqview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.semicirclesoversqview.SemiCirclesOverSqView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SemiCirclesOverSqView.create(this)
    }
}
