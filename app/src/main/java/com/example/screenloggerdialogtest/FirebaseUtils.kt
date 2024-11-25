package com.example.screenloggerdialogtest

import com.google.firebase.database.FirebaseDatabase

object FirebaseUtils {

    // Reference to the Firebase Realtime Database
    val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
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
}