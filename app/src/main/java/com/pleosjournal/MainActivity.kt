package com.pleosjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pleosjournal.presentation.JournalViewModel
import com.pleosjournal.ui.JournalScreen

/** 배선만 한다. 화면은 ui/, 상태와 동작은 presentation/ 에 있다. */
class MainActivity : ComponentActivity() {
    // by viewModels(): 화면이 다시 만들어져도 같은 ViewModel을 돌려준다.
    private val viewModel: JournalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // collectAsState: StateFlow의 최신 값을 Compose 상태로 바꾼다. 값이 바뀌면 화면이 다시 그려진다.
            val state by viewModel.state.collectAsState()
            JournalScreen(state, viewModel)
        }
    }
}
