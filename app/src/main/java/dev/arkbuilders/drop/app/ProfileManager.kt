package dev.arkbuilders.drop.app

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.app.ui.profile.AvatarUtils
import dev.arkbuilders.drop.app.utils.RandomNameGenerator
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

    private fun load(): Profile? {
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString<Profile>(file.readText())
        }.getOrNull()
    }

    fun loadOrDefault(): Profile {
        var profile = load()
        if (profile == null) {
            profile = Profile(
                name = RandomNameGenerator.generateName(),
                avatarB64 = AvatarUtils.getDefaultAvatarBase64(context)
            )
            save(profile)
        }
        if (profile.avatarB64.isEmpty()) {
            profile.avatarB64 = AvatarUtils.getDefaultAvatarBase64(context)
        }
        return profile
    }

    fun exists(): Boolean = file.exists()

    fun delete() {
        file.delete()
    }

    /**
     * Generate a new random name for the user
     */
    fun generateNewName(): String {
        val currentProfile = loadOrDefault()
        return RandomNameGenerator.generateDifferentName(currentProfile.name)
    }

    /**
     * Update the profile with a new random name
     */
    fun updateToRandomName(): Profile {
        val currentProfile = loadOrDefault()
        val newName = generateNewName()

        val updatedProfile = currentProfile.copy(name = newName)
        save(updatedProfile)

        return updatedProfile
    }

    /**
     * Check if the current profile name is a generated name
     */
    fun hasGeneratedName(): Boolean {
        val profile = loadOrDefault()
        return RandomNameGenerator.isValidGeneratedName(profile.name)
    }

    /**
     * Get suggestions for new names
     */
    fun getNameSuggestions(count: Int = 5): List<String> {
        val currentProfile = loadOrDefault()
        val suggestions = RandomNameGenerator.generateUniqueNames(count + 1)

        // Remove current name if it appears in suggestions
        return suggestions.filter { it != currentProfile.name }.take(count)
    }
}