package com.vmb.mlkitscanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.vmb.scanner.Scanner
import com.vmb.scanner.ScannerListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ScannerListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Scanner.startScanner(this, scannerPreView,this)
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
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    override fun onSuccess(scanCode: String) {
        TODO("Not yet implemented")
    }

    override fun onFailed(response: String) {
        TODO("Not yet implemented")
    }
}