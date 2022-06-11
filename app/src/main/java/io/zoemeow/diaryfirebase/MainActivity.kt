package io.zoemeow.diaryfirebase

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.database.*
import io.zoemeow.diaryfirebase.model.Note
import io.zoemeow.diaryfirebase.ui.theme.DiaryFirebaseTheme
import io.zoemeow.diaryfirebase.utils.getCurrentUnixTime
import io.zoemeow.diaryfirebase.viewmodel.MainViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiaryFirebaseTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val snackBarHostState = remember { SnackbarHostState() }
    val mainViewModel = viewModel<MainViewModel>()
    val scope = rememberCoroutineScope()
    mainViewModel.setContext(LocalContext.current)
    mainViewModel.setSnackBarHostState(snackBarHostState)
    val addNoteDialogShow = remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            SmallTopAppBar(
                title = {
                    Text("Diary App")
                },
                actions = {
                    if (mainViewModel.loggedIn.value) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_logout_24),
                            contentDescription = "Logout",
                            modifier = Modifier
                                .clickable {
                                    mainViewModel.logout()
                                }
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (mainViewModel.loggedIn.value) {
                FloatingActionButton(
                    onClick = {
                        addNoteDialogShow.value = true
                    },
                    content = {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_baseline_add_24), contentDescription = "")
                    }
                )
            }
        },
        content = { paddingValue ->
            if (!mainViewModel.loggedIn.value) {
                LoginScreen {user, pass ->
                    mainViewModel.login(user, pass,
                        onSuccessful = { },
                        onFailed = { },
                    )
                }
            }
            else {
                mainViewModel.readDataFromRealtimeDatabase()
                LazyColumn(modifier = Modifier.padding(paddingValue)) {
                    items(mainViewModel.noteData.noteList.sortedByDescending { it.date }) {
                            item -> NoteDisplay(item)
                    }
                }
            }
        }
    )

    AddNoteDialog(
        enabled = addNoteDialogShow,
        addNoteRequested = { mainViewModel.addData(it) }
    )
}

@Composable
fun LoginScreen(loginRequest: (user: String, pass: String) -> Unit) {
    val user = remember { mutableStateOf("") }
    val pass = remember { mutableStateOf("") }
    val passTextFieldFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Login",
            style = MaterialTheme.typography.headlineMedium
        )
        OutlinedTextField(
            label = { Text("Username") },
            value = user.value,
            onValueChange = { user.value = it },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onGo = { passTextFieldFocusRequester.requestFocus() }
            ),
        )
        OutlinedTextField(
            modifier = Modifier
                .focusRequester(passTextFieldFocusRequester),
            label = { Text("Password") },
            value = pass.value,
            onValueChange = { pass.value = it },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (user.value.isNotEmpty() && pass.value.isNotEmpty()) {
                        focusManager.clearFocus()
                        loginRequest(user.value, pass.value)
                    }
                }
            )
        )
        Spacer(modifier = Modifier.size(10.dp))
        Button(
            onClick = {
                if (user.value.isNotEmpty() && pass.value.isNotEmpty()) {
                    focusManager.clearFocus()
                    loginRequest(user.value, pass.value)
                }
            },
            content = {
                Text("Login")
            }
        )
        Spacer(modifier = Modifier.size(5.dp))
        Button(
            onClick = {

            },
            content = {
                Text("Register")
            }
        )
    }
}

@Composable
fun NoteDisplay(note: Note) {
    Column(modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 5.dp, bottom = 5.dp)) {
        Text(
            DateToString(note.date),
            style = MaterialTheme.typography.labelLarge
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp, bottom = 5.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddNoteDialog(
    enabled: MutableState<Boolean>,
    addNoteRequested: (Note) -> Unit
) {
    val title = remember { mutableStateOf("") }
    val content = remember { mutableStateOf("") }
    val passTextFieldFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    if (enabled.value) {
        AlertDialog(
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
            modifier = Modifier
                .padding(15.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            onDismissRequest = { enabled.value = false },
            title = { Text("Add note") },
            text = {
                Column() {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = title.value,
                        onValueChange = { title.value = it },
                        label = {
                            Text("Title")
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onGo = { passTextFieldFocusRequester.requestFocus() }
                        ),
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(passTextFieldFocusRequester),
                        value = content.value,
                        onValueChange = { content.value = it },
                        label = {
                            Text("Content")
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { enabled.value = false },
                    content = {
                        Text("Cancel")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        passTextFieldFocusRequester.freeFocus()
                        focusManager.clearFocus()
                        enabled.value = false
                        val note = Note(
                            date = getCurrentUnixTime(),
                            title = title.value,
                            content = content.value
                        )
                        addNoteRequested(note)
                        title.value = String()
                        content.value = String()
                    },
                    content = {
                        Text("Add")
                    }
                )
            },
        )
    }
}

fun DateToString(unix: Long): String {
    var unixDuration = (getCurrentUnixTime() - unix) / 1000

    if (unixDuration < 1) {
        return "Just now"
    }
    else if (unixDuration < 60) {
        val unixSec = unixDuration % 60
        return "${unixSec} second${if (unixSec.toInt() != 1) "s" else ""} ago"
    }
    else if (unixDuration < 60*60) {
        val unixMin = unixDuration / 60
        return "${unixMin} minute${if (unixMin.toInt() != 1) "s" else ""} ago"
    }
    else if (unixDuration < 60*60*24) {
        val unixHr = unixDuration / 60 / 60
        return "${unixHr} hour${if (unixHr.toInt() != 1) "s" else ""} ago"
    }
    else if (unixDuration < 60*60*24*7) {
        val unixDay = unixDuration / 60 / 60 / 24
        return "${unixDay} day${if (unixDay.toInt() != 1) "s" else ""} ago"
    }
    else return getDateString(unix, "yyyy/MM/dd")
}

@SuppressLint("SimpleDateFormat")
fun getDateString(date: Long, dateFormat: String): String {
    // "dd/MM/yyyy"
    // "dd/MM/yyyy HH:mm"
    val simpleDateFormat = SimpleDateFormat(dateFormat)
    return simpleDateFormat.format(Date(date))
}