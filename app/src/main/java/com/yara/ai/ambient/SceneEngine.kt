package com.yara.ai.ambient

import java.util.Calendar

enum class SceneKind { BEDROOM, KITCHEN, BEACH, CAFE, STREET, LIVING_ROOM }
class SceneEngine {
    private var index = -1
    var scene: SceneKind = SceneKind.BEDROOM; private set
    fun cycle(delta: Int = 1): SceneKind { index = (scene.ordinal + delta + SceneKind.values().size) % SceneKind.values().size; scene = SceneKind.values()[index]; return scene }
    fun setForTime(calendar: Calendar = Calendar.getInstance()): SceneKind { val h=calendar.get(Calendar.HOUR_OF_DAY); scene=when(h){in 5..8->SceneKind.BEACH;in 9..12->SceneKind.KITCHEN;in 13..16->SceneKind.CAFE;in 17..19->SceneKind.STREET;in 20..22->SceneKind.LIVING_ROOM;else->SceneKind.BEDROOM}; index=scene.ordinal; return scene }
    fun current(): SceneKind = scene
}
