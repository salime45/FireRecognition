package com.imonje.firerecognition

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.SparseIntArray
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    protected val REQUEST_PHOTO_CAPTURE = 100
    protected val REQUEST_VIDEO_CAPTURE = 200

    private val PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA)

    var mCurrentPhotoPath: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        verifyStoragePermissions()

        fab.setOnClickListener { view ->
            camera(REQUEST_PHOTO_CAPTURE)
        }
    }


    fun camera(request: Int) {

        var extension = ".jpg"
        var intent: Intent? = null

        if (request == REQUEST_PHOTO_CAPTURE) {
            intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        } else if (request == REQUEST_VIDEO_CAPTURE) {
            intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            extension = ".mp4"
        }
        val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", createImageFile(extension))

        intent?.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, request)
    }

    private fun createImageFile(extension: String): File {

        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

        val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        storageDir.mkdirs()
        val image = File.createTempFile(timeStamp, extension, storageDir)

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PHOTO_CAPTURE && resultCode == Activity.RESULT_OK) {
            val image: FirebaseVisionImage
            try {
                val uri = Uri.parse(mCurrentPhotoPath)

                image = FirebaseVisionImage.fromFilePath(this, uri)

                val detector = FirebaseVision.getInstance().visionLabelDetector;

                detector.detectInImage(image).addOnSuccessListener { (labels) ->

                    text.text = labels.label
//                    for( x in labels)
                    Log.d("TAGGGG", labels.label)
                    Log.d("TAGGGG", labels.entityId)
                    Log.d("TAGGGG", labels.confidence.toString())

                }.addOnFailureListener {

                }

            } catch (e: IOException) {
                e.printStackTrace()
            }

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

    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(CameraAccessException::class)
    private fun getRotationCompensation(cameraId: String, activity: Activity, context: Context): Int {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        val result: Int
        when (rotationCompensation) {
            0 -> result = FirebaseVisionImageMetadata.ROTATION_0
            90 -> result = FirebaseVisionImageMetadata.ROTATION_90
            180 -> result = FirebaseVisionImageMetadata.ROTATION_180
            270 -> result = FirebaseVisionImageMetadata.ROTATION_270
            else -> {
                result = FirebaseVisionImageMetadata.ROTATION_0
                Log.e("TAG", "Bad rotation value: $rotationCompensation")
            }
        }
        return result
    }

    fun verifyStoragePermissions() {

        ActivityCompat.requestPermissions(
                this,
                PERMISSIONS, 1
        )
    }

}
