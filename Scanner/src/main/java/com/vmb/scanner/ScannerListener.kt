package com.vmb.scanner

interface ScannerListener {
    fun onSuccess(scanCode : String)
    fun onFailed(response : String)
}