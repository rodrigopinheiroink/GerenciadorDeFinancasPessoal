package com.example.minhasfinancas.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.minhasfinancas.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ProfilePreferences {

    private const val PREFS_NAME = "profiles_config"
    private const val KEY_PROFILES_JSON = "profiles_list_json"
    private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAllProfiles(context: Context): List<UserProfile> {
        val jsonStr = getPrefs(context).getString(KEY_PROFILES_JSON, null) ?: return emptyList()
        val list = mutableListOf<UserProfile>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val photo = if (obj.has("photoUri") && !obj.isNull("photoUri")) obj.optString("photoUri").takeIf { it.isNotBlank() } else null
                list.add(
                    UserProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        photoUri = photo,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {
            // fallback se houver erro de parsing
        }
        return list
    }

    private fun saveProfiles(context: Context, profiles: List<UserProfile>) {
        val jsonArray = JSONArray()
        for (p in profiles) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("photoUri", p.photoUri)
                put("createdAt", p.createdAt)
            }
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_PROFILES_JSON, jsonArray.toString()).apply()
    }

    fun getActiveProfile(context: Context): UserProfile? {
        val profiles = getAllProfiles(context)
        if (profiles.isEmpty()) return null

        val activeId = getPrefs(context).getString(KEY_ACTIVE_PROFILE_ID, null)
        return profiles.find { it.id == activeId } ?: profiles.firstOrNull()
    }

    fun setActiveProfileId(context: Context, profileId: String) {
        getPrefs(context).edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).apply()
    }

    fun createProfile(context: Context, name: String): UserProfile {
        val profiles = getAllProfiles(context).toMutableList()
        val newProfile = UserProfile(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Perfil ${profiles.size + 1}" },
            createdAt = System.currentTimeMillis()
        )
        profiles.add(newProfile)
        saveProfiles(context, profiles)
        setActiveProfileId(context, newProfile.id)
        return newProfile
    }

    fun updateProfileName(context: Context, profileId: String, newName: String) {
        val profiles = getAllProfiles(context).map {
            if (it.id == profileId) it.copy(name = newName.trim().ifBlank { it.name }) else it
        }
        saveProfiles(context, profiles)
    }

    fun updateProfilePhoto(context: Context, profileId: String, photoUri: String?) {
        val profiles = getAllProfiles(context).map {
            if (it.id == profileId) it.copy(photoUri = photoUri) else it
        }
        saveProfiles(context, profiles)
    }

    fun saveImportedProfile(context: Context, imported: UserProfile): UserProfile {
        val profiles = getAllProfiles(context).toMutableList()
        val existingIndex = profiles.indexOfFirst { it.id == imported.id }
        if (existingIndex != -1) {
            profiles[existingIndex] = imported
        } else {
            profiles.add(imported)
        }
        saveProfiles(context, profiles)
        setActiveProfileId(context, imported.id)
        return imported
    }

    fun deleteProfile(context: Context, profileId: String): UserProfile? {
        val profiles = getAllProfiles(context).toMutableList()
        val index = profiles.indexOfFirst { it.id == profileId }
        if (index != -1) {
            profiles.removeAt(index)
            saveProfiles(context, profiles)

            // Se o perfil excluído era o ativo, define outro perfil como ativo
            val currentActiveId = getPrefs(context).getString(KEY_ACTIVE_PROFILE_ID, null)
            val nextActive = if (currentActiveId == profileId) {
                profiles.firstOrNull()
            } else {
                profiles.find { it.id == currentActiveId } ?: profiles.firstOrNull()
            }

            if (nextActive != null) {
                setActiveProfileId(context, nextActive.id)
            } else {
                getPrefs(context).edit().remove(KEY_ACTIVE_PROFILE_ID).apply()
            }
            return nextActive
        }
        return getActiveProfile(context)
    }
}
