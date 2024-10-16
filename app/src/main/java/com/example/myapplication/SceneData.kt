package com.example.myapplication

import io.github.sceneview.node.ModelNode

class ObjectData(val x: Float, val y: Float, val latitude: Float = 0f,val longitude: Float = 0f)

class NodeData(val modelNode: ModelNode, var linkedNodes: MutableList<NodeData> = mutableListOf(), var totalMovementCost: Float = 0f, var lastNode: NodeData? = null)

class SceneData(val platforms: MutableList<List<ObjectData>>, val planes: MutableList<ObjectData> = mutableListOf())