package com.rvirin.onvif.yashladha

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.pedro.vlc.VlcListener
import com.pedro.vlc.VlcVideoLibrary
import com.rvirin.onvif.R
import kotlinx.android.synthetic.main.activity_stream.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


/**
 * This activity helps us to show the live stream of an ONVIF camera thanks to VLC library.
 */
class StreamActivity : AppCompatActivity(), VlcListener, View.OnClickListener {

    private var vlcVideoLibrary: VlcVideoLibrary? = null
    private val UPLOAD_URL = "http://192.168.43.94:3000/upload"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream)

        val bStartStop = findViewById<Button>(R.id.btn_record)
        bStartStop.setOnClickListener(this)
        vlcVideoLibrary = VlcVideoLibrary(this, this, surfaceView)
    }

    /**
     * Called by VLC library when the video is loading
     */
    override fun onComplete() {
        Toast.makeText(this, " \uD83D\uDC88 Loading video...", Toast.LENGTH_LONG).show()
        btn_record.text = getString(R.string.take_screenshot)
    }

    /**
     * Called by VLC library when an error occured (most of the time, a problem in the URI)
     */
    override fun onError() {
        Toast.makeText(this, " ⛔️ Error, make sure your endpoint is correct", Toast.LENGTH_SHORT).show()
        vlcVideoLibrary?.stop()
    }


    override fun onClick(v: View?) {

        vlcVideoLibrary?.let { vlcVideoLibrary ->

            if (!vlcVideoLibrary.isPlaying) {
                val url = intent.getStringExtra(RTSP_URL)
                vlcVideoLibrary.play(url)
            } else {
                val bitmap = takeScreenShot()
                surfaceView.isDrawingCacheEnabled = false

                val filename = saveFile(bitmap)
                try {
                    val barcodeEncoder = BarcodeEncoder()
                    val qrBMP = barcodeEncoder.encodeBitmap(filename, BarcodeFormat.QR_CODE, 400, 400)
                    iv_qrcode.setImageBitmap(qrBMP)
                    iv_qrcode.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Log.e("QR Code", e.message)
                }
            }
        }
    }

    private fun saveFile(bitmap: Bitmap): String {
        val curDate = SimpleDateFormat("ddMMyyyy_HHmm").format(Date())
        val file = File(Environment.getExternalStorageDirectory().toString() + "/" + curDate + ".jpg")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        return curDate
    }

    private fun stringImage(bitmap: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun takeScreenShot(): Bitmap {
        return surfaceView.bitmap
    }
}

