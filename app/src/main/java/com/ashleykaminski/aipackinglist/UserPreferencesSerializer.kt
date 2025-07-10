package com.ashleykaminski.aipackinglist

import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object UserPreferencesSerializer : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences() // Default empty state

    // In UserPreferencesSerializer
    // UserPreferencesSerializer.kt
    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        Log.d("ID_DEBUG", "Serializer.writeTo: UserPreferences.nextItemId = ${t.nextItemId}, All UserPrefs: $t")
        output.write(
            Json.encodeToString(UserPreferences.serializer(), t).encodeToByteArray()
        )
    }

    override suspend fun readFrom(input: InputStream): UserPreferences {
        try {
            val jsonString = input.readBytes().decodeToString()
            Log.d("ID_DEBUG", "Serializer.readFrom: Raw JSON String = $jsonString")
            val prefs = Json.decodeFromString(
                UserPreferences.serializer(), jsonString
            )
            Log.d("ID_DEBUG", "Serializer.readFrom: Decoded UserPreferences.nextItemId = ${prefs.nextItemId}, All UserPrefs: $prefs")
            return prefs
        } catch (exception: SerializationException) {
            Log.e("ID_DEBUG", "Serializer.readFrom: SerializationException", exception)
            throw CorruptionException("Cannot read proto.", exception)
        }  catch (e: java.io.IOException) {
            Log.e("ID_DEBUG", "Serializer.readFrom: IOException", e)
            throw e
        }
    }
}