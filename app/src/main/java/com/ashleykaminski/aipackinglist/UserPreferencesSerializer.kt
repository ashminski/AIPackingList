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
    override suspend fun readFrom(input: InputStream): UserPreferences {
        try {
            Log.d("DataStoreDebug", "Reading from DataStore...")
            val data = Json.decodeFromString(
                UserPreferences.serializer(), input.readBytes().decodeToString()
            )
            Log.d("DataStoreDebug", "Read data: $data")
            return data
        } catch (exception: SerializationException) {
            Log.e("DataStoreDebug", "Error reading DataStore", exception)
            throw CorruptionException("Cannot read proto.", exception)
        }  catch (e: java.io.IOException) {
            Log.e("DataStoreDebug", "IOException reading DataStore", e)
            throw e
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        Log.d("DataStoreDebug", "Writing to DataStore: $t")
        output.write(
            Json.encodeToString(UserPreferences.serializer(), t).encodeToByteArray()
        )
    }
}