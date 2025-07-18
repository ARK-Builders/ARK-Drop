package dev.arkbuilders.drop.app

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.app.ui.profile.AvatarUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Profile(
    var name: String,
    var avatarB64: String,
)

@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val file = File(context.filesDir, "profile.json")
    private val json = Json { prettyPrint = true }

    fun save(profile: Profile) {
        val encodedProfile: String = json.encodeToString(Profile.serializer(), profile)
        file.writeText(encodedProfile)
    }

    fun load(): Profile? {
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString<Profile>(file.readText())
        }.getOrNull()
    }

    fun loadOrDefault(): Profile {
        return load() ?: Profile(
            name = "User",
            avatarB64 = AvatarUtils.getDefaultAvatarBase64(context)
        )
    }

    fun exists(): Boolean = file.exists()

    fun delete() {
        file.delete()
    }
}
