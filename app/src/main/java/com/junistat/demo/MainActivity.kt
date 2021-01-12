package com.junistat.demo

import android.media.CamcorderProfile
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.junistat.demo.helpers.CameraPermissionHelper
import com.junistat.demo.helpers.VideoRecorder


class MainActivity : AppCompatActivity() {

    lateinit var arFragment: ArFragment
    lateinit var recordButton: Button
    lateinit var spinner: Spinner
    var modelRenderable: ModelRenderable? = null
    var videoRecorder = VideoRecorder()
    private val modelList = mutableListOf<Int>()
    lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        recordButton = findViewById(R.id.recordButton)
        spinner = findViewById(R.id.modelPicker)
        adapter =  ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, arrayOf("Ворота", "Сота", "Спринт"))

        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                view?.let {
                    removePreviousAnchors()
                    ModelRenderable.builder()
                        .setSource(view.context, modelList[position])
                        .build()
                        .thenAccept { modelRenderable = it }
                        .exceptionally {
                            Log.e("Error", it.localizedMessage!!)
                            null
                        }
                }

            }

            override fun onNothingSelected(p0: AdapterView<*>?) = Unit
        }

        modelList.add(R.raw.biggates)
        modelList.add(R.raw.sixaedr)
        modelList.add(R.raw.sprint10m)

        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            if (modelRenderable == null) {
               return@setOnTapArPlaneListener
            }
            val anchor: Anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            val model = TransformableNode(arFragment.transformationSystem)
            model.setParent(anchorNode)
            model.renderable = modelRenderable
            model.select()
        }
        val orientation = resources.configuration.orientation
        videoRecorder.setSceneView(arFragment.arSceneView)
        videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_1080P, orientation)

        recordButton.setOnClickListener {
            videoRecorder.onToggleRecord()
            recordButton.text = if (videoRecorder.isRecording) "Остановить запись" else "Начать запись"
            if (!videoRecorder.isRecording) {
                Toast.makeText(it.context, videoRecorder.videoPath?.absolutePath, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun removePreviousAnchors() {
        val nodeList: List<Node> = ArrayList(arFragment.arSceneView.scene.children)
        for (childNode in nodeList) {
            if (childNode is AnchorNode) {
                if (childNode.anchor != null) {
                    (childNode).anchor!!.detach()
                    (childNode).setParent(null)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
        }
        if (!CameraPermissionHelper.hasStoragePermission(this)) {
            CameraPermissionHelper.requestStoragePermission(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}