package dev.andoshin11.agentstatuspanel

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.TextView
import com.google.firebase.messaging.FirebaseMessaging

/** Adds the widget, and shows the token to give the Mac mini. */
class MainActivity : Activity() {
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val root = findViewById<View>(R.id.root)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<Button>(R.id.open_battery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        findViewById<Button>(R.id.copy_token).setOnClickListener { copyToken() }
        findViewById<Button>(R.id.add_widget).setOnClickListener { addWidget() }
        loadToken()
    }

    private fun addWidget() {
        val manager = getSystemService(AppWidgetManager::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(this, AgentWidgetProvider::class.java), null, null)
        }
    }

    private fun loadToken() {
        val view = findViewById<TextView>(R.id.token)
        view.setText(R.string.token_loading)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                view.setText(R.string.token_failed)
                return@addOnCompleteListener
            }
            token = task.result
            LedgerStore(this).saveToken(task.result)
            view.text = task.result
            findViewById<Button>(R.id.copy_token).isEnabled = true
        }
    }

    private fun copyToken() {
        val value = token ?: return
        val clip = ClipData.newPlainText(getString(R.string.section_token), value)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }
}
