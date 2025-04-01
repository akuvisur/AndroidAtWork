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
import kotlin.math.log
import kotlin.random.Random

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

    data class DailyUsageResult(
        val dailyUsage: Map<String, Long>,
        val errorCounts: Map<String, Int>
    )

    fun calculateUsagePerDay(data: Map<String, Any>): DailyUsageResult {
        val dailyUsage = mutableMapOf<String, Long>()
        val errorCounts = mutableMapOf<String, Int>()

        if (data.isEmpty()) return DailyUsageResult(dailyUsage, errorCounts)

        // Loop through all events and calculate total usage per day
        val screenEvents = data["screen"] as Map<String, Map<String, Any>>

        for (eventEntry in screenEvents.values) {
            val event = eventEntry as Map<String, Any>
            var duration = event["duration"] as Long
            val timestamp = event["timestamp_event_start"] as Long
            val eventType = event["type"] as String
            val eventTime = convertTimestampToLocalDateTime(timestamp)

            // Only consider "ACTION_USER_PRESENT" events for usage
            if (eventType == "ACTION_USER_PRESENT") {
                // Use a 4 AM cutoff for each day
                val adjustedDate = if (eventTime.hour < 4) {
                    eventTime.toLocalDate().minusDays(1) // Consider it part of the previous day
                } else {
                    eventTime.toLocalDate()
                }

                val dateKey = adjustedDate.toString()

                // Default duration to two minutes if it's longer than one hour
                if (duration > 3_600_000) {
                    duration = 180_000 // Three minutes in milliseconds
                    // Increment the error count for this day
                    errorCounts[dateKey] = errorCounts.getOrDefault(dateKey, 0) + 1
                }

                // Add the duration to the respective day
                dailyUsage[dateKey] = dailyUsage.getOrDefault(dateKey, 0L) + duration
            }
        }

        return DailyUsageResult(dailyUsage, errorCounts)
    }

    fun convertTimestampToLocalDateTime(timestamp: Long): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    }

    fun fetchUsageTotal(selectedDay: LocalDateTime, callback: (Map<String, Long>) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/${getCurrentUserUID()}")

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as? Map<String, Any> ?: run {
                    Log.e("FIREBASE", "Snapshot value is null or not a Map")
                    return  // Stop further execution
                }

                val usageResult = try {
                    getCurrentUserUID()?.let { userId ->
                        calculateUsagePerDay(data)
                    }
                } catch (e: Exception) {
                    Log.e("FIREBASE", "Error during usage calculation: ${e.message}")
                    null
                }

                Log.d("UsageResult", usageResult.toString())

                usageResult?.let { result ->
                    // Fetch all verification statuses
                    fetchAllVerificationStatuses { verificationStatuses ->
                        // Filter out usage for days where verification status is false
                        val filteredUsage = result.dailyUsage.filter { (date, _) ->
                            verificationStatuses[date] != false
                        }

                        Log.d("UsageResultFiltered", filteredUsage.toString())
                        callback(filteredUsage)
                    }
                } ?: Log.e("FIREBASE", "Failed to calculate usage.")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Failed to read value: ${error.toException()}")
            }
        })
    }


    fun fetchVerificationStatusForPreviousDay(callback: (Boolean?) -> Unit) {
        val previousDay = LocalDateTime.now().minusDays(1).toLocalDate().toString()
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/${getCurrentUserUID()}/data_verification/$previousDay")

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(Boolean::class.java)
                callback(status)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Failed to read verification status: ${error.toException()}")
                callback(null)
            }
        })
    }

    fun fetchAllVerificationStatuses(callback: (HashMap<String, Boolean>) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/${getCurrentUserUID()}/data_verification")

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val verificationStatuses = HashMap<String, Boolean>()

                // Iterate over each child in the snapshot
                for (childSnapshot in snapshot.children) {
                    val date = childSnapshot.key ?: continue
                    val status = childSnapshot.getValue(Boolean::class.java)

                    // Only add to the map if the status is not null
                    status?.let {
                        verificationStatuses[date] = it
                    }
                }

                Log.d("verification", "all verification statuses: " + verificationStatuses.toString())
                callback(verificationStatuses)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Failed to read verification statuses: ${error.toException()}")
                callback(HashMap())
            }
        })
    }


    fun storeVerificationStatusForPreviousDay(status: Boolean) {
        val previousDay = LocalDateTime.now().minusDays(1).toLocalDate().toString()
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/${getCurrentUserUID()}/data_verification/$previousDay")

        myRef.setValue(status)
            .addOnSuccessListener {
                Log.d("FIREBASE", "Verification status stored successfully.")
            }
            .addOnFailureListener {
                Log.e("FIREBASE", "Failed to store verification status: ${it.message}")
            }
    }

    // testing functions
    fun randomizeAllVerificationStatuses() {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/${getCurrentUserUID()}/data_verification")

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val verificationStatuses = HashMap<String, Boolean>()

                // Iterate over each child in the snapshot
                for (childSnapshot in snapshot.children) {
                    val date = childSnapshot.key ?: continue
                    val randomStatus = Random.nextBoolean()
                    verificationStatuses[date] = randomStatus
                }

                // Update the database with randomized statuses
                for ((date, status) in verificationStatuses) {
                    myRef.child(date).setValue(status)
                        .addOnSuccessListener {
                            Log.d("FIREBASE", "Randomized verification status for $date to $status")
                        }
                        .addOnFailureListener {
                            Log.e("FIREBASE", "Failed to update verification status for $date: ${it.message}")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Failed to read verification statuses: ${error.toException()}")
            }
        })
    }


    fun deleteYesterdaysVerificationStatus() {
        val previousDay = LocalDateTime.now().minusDays(1).toLocalDate().toString()
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/${getCurrentUserUID()}/data_verification/$previousDay")

        myRef.removeValue()
            .addOnSuccessListener {
                Log.d("FIREBASE", "Yesterday's verification status deleted successfully.")
            }
            .addOnFailureListener {
                Log.e("FIREBASE", "Failed to delete yesterday's verification status: ${it.message}")
            }
    }



    fun storePreviousDayEstimate(estimateMinutes: Int) {
        val previousDay = LocalDateTime.now().minusDays(1).toLocalDate().toString()
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/${getCurrentUserUID()}/usage_estimates/$previousDay")

        myRef.setValue(estimateMinutes)
            .addOnSuccessListener {
                Log.d("FIREBASE", "Usage estimate stored successfully.")
            }
            .addOnFailureListener {
                Log.e("FIREBASE", "Failed to store usage estimate: ${it.message}")
            }
    }

    fun fetchPreviousDayEstimate(callback: (Int?) -> Unit) {
        val previousDay = LocalDateTime.now().minusDays(1).toLocalDate().toString()
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/${getCurrentUserUID()}/usage_estimates/$previousDay")

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val estimateMinutes = snapshot.getValue(Int::class.java)
                callback(estimateMinutes)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Failed to read usage estimate: ${error.toException()}")
                callback(null)
            }
        })
    }

    fun deleteYesterdaysUsageEstimate() {
        val previousDay = LocalDateTime.now().minusDays(1).toLocalDate().toString()
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/${getCurrentUserUID()}/usage_estimates/$previousDay")

        myRef.removeValue()
            .addOnSuccessListener {
                Log.d("FIREBASE", "Yesterday's usage estimate deleted successfully.")
            }
            .addOnFailureListener {
                Log.e("FIREBASE", "Failed to delete yesterday's usage estimate: ${it.message}")
            }
    }


    // IMPORTANT
    // timestamp is when the event started
    data class ScreenEvent(
        var type: String,
        var timestamp_event_start: Long = 0L,  // Timestamp in milliseconds (e.g., System.currentTimeMillis())
        var duration: Long = 0L    // Duration in milliseconds (e.g., screen-on time)
    )

    fun uploadFirebaseEntry(path : String, data : FirebaseDataLoggingObject) {
        sendEntryToDatabase(
            path = path, // Path in the database (e.g., "users/user_1")
            data = data,
            onSuccess = {
            },
            onFailure = { exception ->
            }
        )
    }

    data class FirebaseDataLoggingObject(
        val event: String
    )

    fun uploadFeedback(path : String, data : FirebaseFeedbackDataObject) {
        sendEntryToDatabase(
            path = path, // Path in the database (e.g., "users/user_1")
            data = data,
            onSuccess = {
            },
            onFailure = { exception ->
            }
        )
    }

    data class FirebaseFeedbackDataObject(
        val feedback: String
    )

}