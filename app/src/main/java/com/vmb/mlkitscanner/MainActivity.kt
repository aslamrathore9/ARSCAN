package com.vmb.mlkitscanner

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vmb.scanner.Scanner
import com.vmb.scanner.ScannerListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), ScannerListener {

    var pause: Boolean = false
    var cameraMode: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Scanner.startScanner(this, scannerPreView, this, true)
                .checkCodeExists(false)
                .setResolution(Scanner.Low_Resolution)
                .logPrint(true)
                .muteBeepSound(false)

        flipCamera.setOnClickListener {
            if(pause){
                pause = false
                Scanner.pauseScan()
            }else{
                pause = true
                Scanner.resumeScan()
            }
        }

        flash.setOnClickListener {
            Scanner.toggleTorch()
        }
/*
        flipCamera.setOnClickListener {
            if(cameraMode){
                cameraMode = false
                Scanner.cameraSelect(this, Scanner.FrontCamera)
            }
            else{
                cameraMode = true
                Scanner.cameraSelect(this, Scanner.BackCamera)
            }
        }*/

//        Scanner.muteBeepSound(true)
//        val afd = assets.openFd("AudioFile.mp3")
//        Scanner.mediaPlayer?.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Scanner.REQUEST_CODE_PERMISSIONS) {
            if (Scanner.allPermissionsGranted(this)) {
                Scanner.startScanner(this, scannerPreView, this);
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    override fun onSuccess(scanCode: String) {

    }

    override fun onFailed(response: String) {

    }
}