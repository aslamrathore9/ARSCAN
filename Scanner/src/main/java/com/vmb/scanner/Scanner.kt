package com.vmb.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.odml.image.MlImage.IMAGE_FORMAT_YV21
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern
import kotlin.collections.ArrayList


typealias LumaListener = (luma: String) -> Unit

/**
 *  Scanner is module for Qr and Barcode scan
 *
 *  Options of setting in scanner ->
 *      1. pauseScan
 *      2. resumeScan
 *      3. checkCodeExists
 *      4. muteBeepSound
 *      5. setResolution
 *      6. logPrint
 *      7. cameraSelect
 *      8. setBeepSound
 *      9. scanDelayTime
 *      10.toggleTorch
 */

object Scanner {

    private const val TAG = "Scanner"
    const val Already_Code_Scanned = "Already Code Scanned"

    private lateinit var viewFinder: PreviewView
    private lateinit var scannerListener: ScannerListener

    // last scanned time
    private var lastAnalyzedTimestamp = 0L

    const val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    // OCR enable check
    private var enableOCR: Boolean = false
    // Text recognizer
    private lateinit var recognizer: TextRecognizer
    // Object detector
    private lateinit var objectDetector: ObjectDetector

    // scanned code list
    var scanCodes = ArrayList<String>()

    var mediaPlayer: MediaPlayer? = null
    private var mutePlayer: Boolean = false
    private lateinit var barCodeValue: String

    // Select back camera as a default
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    val FrontCamera = 101
    val BackCamera = 102

    // ImageAnalyzer
    private lateinit var imageAnalyzer: ImageAnalysis
    // camera controller
    private lateinit var cameraControl: CameraControl
    // camera Information
    lateinit var cameraInfo: CameraInfo

    private var isCheckCodeExists: Boolean = true

    // default set log print false
    private var printLog: Boolean = false

    // camera resolution options
    private lateinit var camera_resolution: Size
    val Low_Resolution = Size(176, 144)
    val Medium_Resolution = Size(352, 288)
    val High_Resolution = Size(640, 480)

    // scanner delay time set
    private var scannerDelay:Long = 500L

    fun allPermissionsGranted(context: Context) = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun mediaPlayer(context: Context) {
        if (mediaPlayer == null)
            mediaPlayer = MediaPlayer.create(context, R.raw.beep)
        mediaPlayer?.setOnPreparedListener {
            log("Media Play Ready To Go")
        }
    }

    fun startScanner(
        context: Context,
        viewFinder: PreviewView,
        scannerListener: ScannerListener,
        @Nullable enableOCR:Boolean = false
    ): Scanner {

        this.viewFinder = viewFinder
        this.scannerListener = scannerListener

        // initial value set
        barCodeValue = ""

        if (allPermissionsGranted(context)) {
            // default set resolution to Low
            camera_resolution = Low_Resolution
            mediaPlayer(context)
            camera(context, context as AppCompatActivity, viewFinder, scannerListener)

            // Secondary scan will be OCR
            if(enableOCR) {
                // enable OCR for entire class
                this.enableOCR = enableOCR
                // OCR
                recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            /*    val localModel = LocalModel.Builder()
                    .setAssetFilePath("model.tflite")
                    // or .setAbsoluteFilePath(absolute file path to model file)
                    // or .setUri(URI to model file)
                    .build()

                // Live detection and tracking
                val customObjectDetectorOptions =
                    CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.5f)
                        .setMaxPerObjectLabelCount(3)
                        .build()

                // Object detection
                objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)*/
            }

        } else {
            log("Permissions not granted by the user.")
            ActivityCompat.requestPermissions(
                context as Activity,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        return this
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun camera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        viewFinder: PreviewView,
        scannerListener: ScannerListener
    ) {

        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
              val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

         /*   if (imageCapture == null)
                imageCapture = ImageCapture.Builder()
                    .setTargetResolution(camera_resolution)
                    .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
        */

              imageAnalyzer = ImageAnalysis.Builder()
                  .setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                  .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                  .build()

            // set ImageAnalysis
             setScannerAnalyzer(scannerListener)

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
               val camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
//                    imageCapture,
                    imageAnalyzer
                )

                // Listen to pinch gestures
                val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        // Get the camera's current zoom ratio
                        val currentZoomRatio = camera!!.cameraInfo.zoomState.value?.zoomRatio ?: 0F

                        // Get the pinch gesture's scaling factor
                        val delta = detector.scaleFactor

                        // Update the camera's zoom ratio. This is an asynchronous operation that returns
                        // a ListenableFuture, allowing you to listen to when the operation completes.
                        cameraControl.setZoomRatio(currentZoomRatio * delta)

                        // Return true, as the event was handled
                        return true
                    }
                }

                // Zoom gesture detector
                val scaleGestureDetector = ScaleGestureDetector(context, listener)

                // Listen to tap events on the viewfinder and set them as focus regions
                viewFinder.setOnTouchListener(View.OnTouchListener { _: View, motionEvent: MotionEvent ->
                    // Zoom gesture
                    scaleGestureDetector.onTouchEvent(motionEvent)

                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> return@OnTouchListener true
                        MotionEvent.ACTION_UP -> {
                            // Get the MeteringPointFactory from PreviewView
                            val factory = viewFinder.meteringPointFactory

                            // Create a MeteringPoint from the tap coordinates
                            val point = factory.createPoint(motionEvent.x, motionEvent.y)

                            // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode
                            val action = FocusMeteringAction.Builder(point).build()

                            // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                            // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                            camera!!.cameraControl.startFocusAndMetering(action)


                            return@OnTouchListener true
                        }
                        else -> return@OnTouchListener false
                    }

                })

                // camera control
                cameraControl = camera?.cameraControl!!
                cameraInfo = camera.cameraInfo

            } catch (exc: Exception) {
                loge("Use case binding failed  $exc")
            }

        }, ContextCompat.getMainExecutor(context))

    }

    private fun setScannerAnalyzer(scannerListener: ScannerListener) {
        if(::imageAnalyzer.isInitialized) {
            imageAnalyzer.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->

                    if (luma != "0" && !isCheckCodeExists) {
                        scanSuccess(luma, scannerListener)
                    } else if (luma != "0" && !scanCodes.contains(luma)) {
                        scanSuccess(luma, scannerListener)
                    } else if (scanCodes.contains(luma)) {
                        loge("Scan Code : $luma $Already_Code_Scanned")
                        scannerListener.onFailed(Already_Code_Scanned)
                    }

            })
        }
    }

    private fun scanSuccess(luma: String, scannerListener: ScannerListener) {
        log("Scan Code : $luma")

        try {
            scanCodes.add(luma)
            scannerListener.onSuccess(luma)

            if (!mutePlayer)
                mediaPlayer?.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        //    specify the formats to recognize:
        val scanner = BarcodeScanning.getClient(/*options*/)

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        @SuppressLint("UnsafeOptInUsageError")
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        override fun analyze(imageProxy: ImageProxy) {

            // Set delay timer to process
            val currentTimestamp = System.currentTimeMillis()
            if (currentTimestamp - lastAnalyzedTimestamp >= scannerDelay) {

                val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

                /*
             *  *************************** Barcode and QR Scan Process engine ************************************
             * */

                // Pass image to an ML Kit Vision API
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->


                        if (barcodes.isNotEmpty()) {
                            for (barcode in barcodes) {
                                val bounds = barcode.boundingBox
                                val corners = barcode.cornerPoints

                                val rawValue = barcode.rawValue

                                log("Scan Codes : Bounds = $bounds, Corners = $corners, RawValue = $rawValue ")

                                rawValue?.let {
                                    if (barCodeValue != it) {
                                        listener(it)
                                    }
                                    // store value
                                    barCodeValue = it
                                }
                            }

                        } else {

                            log("Scan Codes : No Scan code found")

                            if (enableOCR) {

                                /**  *************************** OCR Process engine ************************************
                                 */

                                recognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        // Task completed successfully

                                        val blocks = visionText.textBlocks

                                        if (blocks.isEmpty()) {
                                            log("OCR Codes : No text found")
                                        } else {

                                            for (i in blocks.indices) {
                                                val lines: List<Text.Line> = blocks[i].lines
                                                for (j in lines.indices) {
                                                    val elements: List<Text.Element> =
                                                        lines[j].elements

                                                    val finalString = getLongestString(elements)

                                                    if (barCodeValue != finalString) {
                                                        listener(finalString.toString())
                                                    }
                                                    // store value
                                                    barCodeValue = finalString.toString()
                                                    log("OCR Value : $barCodeValue")
                                                    //                                                for (k in elements.indices) {
                                                    //                                                    log("OCR Codes : ${elements[k].text} ")
                                                    //                                                }
                                                }
                                            }
                                        }

                                    }
                                    .addOnFailureListener { e ->
                                        // Task failed with an exception
                                        loge(e.message.toString())
                                    }
                                    .addOnCompleteListener {
                                        try {

                                            imageProxy.close()

                                            if (it.result.text.isNotEmpty()) {
                                                barCodeValue = ""       // clear data
                                                log("analyze OCR : Older value removed : ${it.result.text}")
                                            }

                                        } catch (e: Exception) {
                                            loge(e.message!!)
                                        }
                                    }

                                /**  *************************** EOF OCR Process engine ************************************
                                 */

                            }
                        }

                    }
                    .addOnFailureListener {
                        // Task failed with an exception
                        if (printLog)
                            it.printStackTrace()
                        loge("Scanner failed")
                    }
                    .addOnCompleteListener {
                        try {

                            if (!enableOCR)
                                imageProxy.close()

                            if (it.result.isNotEmpty()) {
                                barCodeValue = ""       // clear data
                                log("analyze: Older value removed : ${it.result[0]}")
                                imageProxy.close()
                            }

                        } catch (e: Exception) {
                            loge(e.message!!)
                        }
                    }

                /*
             *  *************************** Object detection Process engine ************************************
             * */

                /* objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                // Task completed successfully
                for (detectedObject in detectedObjects) {
                    val boundingBox = detectedObject.boundingBox
                    val trackingId = detectedObject.trackingId

//                    ObjectGraphic(detectedObject = detectedObject, overlay = viewFinder)

                    for (label in detectedObject.labels) {
                        val text = label.text
                        val confidence = label.confidence
                        log("Object : $text")

                    }
                    log("Object : Top:${boundingBox.top}, Bottom:${boundingBox.bottom}, Left:${boundingBox.left}, Right:${boundingBox.right}")
                }
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                loge(e.message.toString())
            }.addOnCompleteListener {
                imageProxy.close()
            }
*/

                // Update timestamp of last analyzed frame
                lastAnalyzedTimestamp = currentTimestamp

            }else{
                // close image proxy
                imageProxy.close()
            }
        }

    }

    private val regex = "^[a-zA-Z0-9]+$"
    private val pattern: Pattern = Pattern.compile(regex)

    fun getLongestString(array: List<Text.Element>): String? {
        var maxLength = 0
        var longestString: String? = ""
        for (s in array) {
            if(pattern.matcher(s.text).matches()) {
                if (s.text.length > maxLength) {
                    maxLength = s.text.length
                    longestString = s.text
                }
            }
        }
        return longestString
    }

    fun destroyScanner() {
        if (this::cameraExecutor.isInitialized)
            cameraExecutor.shutdown()

        scanCodes.clear()

        log("onDestroy: Scanner")
    }

    private fun log(d: String) {
        if (printLog)
            Log.d(TAG, d)
    }

    private fun loge(e: String) {
        if (printLog)
            Log.e(TAG, e)
    }


    /**
     *
     *  Options of scanner ->
     *      1. pauseScan
     *      2. resumeScan
     *      3. checkCodeExists
     *      4. muteBeepSound
     *      5. setResolution
     *      6. logPrint
     *      7. cameraSelect
     *      8. setBeepSound
     *      9. scanDelayTime
     *      10. toggleTorch
     */

    fun pauseScan() {
        if(::imageAnalyzer.isInitialized) {
            imageAnalyzer.clearAnalyzer()
            log("Scanner is Paused")
        }
    }

    fun resumeScan() {
        if(::imageAnalyzer.isInitialized) {
            setScannerAnalyzer(scannerListener)
            log("Scanner is Resumed")
        }
    }

    fun checkCodeExists(isCheck: Boolean): Scanner {
        isCheckCodeExists = isCheck
        log("Scanner : Check code already scanned is $isCheck")
        return this
    }

    fun muteBeepSound(mute: Boolean): Scanner {
        mutePlayer = mute
        log("Scanner sound is muted")
        return this
    }

    fun setResolution(resolution: Size): Scanner {
        camera_resolution = resolution
        log("Resolution set to $resolution")
        return this
    }

    fun logPrint(printLog: Boolean): Scanner {
        this.printLog = printLog
        return this
    }

    fun cameraSelect(context: Context, Camera: Int): Scanner {
        cameraSelector = if (Camera == BackCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        // restart camera
        startScanner(context, viewFinder, scannerListener)

        return this
    }

    /**
     *  eg:.  val afd = assets.openFd("AudioFile.mp3")
     *        setBeepSound(afd)
    * */
    fun setBeepSound(afd: AssetFileDescriptor): Scanner {
        mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        return this
    }

    /**
     *   set value in milliseconds
     */
    fun scanDelayTime(delayMilliSeconds: Long){
        scannerDelay = delayMilliSeconds
    }

    /**
     *   Switch ON / OFF Torch
     */
    fun toggleTorch() {
        if (cameraInfo.torchState.value == TorchState.ON) {
            cameraControl.enableTorch(false)
        } else {
            cameraControl.enableTorch(true)
        }
    }

}