package com.rvirin.onvif.yashladha

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.rvirin.onvif.R
import com.rvirin.onvif.onvifcamera.OnvifDevice
import com.rvirin.onvif.onvifcamera.OnvifListener
import com.rvirin.onvif.onvifcamera.OnvifRequest.Type.*
import com.rvirin.onvif.onvifcamera.OnvifResponse
import com.rvirin.onvif.onvifcamera.currentDevice
import kotlinx.android.synthetic.main.activity_main.*

const val RTSP_URL = "com.rvirin.onvif.onvifcamera.demo.RTSP_URL"

class MainActivity : AppCompatActivity(), OnvifListener, View.OnClickListener {

    private val TAG = "Login"
    private var toast : Toast? = null
    private val requestPermission = 1

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    requestPermission)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            requestPermission -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "Permissions Granted")
                } else {
                    toast = Toast.makeText(this, "Please grant the permissions", Toast.LENGTH_SHORT)
                    toast?.show()
                }
            }
        }
    }

    override fun onClick(v: View?) {
        if (v == btn_stream) {
            val ipAddr = et_ip.text.toString()
            val username = et_username.text.toString()
            val password = et_password.text.toString()

            if (ipAddr.isNotEmpty() and username.isNotEmpty() and password.isNotEmpty()) {
                currentDevice = OnvifDevice(ipAddr, username, password)
                currentDevice.listener = this
                main_layout.alpha = 0.5f
                pbar.visibility = View.VISIBLE
                currentDevice.getServices()
            } else {
                toast?.cancel()
                toast = Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT)
                toast?.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_stream.setOnClickListener(this)

        et_ip.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s) && !Patterns.IP_ADDRESS.matcher(s).matches()) {
                    til_ip.isErrorEnabled = true
                    til_ip.error = "Invalid IP Address"
                } else {
                    til_ip.isErrorEnabled = false
                }
            }
        })
    }

    override fun requestPerformed(response: OnvifResponse) {

        Log.d("INFO", response.parsingUIMessage)

        toast?.cancel()

        if (!response.success) {
            main_layout.alpha = 1f
            pbar.visibility = View.GONE
            Log.e("ERROR", "request failed: ${response.request.type} \n Response: ${response.error}")
            toast = Toast.makeText(this, " ⛔️ Request Failed: ${response.request.type}", Toast.LENGTH_SHORT);
            toast?.show()
        }

        // if GetServices have been completed, we request the device information
        else if (response.request.type == GetServices) {
            currentDevice.getDeviceInformation()
        }

        // if GetDeviceInformation have been completed, we request the profiles
        else if (response.request.type == GetDeviceInformation) {
            toast = Toast.makeText(this, " \uD83D\uDCF3 Device Information received", Toast.LENGTH_SHORT)
            toast?.show()
            currentDevice.getProfiles()
        }

        // if GetProfiles have been completed, we request the Stream URI
        else if (response.request.type == GetProfiles) {
            currentDevice.getStreamURI()
        }

        // if GetStreamURI have been completed, we're ready to play the video
        else if (response.request.type == GetStreamURI) {
            main_layout.alpha = 1f
            pbar.visibility = View.GONE
            toast = Toast.makeText(this, "Stream URI Received ✨", Toast.LENGTH_SHORT);
            toast?.show()

            currentDevice.rtspURI?.let {uri ->
                val intent = Intent(this, StreamActivity::class.java).apply {
                    putExtra(RTSP_URL, uri)
                }
                startActivity(intent)
            }
        }

    }


}
