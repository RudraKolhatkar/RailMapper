package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.filament.EntityManager
import com.google.android.filament.RenderableManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.ar.sceneform.rendering.Renderable
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.Plane
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

import com.example.navbartest.R

class Renderer : AppCompatActivity() {

    private var PermissionCode = 1
    lateinit var fusedLocationclient: FusedLocationProviderClient
    lateinit var viewModel : ViewModel

    fun minPoint(node1: NodeData, node2: NodeData): Position{
        val xmid = min(node1.modelNode.position.x, node2.modelNode.position.x) + (abs(node1.modelNode.position.x - node2.modelNode.position.x) / 2)
        val ymid = min(node1.modelNode.position.y, node2.modelNode.position.y) + (abs(node1.modelNode.position.y - node2.modelNode.position.y) / 2)
        val zmid = min(node1.modelNode.position.z, node2.modelNode.position.z) + (abs(node1.modelNode.position.z - node2.modelNode.position.z) / 2)

        return(Position(xmid, ymid, zmid))
    }

    fun distanceFormula(node1: NodeData, node2: NodeData): Float{
        val xdist: Float = abs(node1.modelNode.position.x - node2.modelNode.position.x)
        val ydist: Float = abs(node1.modelNode.position.y - node2.modelNode.position.y)
        val zdist: Float = abs(node1.modelNode.position.z - node2.modelNode.position.z)

        val totalDist: Float = sqrt(((xdist*xdist) + (ydist*ydist) + (zdist*zdist)).toDouble()).toFloat()
        return totalDist
    }

    fun route(startNode: NodeData, endNode: NodeData): MutableList<NodeData>{
        val openNodes = mutableListOf<NodeData>()
        val closedNodes = mutableListOf<NodeData>(startNode)
        var currentNode : NodeData = startNode
        var linkedNodes : MutableList<NodeData>

        while (currentNode != endNode){
            linkedNodes = currentNode.linkedNodes


            linkedNodes.forEach { Node ->
                val fCost: Float
                val hCost: Float
                if (Node !in closedNodes) {
                    fCost = currentNode.totalMovementCost + 1
                    hCost = distanceFormula(Node, endNode)
                    val newMovementCost = fCost + hCost

                    if (Node.totalMovementCost == 0f || Node.totalMovementCost > newMovementCost){
                        Node.totalMovementCost = newMovementCost
                        Node.lastNode = currentNode
                    }

                    if (Node !in openNodes){
                        openNodes += Node
                    }
                }
            }

            closedNodes += currentNode
            openNodes.remove(currentNode)

            val minimumOpenNode = openNodes.minByOrNull { it.totalMovementCost }
            if (minimumOpenNode != null) {
                currentNode = minimumOpenNode
            }

            if (openNodes.isEmpty()){
                println("No route")
                currentNode = startNode
                break
            }
        }

        val routeList = mutableListOf<NodeData>()
        if (currentNode == endNode){
            while (currentNode.lastNode != null){
                routeList += currentNode
                currentNode = currentNode.lastNode!!
            }
            routeList += currentNode
        }
        else {
            println("Exception thrown for routing")
        }

        return routeList
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_renderer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationclient = LocationServices.getFusedLocationProviderClient(this)
        val cancellationtokenSource = CancellationTokenSource()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permission = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            requestPermissions(permission, PermissionCode)

        }


        //SceneData - going to be streamed directly from server
        val scenedata = SceneData(platforms = mutableListOf(
            listOf(ObjectData(x = 0f, y = 0f), ObjectData(x = 1f, y = 0f)),
            listOf(ObjectData(x = 0f, y = 1f), ObjectData(x = 1f, y = 1f)),
            listOf(ObjectData(x = 0f, y = 2f), ObjectData(x = 1f, y = 2f)),
            listOf(ObjectData(x = 0f, y = 3f), ObjectData(x = 1f, y = 3f)),
            listOf(ObjectData(x = 0f, y = 4f), ObjectData(x = 1f, y = 4f)),
            listOf(ObjectData(x = 0f, y = 5f), ObjectData(x = 1f, y = 5f)),
            listOf(ObjectData(x = 0f, y = 6f), ObjectData(x = 1f, y = 6f)),
            listOf(ObjectData(x = 0f, y = 7f), ObjectData(x = 1f, y = 7f)),
            listOf(ObjectData(x = 0f, y = 8f), ObjectData(x = 1f, y = 8f)),
            listOf(ObjectData(x = 0f, y = 9f), ObjectData(x = 1f, y = 9f))
        ))

        // Viewmodel Initialization
        viewModel = ViewModelProvider(this)[ViewModel::class.java]

        //Element Initialization
        val Sceneview = findViewById<SceneView>(R.id.Sceneviewmain)
        val SeekbarX = findViewById<SeekBar>(R.id.seekBarx)
        val SeekbarY = findViewById<SeekBar>(R.id.seekBary)
        val button = findViewById<Button>(R.id.button)
        val originNode = Node(Sceneview.engine, EntityManager.get().create())

        //Camera and Origin node Initialization
        Sceneview.cameraNode.far = 1000f
        originNode.position = Position(viewModel.origin_pos[0], viewModel.origin_pos[1], viewModel.origin_pos[2])
        Sceneview.cameraNode.position = Position(viewModel.camera_pos_def[0], viewModel.camera_pos_def[1], viewModel.camera_pos_def[2])


        //Instance data initialization
        println("instance data")
        val instanceData: MutableList<MutableList<ModelInstance>> = mutableListOf()
        for (i in 0..<scenedata.platforms.size){
            val temp_list = mutableListOf<ModelInstance>()

            for (j in 0..<scenedata.platforms[i].size){
                temp_list += Sceneview.modelLoader.createModelInstance("Plane.glb")
            }

            instanceData += temp_list
        }

        //Model node initialization
        val modelnodes: MutableList<MutableList<ModelNode>> = mutableListOf()
        println("Modelnodes")
        for (i in 0..<instanceData.size){
            val temp_list = mutableListOf<ModelNode>()

            for (j in 0..<instanceData[i].size){

                temp_list += ModelNode(instanceData[i][j], scaleToUnits = 1f)
                temp_list[j].position = Position(scenedata.platforms[i][j].x, 0f, scenedata.platforms[i][j].y)
            }

            modelnodes += temp_list
        }

        //Adding nodes to scene
        println("Adding nodes")
        for (i in 0..<modelnodes.size){
            for (j in 0..<modelnodes[i].size){
                Sceneview.addChildNode(modelnodes[i][j])
            }
        }

        val nodeData : MutableList<MutableList<NodeData>> = mutableListOf()
        for (i in 0..<modelnodes.size){
            var templist = mutableListOf<NodeData>()
            for (j in 0..<modelnodes[i].size){
                templist += NodeData(modelNode = modelnodes[i][j])
            }
            nodeData += templist
        }

        //Initialize NodeData list with Linked nodes
        println("Make node data")

        for (i in 0..<nodeData.size){
            for (j in 0..<nodeData[i].size){
                println("${i}, $j")
                if (j == 0) {
                    nodeData[i][j].linkedNodes = when (i) {
                        0 -> {
                            println("1")
                            mutableListOf(nodeData[i][j+1], nodeData[i+1][j], nodeData[i+1][j+1])
                        }

                        modelnodes.size - 1 -> {
                            println("2")
                            mutableListOf(nodeData[i][j+1], nodeData[i-1][j], nodeData[i-1][j+1])
                        }

                        else -> {
                            println("3")
                            mutableListOf(nodeData[i-1][j+1], nodeData[i-1][j],
                                nodeData[i][j+1],
                                nodeData[i+1][j+1], nodeData[i+1][j])
                        }
                    }
                }
                else if (j == nodeData[i].size - 1) {
                    nodeData[i][j].linkedNodes = when (i) {
                        0 -> {
                            println("4")
                            mutableListOf(nodeData[i][j-1], nodeData[i+1][j], nodeData[i+1][j-1])
                        }

                        nodeData.size - 1 -> {
                            println("5")
                            mutableListOf(nodeData[i][j-1], nodeData[i-1][j], nodeData[i-1][j-1])
                        }

                        else -> {
                            println("6")
                            mutableListOf(nodeData[i-1][j], nodeData[i-1][j-1],
                                nodeData[i][j-1],
                                nodeData[i+1][j-1], nodeData[i+1][j])
                        }
                    }
                }
                else {

                    nodeData[i][j].linkedNodes = when (i) {
                        0 -> {
                            println("7")
                            mutableListOf(nodeData[i][j-1],                       nodeData[i][j+1],
                                nodeData[i+1][j-1], nodeData[i+1][j], nodeData[i+1][j+1])
                        }

                        nodeData.size - 1 -> {
                            println("8")
                            mutableListOf(nodeData[i-1][j-1], nodeData[i-1][j], nodeData[i-1][j+1],
                                nodeData[i][j-1],                       nodeData[i][j+1])
                        }

                        else -> {
                            println("9")
                            mutableListOf(nodeData[i-1][j-1], nodeData[i-1][j], nodeData[i-1][j+1],
                                nodeData[i][j-1],                       nodeData[i][j+1],
                                nodeData[i+1][j-1], nodeData[i+1][j], nodeData[i+1][j+1])
                        }
                    }

                }
            }
        }

        val routeList = route(nodeData[0][0], nodeData[7][1])


        for (i in 0..<routeList.size) {
            if (i == 0 || i == routeList.size - 1) {
                val routeLineStartInstance = Sceneview.modelLoader.createModelInstance("End_Point.glb")
                val routeLineStartModel = ModelNode(routeLineStartInstance, scaleToUnits = 0.7f)
                routeLineStartModel.position = routeList[i].modelNode.position
                Sceneview.addChildNode(routeLineStartModel)
            } else {
                val instance = Sceneview.modelLoader.createModelInstance("Routing_line.glb")
                val modelNode = ModelNode(instance, scaleToUnits = 1.0f)
//                val minpoint = minPoint(routeList[i], routeList[i + 1])
                modelNode.position = routeList[i].modelNode.position
                Sceneview.addChildNode(modelNode)
            }
        }

        //function to run every frame
        Sceneview.onFrame = {
            Sceneview.cameraNode.lookAt(originNode.position)
            fusedLocationclient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationtokenSource.token).addOnSuccessListener { location ->
                println("latitude: ${location.latitude}" +
                        " longitude: ${location.longitude}")
            }
        }

        //Seekbar connection to the camera
        SeekbarX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.camera_pos[0] = viewModel.camera_pos_def[0] + (progress.toFloat()/10) - 50
                Sceneview.cameraNode.position = Position(viewModel.camera_pos[0], viewModel.camera_pos[1], viewModel.camera_pos[2])

                viewModel.origin_pos[0] = viewModel.origin_pos_def[0] + (progress.toFloat()/10) - 50
                viewModel.ARcamera_pos[0] = viewModel.origin_pos[0]
                originNode.position = Position(viewModel.origin_pos[0], viewModel.origin_pos[1], viewModel.origin_pos[2])
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        SeekbarY.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.camera_pos[2] = viewModel.camera_pos_def[2] + (progress.toFloat()/10) - 50
                Sceneview.cameraNode.position = Position(viewModel.camera_pos[0], viewModel.camera_pos[1], viewModel.camera_pos[2])

                viewModel.origin_pos[2] = viewModel.origin_pos_def[2] + (progress.toFloat()/10) - 50
                viewModel.ARcamera_pos[2] = viewModel.origin_pos[2]
                originNode.position = Position(viewModel.origin_pos[0], viewModel.origin_pos[1], viewModel.origin_pos[2])
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

    }
}