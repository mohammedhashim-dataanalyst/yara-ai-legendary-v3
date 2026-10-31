package com.yara.ai.device

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent
import android.hardware.camera2.CameraManager
import android.net.Uri

class DeviceController(private val context: Context) {
    private val audio: AudioManager get() = context.getSystemService(AudioManager::class.java)

    fun setVolume(percent: Int): String {
        val level = (audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * percent.coerceIn(0, 100) / 100f).toInt()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0); return "Media volume set to ${percent.coerceIn(0, 100)} percent."
    }
    fun adjustVolume(up: Boolean): String { audio.adjustVolume(if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0); return if (up) "Volume increased." else "Volume decreased." }
    fun setBrightness(percent: Int): String {
        if (!Settings.System.canWrite(context)) { context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return "Please allow Yara to modify system settings." }
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (percent.coerceIn(1, 100) * 255 / 100)); return "Brightness set to ${percent.coerceIn(1, 100)} percent."
    }
    fun flashlight(on: Boolean): String {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = manager.cameraIdList.firstOrNull() ?: return "No camera flashlight is available."
        manager.setTorchMode(id, on); return if (on) "Flashlight turned on." else "Flashlight turned off."
    }
    fun setRinger(silent: Boolean): String { audio.ringerMode = if (silent) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL; return if (silent) "Silent mode enabled." else "Normal ringer mode enabled." }
    fun alarm(hour: Int, minute: Int, message: String = "Yara alarm"): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply { putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23)); putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59)); putExtra(AlarmClock.EXTRA_MESSAGE, message); putExtra(AlarmClock.EXTRA_SKIP_UI, false); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent); return "Alarm request opened for %02d:%02d.".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }
    fun timer(seconds: Int, message: String = "Yara timer"): String {
        context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply { putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1)); putExtra(AlarmClock.EXTRA_MESSAGE, message); putExtra(AlarmClock.EXTRA_SKIP_UI, false); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); return "Timer request opened."
    }
    fun battery(): String { val bm = context.getSystemService(BatteryManager::class.java); val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY); return "Battery is at $level percent." }
    fun openSettings(action: String = Settings.ACTION_SETTINGS): String { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return "Settings opened." }
    fun mediaKey(keyCode: Int): String {
        val down = Intent(Intent.ACTION_MEDIA_BUTTON).apply { putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode)) }
        val up = Intent(Intent.ACTION_MEDIA_BUTTON).apply { putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode)) }
        context.sendBroadcast(down); context.sendBroadcast(up); return "Media command sent."
    }
    fun media(action: String): String { context.sendBroadcast(Intent(action).apply { addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES) }); return "Media command sent." }
}
