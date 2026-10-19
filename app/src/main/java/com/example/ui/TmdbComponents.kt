package com.example.ui

import android.widget.Toast
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGoldLight
import com.example.ui.theme.ElectricCyan
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.MovieMatch
import com.example.data.api.TmdbMovie

@Composable
fun TmdbMoviePoster(
    posterUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    if (posterUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Poster for $title",
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CinemaGold
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TmdbLiveSearchSection(
    viewModel: MovieViewModel,
    context: Context
) {
    var searchQuery by remember { mutableStateOf("") }
    var isFilterExpanded by remember { mutableStateOf(false) }
    var selectedMovieForDetail by remember { mutableStateOf<TmdbMovie?>(null) }

    val selectedYear = viewModel.selectedYearFilter
    val selectedGenre = viewModel.selectedGenreFilter
    val selectedRating = viewModel.selectedMinRatingFilter

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, CinemaGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .testTag("tmdb_live_search_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = CinemaGoldLight, modifier = Modifier.size(20.dp))
                    Text(
                        text = "LIVE TMDB SEARCH & FILTERS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = CinemaGoldLight,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = { isFilterExpanded = !isFilterExpanded },
                    modifier = Modifier.size(32.dp).testTag("toggle_search_filters_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Search Filters",
                        tint = if (selectedYear != null || selectedGenre != null || selectedRating != null) CinemaGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Search text box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchTmdbMovies(it)
                },
                placeholder = { Text("Search movies, TV shows, actors on TMDB...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tmdb_search_input"),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.searchTmdbMovies("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.searchTmdbMovies(searchQuery)
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CinemaGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Filter Chips Section
            AnimatedVisibility(visible = isFilterExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Filter Chips (Release Year, Genre, Rating)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CinemaGoldLight)

                    // Release Year Chips
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Year:", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                        listOf(null, 2025, 2024, 2023, 2022, 2020, 2010, 2000).forEach { year ->
                            val label = year?.toString() ?: "All"
                            FilterChip(
                                selected = selectedYear == year,
                                onClick = {
                                    viewModel.selectedYearFilter = year
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.searchTmdbMovies(searchQuery)
                                    } else {
                                        viewModel.fetchDiscoverMovies()
                                    }
                                },
                                label = { Text(label, fontSize = 9.5.sp) },
                                shape = RoundedCornerShape(50.dp)
                            )
                        }
                    }

                    // Minimum Rating Chips
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Min Rating:", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                        listOf(null, 8.0f, 7.0f, 6.0f).forEach { rating ->
                            val label = if (rating != null) "⭐ ${rating.toInt()}+" else "Any"
                            FilterChip(
                                selected = selectedRating == rating,
                                onClick = {
                                    viewModel.selectedMinRatingFilter = rating
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.searchTmdbMovies(searchQuery)
                                    } else {
                                        viewModel.fetchDiscoverMovies()
                                    }
                                },
                                label = { Text(label, fontSize = 9.5.sp) },
                                shape = RoundedCornerShape(50.dp)
                            )
                        }
                    }
                }
            }

            // Results List
            if (viewModel.isTmdbLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CinemaGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching TMDB multi-search results...", fontSize = 11.sp, color = CinemaGoldLight)
                }
            } else if (viewModel.tmdbSearchErrorMessage != null && viewModel.searchResultsState.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = CinemaGoldLight, modifier = Modifier.size(18.dp))
                        Text(
                            text = viewModel.tmdbSearchErrorMessage!!,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            } else if (viewModel.searchResultsState.isNotEmpty()) {
                Text(
                    text = "${viewModel.searchResultsState.size} TMDB MATCHES FOUND",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("tmdb_search_results_row")
                ) {
                    items(viewModel.searchResultsState, key = { it.id }) { movie ->
                        TmdbMovieCardItem(
                            movie = movie,
                            onSaveWatchlist = {
                                val match = MovieMatch(
                                    title = movie.displayTitle,
                                    year = movie.displayYear,
                                    director = "TMDB Verified",
                                    imdbUrl = "https://www.themoviedb.org/movie/${movie.id}",
                                    confidence = "High",
                                    rationale = movie.overview?.take(140) ?: "TMDB Database Search Match",
                                    actors = "TMDB Live Metadata",
                                    genre = "TMDB Movie",
                                    searchType = "TEXT",
                                    isInWatchlist = true,
                                    posterPath = movie.fullPosterUrl
                                )
                                viewModel.toggleWatchlist(match)
                            },
                            onOpenTmdb = {
                                selectedMovieForDetail = movie
                            }
                        )
                    }
                }
            }
        }
    }

    val movieToDisplay = selectedMovieForDetail
    if (movieToDisplay != null) {
        TmdbMovieDetailDialog(
            movie = movieToDisplay,
            viewModel = viewModel,
            context = context,
            onDismiss = { selectedMovieForDetail = null }
        )
    }
}

@Composable
fun TmdbMovieCardItem(
    movie: TmdbMovie,
    onSaveWatchlist: () -> Unit,
    onOpenTmdb: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable { onOpenTmdb() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                TmdbMoviePoster(
                    posterUrl = movie.fullPosterUrl,
                    title = movie.displayTitle,
                    modifier = Modifier.fillMaxSize()
                )

                if ((movie.vote_average ?: 0.0) > 0) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⭐ " + String.format("%.1f", movie.vote_average),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = movie.displayTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = movie.displayYear,
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Button(
                    onClick = onSaveWatchlist,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(26.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(10.dp))
                        Text("Watchlist", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TmdbTrendingSection(
    viewModel: MovieViewModel,
    context: Context
) {
    val trendingList = viewModel.trendingMoviesState
    var selectedMovieForDetail by remember { mutableStateOf<TmdbMovie?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().testTag("tmdb_trending_section")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🔥", fontSize = 16.sp)
                Text(
                    text = "TMDB TRENDING MOVIES TODAY",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = CinemaGoldLight,
                    letterSpacing = 0.5.sp
                )
            }

            TextButton(
                onClick = { viewModel.fetchTrendingMovies() },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Refresh", fontSize = 10.sp, color = CinemaGold)
            }
        }

        if (viewModel.isTmdbLoading && trendingList.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CinemaGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Loading TMDB live trending feed...", fontSize = 11.sp, color = CinemaGoldLight)
            }
        } else if (viewModel.tmdbErrorMessage != null && trendingList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = CinemaGoldLight, modifier = Modifier.size(20.dp))
                        Text(
                            text = viewModel.tmdbErrorMessage!!,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = { viewModel.fetchTrendingMovies() },
                        colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Retry", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (trendingList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Movie, contentDescription = null, tint = CinemaGoldLight, modifier = Modifier.size(18.dp))
                    Text("No trending movies currently loaded.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    TextButton(onClick = { viewModel.fetchTrendingMovies() }) {
                        Text("Refresh", fontSize = 11.sp, color = CinemaGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(trendingList, key = { it.id }) { movie ->
                    TmdbMovieCardItem(
                        movie = movie,
                        onSaveWatchlist = {
                            val match = MovieMatch(
                                title = movie.displayTitle,
                                year = movie.displayYear,
                                director = "TMDB Trending",
                                imdbUrl = "https://www.themoviedb.org/movie/${movie.id}",
                                confidence = "High",
                                rationale = movie.overview?.take(140) ?: "Popular movie trending worldwide on TMDB today.",
                                actors = "Live TMDB Trending",
                                genre = "Trending",
                                searchType = "TEXT",
                                isInWatchlist = true,
                                posterPath = movie.fullPosterUrl
                            )
                            viewModel.toggleWatchlist(match)
                        },
                        onOpenTmdb = {
                            selectedMovieForDetail = movie
                        }
                    )
                }
            }
        }
    }

    val movieToDisplay = selectedMovieForDetail
    if (movieToDisplay != null) {
        TmdbMovieDetailDialog(
            movie = movieToDisplay,
            viewModel = viewModel,
            context = context,
            onDismiss = { selectedMovieForDetail = null }
        )
    }
}

@Composable
fun ShareTasteProfileCard(
    viewModel: MovieViewModel,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, CinemaGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .testTag("share_taste_profile_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🌟", fontSize = 18.sp)
                    Column {
                        Text(
                            text = "SHARE AI TASTE PROFILE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = CinemaGoldLight,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Export your cinematic preferences as a shareable card",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "👤 User: ${viewModel.userName ?: "Film Enthusiast"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "⭐ Top Genres: " + if (viewModel.onboardingGenres.isNotEmpty()) viewModel.onboardingGenres.joinToString(", ") else "Sci-Fi, Cyberpunk, Drama",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "🏆 Favorites: " + if (viewModel.onboardingFavoriteMovies.isNotEmpty()) viewModel.onboardingFavoriteMovies.joinToString(", ") else "Interstellar, Blade Runner 2049, Inception",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Button(
                onClick = { viewModel.shareTasteProfile(context) },
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("share_taste_profile_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Share Taste Card & Deep Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TmdbMovieDetailDialog(
    movie: TmdbMovie,
    viewModel: MovieViewModel,
    context: Context,
    onDismiss: () -> Unit
) {
    val watchlist by viewModel.watchlistHistory.collectAsState()
    val isInWatchlist = watchlist.any { it.title.equals(movie.displayTitle, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ElectricCyan, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = movie.displayTitle,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!movie.fullPosterUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        TmdbMoviePoster(
                            posterUrl = movie.fullPosterUrl,
                            title = movie.displayTitle,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Year: ${movie.displayYear}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                    if ((movie.vote_average ?: 0.0) > 0.0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⭐ ${String.format(java.util.Locale.US, "%.1f", movie.vote_average)} / 10 (${movie.vote_count ?: 0} votes)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CinemaGold
                            )
                        }
                    }
                }

                if (!movie.overview.isNullOrBlank()) {
                    Text(
                        text = "SYNOPSIS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = movie.overview,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Actions: Watchlist and Open TMDB Web
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val match = MovieMatch(
                                title = movie.displayTitle,
                                year = movie.displayYear,
                                director = "TMDB Verified",
                                imdbUrl = "https://www.themoviedb.org/movie/${movie.id}",
                                confidence = "High",
                                rationale = movie.overview?.take(140) ?: "Discovered via TMDB live cinema feed",
                                actors = "TMDB Live Metadata",
                                genre = "Cinema",
                                searchType = "TEXT",
                                isInWatchlist = true,
                                posterPath = movie.fullPosterUrl
                            )
                            viewModel.toggleWatchlist(match)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInWatchlist) Color(0xFF10B981) else ElectricCyan,
                            contentColor = Color(0xFF12131A)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Filled.Check else Icons.Filled.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isInWatchlist) "In Watchlist" else "Save Watchlist",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/movie/${movie.id}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TMDB Web", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}
