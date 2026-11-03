package dev.andoshin11.agentstatuspanel

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.google.firebase.messaging.FirebaseMessaging

/** Shows whether notifications can appear, and the token to give the Mac mini. */
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
        findViewById<Button>(R.id.allow_notifications).setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
        findViewById<Button>(R.id.open_live_updates).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
                )
            }
        }
        findViewById<Button>(R.id.open_battery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        findViewById<Button>(R.id.preview).setOnClickListener { showPreview() }
        findViewById<Button>(R.id.copy_token).setOnClickListener { copyToken() }
        findViewById<Button>(R.id.add_widget).setOnClickListener { addWidget() }
        loadToken()
    }

    override fun onResume() {
        super.onResume()
        showStatus()
        showWidget()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        showStatus()
    }

    private fun showStatus() {
        val allowed = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        findViewById<TextView>(R.id.notifications_status)
            .setText(if (allowed) R.string.notifications_on else R.string.notifications_off)
        findViewById<Button>(R.id.allow_notifications).visibility = if (allowed) View.GONE else View.VISIBLE

        val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
        val promoted = supported && getSystemService(NotificationManager::class.java).canPostPromotedNotifications()
        findViewById<TextView>(R.id.live_updates_status).setText(
            when {
                !supported -> R.string.live_updates_unsupported
                promoted -> R.string.live_updates_on
                else -> R.string.live_updates_off
            },
        )
        findViewById<Button>(R.id.open_live_updates).visibility = if (supported) View.VISIBLE else View.GONE
    }

    /** Draws what the widget shows now, with the same code. */
    private fun showWidget() {
        val host = findViewById<FrameLayout>(R.id.widget_host)
        host.removeAllViews()
        val heightDp = host.layoutParams.height / resources.displayMetrics.density
        val sessions = SessionStore(this).read(System.currentTimeMillis())
        host.addView(WidgetRenderer(this).render(sessions, heightDp.toInt()).apply(this, host))
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

    /** Shows a sample Live Update, then removes it: nothing else would end it. */
    private fun showPreview() {
        val notifier = ProgressNotifier(applicationContext)
        notifier.show(
            ProgressMessage.Update(
                id = "preview",
                at = System.currentTimeMillis(),
                finished = false,
                agent = "claude",
                workspace = "preview",
                goal = getString(R.string.preview_goal),
                step = getString(R.string.preview_step),
                done = 2,
                total = 5,
            ),
        )
        Handler(Looper.getMainLooper()).postDelayed({ notifier.remove("preview") }, PREVIEW_MS)
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 1
        const val PREVIEW_MS = 30_000L
    }
}
