package io.callstats.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    submit.setOnClickListener {
      // check the inputs
      if (room.text.isBlank()) {
        Toast.makeText(this, R.string.main_error_input, Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }
      requestPermissions()
    }
  }

  private fun startCall() {
    val intent = Intent(this, CallActivity::class.java)
    intent.putExtra(CallActivity.EXTRA_ROOM, room.text.toString())
    startActivity(intent)
  }

  private fun requestPermissions() {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
        0)
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    for (i in 0 until permissions.size) {
      val permission = permissions[i]
      val result = grantResults[i]
      if (permission == Manifest.permission.RECORD_AUDIO && result == PackageManager.PERMISSION_GRANTED) {
        startCall()
        return
      }
    }
    Toast.makeText(this, R.string.main_error_permission, Toast.LENGTH_SHORT).show()
  }
}
