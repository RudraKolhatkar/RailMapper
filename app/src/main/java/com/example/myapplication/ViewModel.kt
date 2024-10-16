package com.example.myapplication

import androidx.lifecycle.ViewModel

class ViewModel: ViewModel() {
    //MainActivity properties
    var camera_pos = mutableListOf<Float>(-10f, 10f, -10f)
    val camera_pos_def = listOf<Float>(-10f, 10f, -10f)
    val origin_pos = mutableListOf<Float>(0f, 0f, 0f)
    val origin_pos_def = listOf<Float>(0f, 0f, 0f)

    //ArActivity properties
    val ARcamera_pos = mutableListOf<Float>(0f, 0f, 0f)
}