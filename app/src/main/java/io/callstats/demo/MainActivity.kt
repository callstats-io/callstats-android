package io.callstats.demo

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    submit.setOnClickListener {
      val room = room.text.toString()
      val user = username.text.toString()

      // check the inputs
      if (room.isBlank() || user.isBlank()) {
        Toast.makeText(this, R.string.main_error_input, Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      val intent = Intent(this, CallActivity::class.java)
      intent.putExtra(CallActivity.EXTRA_ROOM, room)
      intent.putExtra(CallActivity.EXTRA_USER, user)
      startActivity(intent)
    }
  }
}
