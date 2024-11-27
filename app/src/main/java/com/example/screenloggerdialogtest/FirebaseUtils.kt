package com.example.screenloggerdialogtest

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object FirebaseUtils {

    // Reference to the Firebase Realtime Database
    val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val databaseReference = database.reference // Root of the database

    /**
     * Function to send an entry to the Realtime Database
     * @param path The path in the database where the data will be saved
     * @param data The data to be sent (can be any data object, e.g., HashMap, data class, etc.)
     */
    fun sendEntryToDatabase(path: String, data: Any, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        databaseReference.child(path).setValue(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun authenticateUser(onSuccess: (String?) -> Unit, onFailure: (String?) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Authentication succeeded, pass UID to onSuccess callback
                    val user = auth.currentUser
                    onSuccess(user?.uid)
                } else {
                    // Authentication failed, pass error message to onFailure callback
                    onFailure(task.exception?.message)
                }
            }
    }

    fun getCurrentUserUID(): String? {
        return auth.currentUser?.uid
    }

    fun calculateUsage(
        userId: String,
        data: Map<String, Any>,
        selectedDay: LocalDateTime? = null
    ): Map<String, Long> {
        var totalUsageAllDays: Long = 0
        var totalUsageSelectedDay: Long = 0

        // Loop through all events and calculate total usage
        val screenEvents = data["screen"] as Map<String, Map<String, Any>>

        for (eventEntry in screenEvents.values) {
            val event = eventEntry as Map<String, Any>
            val duration = event["duration"] as Long
            val timestamp = event["timestamp_event_start"] as Long
            val eventType = event["type"] as String
            val eventTime = convertTimestampToLocalDateTime(timestamp)

            // Add to total usage for all days
            if (eventType == "ACTION_USER_PRESENT") {
                totalUsageAllDays += duration

                // If selected day is provided, check if event falls within the selected day's range
                selectedDay?.let {
                    val dayStart = selectedDay.withHour(4).withMinute(0).withSecond(0).withNano(0)
                    val dayEnd = dayStart.plusDays(1) // Next day at 4 AM

                    if (eventTime.isAfter(dayStart) && eventTime.isBefore(dayEnd)) {
                        totalUsageSelectedDay += duration
                    }
                }
            }
        }

        return mapOf(
            "totalUsageAllDays" to totalUsageAllDays,
            "totalUsageSelectedDay" to totalUsageSelectedDay
        )
    }

    fun convertTimestampToLocalDateTime(timestamp: Long): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    }

    fun fetchUsageTotal(selectedDay: LocalDateTime, callback: (Map<String, Long>) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/${getCurrentUserUID()}")

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Assuming that the data is fetched from Firebase
                val data = snapshot.value as? Map<String, Any> ?: run {
                    // Handle the case where snapshot.value is null or not of the expected type
                    Log.e("FIREBASE", "Snapshot value is null or not a Map")
                    return@run null  // Return null or an empty map or whatever is appropriate
                }

                // Only proceed if data is valid
                data?.let {
                    val usage = getCurrentUserUID()?.let { userId ->
                        calculateUsage(userId, data, selectedDay)
                    }
                    usage?.let {
                        callback(it)
                    } ?: Log.e("FIREBASE", "Failed to calculate usage.")
                }

            }

            override fun onCancelled(error: DatabaseError) {
                println("Failed to read value: ${error.toException()}")
            }

        })

    }

    // IMPORTANT
    // timestamp is when the event started
    data class ScreenEvent(
        val type: String,
        val timestamp_event_start: Long = 0L,  // Timestamp in milliseconds (e.g., System.currentTimeMillis())
        val duration: Long = 0L    // Duration in milliseconds (e.g., screen-on time)
    )

}