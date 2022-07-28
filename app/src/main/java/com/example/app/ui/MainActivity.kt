package com.example.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.app.R
import com.mgsoftware.constraintframelayout.widget.ConstraintFrameLayout

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.weight_constraint_inside_flow_more_complex)
        ConstraintFrameLayout.debug = true
    }
}
