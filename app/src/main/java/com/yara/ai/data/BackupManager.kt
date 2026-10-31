package com.yara.ai.data
import android.content.Context
import com.google.gson.GsonBuilder
import java.io.*
data class YaraBackup(val profile:UserProfile,val history:List<ChatMessage>,val memories:List<Memory>)
object BackupManager{fun export(store:MemoryStore):String=GsonBuilder().setPrettyPrinting().create().toJson(YaraBackup(store.profile,store.history,store.memories));fun write(ctx:Context,stream:OutputStream,store:MemoryStore){stream.bufferedWriter().use{it.write(export(store))}};fun restore(ctx:Context,stream:InputStream,store:MemoryStore){val b=com.google.gson.Gson().fromJson(stream.bufferedReader().use{it.readText()},YaraBackup::class.java);store.profile=b.profile;store.history=b.history.toMutableList();store.memories=b.memories.toMutableList();store.saveProfile();store.saveHistory();store.saveMemories()}}
