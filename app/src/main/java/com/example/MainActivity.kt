package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.MovieDatabase
import com.example.data.MovieRepository
import com.example.ui.MovieFinderApp
import com.example.ui.MovieViewModel
import com.example.ui.MovieViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Instantiate local Room persistence stack
    val database = MovieDatabase.getDatabase(this)
    val repository = MovieRepository(
        dao = database.movieMatchDao(),
        userDao = database.appUserDao(),
        deletedDao = database.deletedRecordDao(),
        restoreDao = database.configRestorePointDao()
    )
    val factory = MovieViewModelFactory(repository)

    // Retrieve active ViewModel
    val viewModel = ViewModelProvider(this, factory)[MovieViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      MovieFinderApp(viewModel = viewModel)
    }
  }
}
