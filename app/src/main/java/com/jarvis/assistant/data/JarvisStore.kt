package com.jarvis.assistant.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.MemoryItem
import com.jarvis.assistant.core.MessageRole
import com.jarvis.assistant.core.SourceLink
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val text: String,
    val timestamp: Long,
    val sources: String,
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long,
)

@Dao
interface JarvisDao {
    @Query("SELECT * FROM (SELECT * FROM messages ORDER BY timestamp DESC LIMIT 200) ORDER BY timestamp ASC")
    fun observeMessages(): kotlinx.coroutines.flow.Flow<List<MessageEntity>>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun observeMemories(): kotlinx.coroutines.flow.Flow<List<MemoryEntity>>

    @Query("SELECT text FROM memories")
    suspend fun memoryTexts(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id NOT IN (SELECT id FROM messages ORDER BY timestamp DESC LIMIT 200)")
    suspend fun trimMessages()

    @Query("DELETE FROM messages WHERE timestamp < :before")
    suspend fun expireMessages(before:Long)

    @Query("DELETE FROM messages")
    suspend fun deleteMessages()

    @Insert
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Query("UPDATE memories SET text = :text WHERE id = :id")
    suspend fun editMemory(id:Long,text:String):Int

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM memories")
    suspend fun deleteMemories()
}

@Database(entities = [MessageEntity::class, MemoryEntity::class], version = 1, exportSchema = false)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun dao(): JarvisDao
}

/** App-private history and explicit memories. Credentials use Keystore encryption separately. */
class JarvisStore(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences = app.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)
    private val credentials = app.getSharedPreferences("jarvis_credentials", Context.MODE_PRIVATE)
    private val database = Room.databaseBuilder(app, JarvisDatabase::class.java, "jarvis_private.db").build()
    private val dao = database.dao()
    private val mutableSettings = MutableStateFlow(readSettings())
    val settings: StateFlow<AssistantSettings> = mutableSettings.asStateFlow()
    private val ephemeral=MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = dao.observeMessages().map { rows ->
        rows.map { row ->
            ChatMessage(
                id = row.id,
                role = runCatching { MessageRole.valueOf(row.role) }.getOrDefault(MessageRole.STATUS),
                text = row.text,
                timestamp = row.timestamp,
                sources = decodeSources(row.sources),
            )
        }
    }.let{persisted->kotlinx.coroutines.flow.combine(persisted,ephemeral){a,b->(a+b).sortedBy{it.timestamp}.takeLast(200)}}.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val memories: StateFlow<List<MemoryItem>> = dao.observeMemories().map { rows ->
        rows.map { MemoryItem(it.id, it.text, it.createdAt) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun addMessage(message: ChatMessage) {
        dao.insertMessage(
            MessageEntity(
                message.id, message.role.name, message.text.take(32_000), message.timestamp,
                JSONArray().apply {
                    message.sources.take(12).forEach { source ->
                        put(JSONObject().put("title", source.title.take(500)).put("url", source.url.take(2048)))
                    }
                }.toString(),
            ),
        )
        dao.trimMessages()
    }

    suspend fun expireHistory(before:Long)=dao.expireMessages(before)

    fun addEphemeral(message:ChatMessage){ephemeral.value=(ephemeral.value+message).takeLast(200)}
    suspend fun clearConversation(){ephemeral.value=emptyList();dao.deleteMessages()}

    /** Called only after the user's explicit remember request or memory-screen action. */
    suspend fun remember(text: String): Long {
        val value = text.trim().take(4_000)
        return if (value.isNotBlank()) dao.insertMemory(MemoryEntity(text = value, createdAt = System.currentTimeMillis())) else -1L
    }

    suspend fun editMemory(id:Long,text:String){val clean=text.trim();require(clean.length in 1..4000);check(dao.editMemory(id,clean)==1){"Original memory no longer exists."}}

    suspend fun forget(id: Long) = dao.deleteMemory(id)

    /** Explicit backup approval only; retries never duplicate the same memory text. */
    suspend fun restoreMemories(items:List<com.jarvis.assistant.neural.RestoredMemory>):Int = database.withTransaction {
        val existing=dao.memoryTexts().toMutableSet()
        var added=0
        items.forEach{item->if(existing.add(item.text)){
            dao.insertMemory(MemoryEntity(text=item.text,createdAt=item.createdAt));added++
        }}
        added
    }

    @Synchronized
    fun updateSettings(value: AssistantSettings) {
        val safe = value.copy(language=if(value.language in AppLanguage.supported)value.language else AppLanguage.EN,name = value.name.trim().take(80), model = value.model.trim().take(120))
        preferences.edit()
            .putString("name", safe.name)
            .putString("language", safe.language.code)
            .putString("provider", safe.provider)
            .putString("model", safe.model)
            .putBoolean("speakReplies", safe.speakReplies)
            .putBoolean("allowNetworkSpeech", safe.allowNetworkSpeech)
            .putBoolean("useCloudVoice", safe.useCloudVoice)
            .putBoolean("shareSavedMemories", safe.shareSavedMemories)
            .putBoolean("allowLockedChat", safe.allowLockedChat)
            .putBoolean("onboardingComplete", safe.onboardingComplete)
            .apply()
        mutableSettings.value = safe
    }

    @Synchronized
    fun getApiKey(provider: String): String {
        val name = credentialName(provider) ?: return ""
        val encoded = credentials.getString(name, null) ?: return ""
        return try {
            val pieces = encoded.split(":", limit = 2)
            if (pieces.size != 2) return ""
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, Base64.decode(pieces[0], Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(pieces[1], Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) {
            // A restored/invalidated key is unavailable, never treated as plaintext.
            ""
        }
    }

    @Synchronized
    fun setApiKey(provider: String, key: String) {
        val name = requireNotNull(credentialName(provider)) { "Unsupported provider" }
        if (key.isBlank()) {
            credentials.edit().remove(name).commit()
            return
        }
        val safeKey = BrainProtocol.validateApiKey(key)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(cipher.doFinal(safeKey.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(credentials.edit().putString(name, "$iv:$encrypted").commit()) { "Could not save credential securely" }
    }

    suspend fun clearAll() {
        ephemeral.value=emptyList()
        database.withTransaction {
            dao.deleteMessages()
            dao.deleteMemories()
        }
        synchronized(this) {
            check(preferences.edit().clear().commit()) { "Settings could not be removed. Please retry the reset." }
            check(credentials.edit().clear().commit()) { "Saved credentials could not be removed. Please retry the reset." }
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEY_ALIAS)
            mutableSettings.value = AssistantSettings()
        }
    }

    private fun encryptionKey(): SecretKey {
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keystore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun readSettings(): AssistantSettings {
        val defaults = AssistantSettings()
        return AssistantSettings(
            name = preferences.getString("name", defaults.name).orEmpty(),
            language = AppLanguage.fromCode(preferences.getString("language", defaults.language.code).orEmpty()),
            provider = preferences.getString("provider", defaults.provider).orEmpty(),
            model = preferences.getString("model", defaults.model).orEmpty(),
            speakReplies = preferences.getBoolean("speakReplies", defaults.speakReplies),
            allowNetworkSpeech = preferences.getBoolean("allowNetworkSpeech", defaults.allowNetworkSpeech),
            useCloudVoice = preferences.getBoolean("useCloudVoice", defaults.useCloudVoice),
            shareSavedMemories = preferences.getBoolean("shareSavedMemories", defaults.shareSavedMemories),
            allowLockedChat = preferences.getBoolean("allowLockedChat", defaults.allowLockedChat),
            onboardingComplete = preferences.getBoolean("onboardingComplete", defaults.onboardingComplete),
        )
    }

    companion object {
        private const val KEY_ALIAS = "jarvis_provider_credentials_v1"
        internal fun credentialName(provider: String): String? = when (provider) {
            "gemini", "openai", "anthropic" -> "key_$provider"
            else -> null
        }

        private fun decodeSources(value: String): List<SourceLink> = runCatching {
            val array = JSONArray(value)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val url = item.optString("url")
                if (!url.startsWith("https://") && !url.startsWith("http://")) return@mapNotNull null
                SourceLink(item.optString("title"), url)
            }
        }.getOrDefault(emptyList())
    }
}
