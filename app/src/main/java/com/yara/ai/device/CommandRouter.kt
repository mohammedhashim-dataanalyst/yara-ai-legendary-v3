package com.yara.ai.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import com.yara.ai.data.MemoryStore
import com.yara.ai.features.FeatureRuntime

/** Parses only commands it can execute; ordinary conversation returns null. */
class CommandRouter(private val context: Context, private val controller: DeviceController = DeviceController(context)) {
    private val store = MemoryStore.get(context)
    fun route(command: String): String? {
        val text = command.trim().lowercase()
        if (text.isBlank()) return null
        return try {
            when {
                text == "back" || text.contains("go back") || text.contains("പിന്നോട്ട്") -> accessibility()?.back() ?: unavailable()
                text == "home" || text.contains("go home") || text.contains("ഹോം") -> accessibility()?.home() ?: unavailable()
                text.contains("recent apps") || text.contains("recents") || text.contains("റീസന്റ്") -> accessibility()?.recents() ?: unavailable()
                text.contains("notifications") || text.contains("അറിയിപ്പ്") -> accessibility()?.notifications() ?: unavailable()
                text.startsWith("scroll") || text.contains("സ്ക്രോൾ") -> accessibility()?.scroll(if (text.contains("up") || text.contains("മുകളിൽ")) -1 else 1) ?: unavailable()
                text.startsWith("type ") || text.startsWith("enter text ") || text.startsWith("ടൈപ്പ് ") -> accessibility()?.setFocusedText(text.substringAfter(' ').trim()) ?: unavailable()
                text.contains("flash") || text.contains("torch") || text.contains("ഫ്ലാഷ്") -> controller.flashlight(!(text.contains("off") || text.contains("അണയ്ക്ക")))
                text.contains("brightness") || text.contains("screen light") || text.contains("വെളിച്ചം") -> controller.setBrightness(number(text, 60))
                text.contains("volume") || text.contains("ശബ്ദം") -> if (text.contains("up") || text.contains("increase") || text.contains("കൂട്ട")) controller.adjustVolume(true) else if (text.contains("down") || text.contains("decrease") || text.contains("കുറ")) controller.adjustVolume(false) else controller.setVolume(number(text, 50))
                text.contains("silent") || text.contains("mute") || text.contains("സൈലന്റ്") -> controller.setRinger(true)
                text.contains("ringer") || text.contains("normal mode") -> controller.setRinger(false)
                text.contains("battery") || text.contains("ചാർജ്") -> controller.battery()
                text.contains("open settings") || text.contains("settings തുറ") -> controller.openSettings()
                FeatureRuntime.on(store, "open_apps") && (text.startsWith("open ") || text.startsWith("തുറക്കൂ ")) -> openApp(text.substringAfter(' ').trim())
                FeatureRuntime.on(store, "tap_control") && (text.startsWith("click ") || text.startsWith("tap ") || text.startsWith("ടാപ്പ് ")) -> click(text.substringAfter(' ').trim())
                FeatureRuntime.on(store, "screen_read") && (text.contains("read screen") || text.contains("സ്ക്രീൻ വായിക്ക")) -> readScreen()
                text.contains("set alarm") || text.contains("alarm") || text.contains("അലാറം") -> controller.alarm(number(text, 8), 0)
                text.contains("timer") || text.contains("ടൈമർ") -> controller.timer(number(text, 60))
                FeatureRuntime.on(store, "phone_call") && (text.startsWith("call ") || text.startsWith("വിളിക്കൂ ")) -> call(text.substringAfter(' ').trim())
                FeatureRuntime.on(store, "send_messages") && (text.startsWith("sms ") || text.startsWith("message ") || text.startsWith("മെസേജ് ")) -> sms(text.substringAfter(' ').trim())
                FeatureRuntime.on(store, "whatsapp_automation") && text.contains("whatsapp") -> whatsapp(text)
                FeatureRuntime.on(store, "media_control") && (text.contains("pause") || text.contains("താൽക്കാലികമായി നിർത്ത")) -> controller.mediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
                FeatureRuntime.on(store, "media_control") && (text.contains("play music") || text.contains("play song") || text == "play" || text == "പ്ലേ") -> controller.mediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
                FeatureRuntime.on(store, "media_control") && text.contains("next") -> controller.mediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                FeatureRuntime.on(store, "media_control") && (text.contains("previous") || text.contains("back song")) -> controller.mediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                else -> null
            }
        } catch (_: Exception) { "I could not complete that device command." }
    }
    fun supportedCommands(): Set<String> = setOf("volume", "brightness", "torch", "ringer", "open app", "call", "sms", "whatsapp", "play", "pause", "back", "home", "recents", "notifications", "scroll", "type text", "battery", "alarm", "timer")
    private fun number(text: String, fallback: Int): Int = Regex("\\d+").find(text)?.value?.toIntOrNull() ?: fallback
    private fun accessibility() = YaraAccessibilityService.instance
    private fun unavailable(): String = "Please enable accessibility access first."
    private fun click(label: String): String = accessibility()?.let { if (it.clickText(label)) "Tapped $label." else "I could not find $label on screen." } ?: unavailable()
    private fun readScreen(): String = accessibility()?.readScreen()?.takeIf { it.isNotBlank() } ?: "The screen is not available."
    private fun openApp(name: String): String = accessibility()?.let { if (it.openApp(name)) "Opening $name." else "I could not find $name." } ?: unavailable()
    private fun call(number: String): String { context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return "Calling $number." }
    private fun sms(value: String): String { val parts = value.split(Regex("\\s+"), limit = 2); if (parts.size < 2) return "Say SMS followed by a number and message."; SmsManager.getDefault().sendTextMessage(parts[0], null, parts[1], null, null); return "SMS sent." }
    private fun whatsapp(text: String): String { val payload = text.substringAfter("whatsapp").trim(); context.startActivity(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, payload); setPackage("com.whatsapp"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); return "WhatsApp share opened." }
}
