package io.zoemeow.diaryfirebase.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import io.zoemeow.diaryfirebase.data.NoteData
import io.zoemeow.diaryfirebase.model.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    var noteData = NoteData()

    internal val loggedIn: MutableState<Boolean> = mutableStateOf(false)

    private val mainActivitySnackBarHostState: MutableState<SnackbarHostState?> = mutableStateOf(null)
    fun setSnackBarHostState(item: SnackbarHostState) {
        mainActivitySnackBarHostState.value = item
    }

    // Get context main activity
    private val mainActivityContext: MutableState<Context?> = mutableStateOf(null)
    fun setContext(item: Context) {
        mainActivityContext.value = item
    }

    private var auth: FirebaseAuth = Firebase.auth
    private var database: FirebaseDatabase = Firebase.database

    fun login(
        user: String, pass: String,
        onSuccessful: () -> Unit,
        onFailed: () -> Unit
    ) {
        viewModelScope.launch {
            mainActivitySnackBarHostState.value?.currentSnackbarData?.dismiss()
            mainActivitySnackBarHostState.value?.showSnackbar("Logging in...")
        }
        auth.signInWithEmailAndPassword(user, pass)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    // TODO: Successful login here!
                    onSuccessful()

                    viewModelScope.launch {
                        mainActivitySnackBarHostState.value?.currentSnackbarData?.dismiss()
                        mainActivitySnackBarHostState.value?.showSnackbar("Successfully login!")
                    }
                }
                else {
                    // TODO: Failed login here!
                    onFailed()
                }
            }
    }

    fun logout() {
        auth.signOut()
        loggedIn.value = false

        viewModelScope.launch {
            mainActivitySnackBarHostState.value?.currentSnackbarData?.dismiss()
            mainActivitySnackBarHostState.value?.showSnackbar("Successfully logout!")
        }
    }

    fun readDataFromRealtimeDatabase() {
        val onlineUserId = auth.currentUser?.uid.toString()
        database.reference.child("note").child(onlineUserId)
        .addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                noteData.deleteAllNotes()
                for (userSnapshot in snapshot.children){
                    val data = userSnapshot.getValue(Note::class.java)
                    if (data != null) {
                        noteData.addNote(data)
                        Log.d(
                            "Check",
                            "io.zoemeow.diaryfirebase: ${userSnapshot.key}-${data.title}-${data.content}"
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun checkUserLoggedIn() {
        auth.addAuthStateListener {
            loggedIn.value = (it.currentUser != null)
        }
    }

    fun addData(note: Note) {
        val myRefRoot = database.reference
        val onlineUserId = auth.currentUser?.uid.toString()
        myRefRoot.child("note").child(onlineUserId).child(note.date.toString()).setValue(note)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    viewModelScope.launch {
                        mainActivitySnackBarHostState.value?.currentSnackbarData?.dismiss()
                        mainActivitySnackBarHostState.value?.showSnackbar("Note added.")
                    }
                }
                else {
                    // TODO: Failed here!
                }
            }
    }

//    private fun postDataToFireStore() {
//// Create a new user with a first and last name
//        val user = hashMapOf(
//            "first" to "Ada",
//            "last" to "Lovelace",
//            "born" to 1815
//        )
//
//// Add a new document with a generated ID
//        firestore.collection("users")
//            .add(user)
//            .addOnSuccessListener { documentReference ->
//                Log.d(ContentValues.TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
//            }
//            .addOnFailureListener { e ->
//                Log.w(ContentValues.TAG, "Error adding document", e)
//            }
//    }

    init {
        readDataFromRealtimeDatabase()

        checkUserLoggedIn()
//        login("user01@gmail.com", "cloney1301")
//        readDataFromRealtimeDB()
//        // postDataToFireStore()
//        addData(Note(date = unixTime, title = "123", content = "456"), onSuccessful = {}, onFailed = {})
//        // addPostData(Note(title = "789", content = "012"))
    }
}