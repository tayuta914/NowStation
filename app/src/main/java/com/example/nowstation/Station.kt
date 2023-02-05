package com.example.nowstation

data class Station(
    val name: String,
    val prev: String,
    val next: String,
    val x: Int,
    val y: Int,
    val dt: Int,
    val distance: Int,
    val postal: Int,
    val prefecture: String,
    val line: String,
)