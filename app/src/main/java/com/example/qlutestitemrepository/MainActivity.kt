package com.example.qlutestitemrepository

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.qlutestitemrepository.ui.HomeViewModel
import com.example.qlutestitemrepository.ui.MainViewModel
import com.example.qlutestitemrepository.ui.navigation.BottomNavigationBar
import com.example.qlutestitemrepository.ui.navigation.Screen
import com.example.qlutestitemrepository.ui.screens.AboutScreen
import com.example.qlutestitemrepository.ui.screens.FileManagerScreen
import com.example.qlutestitemrepository.ui.screens.HomeScreen
import com.example.qlutestitemrepository.ui.screens.MoreScreen
import com.example.qlutestitemrepository.ui.screens.SettingsScreen
import com.example.qlutestitemrepository.ui.theme.QLUTestItemRepositoryTheme

class MainActivity : ComponentActivity() {
    private val mainViewModel by viewModels<MainViewModel>()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            // HomeViewModel needs to be scoped to the NavGraph or Activity to persist across tab switches if we want shared state,
            // but here we just need it for HomeScreen and to receive update events.
            // Using viewModel() here creates it scoped to the *Activity* because MainActivity is the owner if we don't pass a `viewModelStoreOwner`.
            // Wait, viewModel() without args uses LocalViewModelStoreOwner, which is the Activity in setContent.
            // So it is shared.
            val homeViewModel: HomeViewModel = viewModel()

            // Observe auto-update trigger
            LaunchedEffect(Unit) {
                mainViewModel.refreshTrigger.collect {
                    homeViewModel.refresh()
                }
            }

            AnimatedContent(
                targetState = mainViewModel.isDarkTheme,
                transitionSpec = {
                    expandIn(
                        animationSpec = tween(700),
                        expandFrom = Alignment.BottomStart
                    ) + fadeIn(animationSpec = tween(700)) with
                    shrinkOut(
                        animationSpec = tween(700),
                        shrinkTowards = Alignment.BottomStart
                    ) + fadeOut(animationSpec = tween(700))
                },
                label = "ThemeTransition"
            ) { isDark ->
                QLUTestItemRepositoryTheme(
                    darkTheme = isDark,
                    themeColor = mainViewModel.themeColor
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val showBottomBar = currentRoute in listOf(
                        Screen.Home.route,
                        Screen.More.route,
                        Screen.Settings.route
                    )

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                BottomNavigationBar(navController = navController)
                            }
                        }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Home.route
                            ) {
                                composable(Screen.Home.route) {
                                    HomeScreen(
                                        isDarkTheme = mainViewModel.isDarkTheme,
                                        onThemeToggle = { mainViewModel.toggleTheme() },
                                        viewModel = homeViewModel
                                    )
                                }
                                composable(Screen.More.route) {
                                    MoreScreen(
                                        navController = navController,
                                        isDarkTheme = mainViewModel.isDarkTheme,
                                        onThemeToggle = { mainViewModel.toggleTheme() }
                                    )
                                }
                                composable(Screen.FileManager.route) {
                                    FileManagerScreen(navController)
                                }
                                composable(Screen.Settings.route) {
                                    SettingsScreen(
                                        navController = navController,
                                        isDarkTheme = mainViewModel.isDarkTheme,
                                        onThemeToggle = { mainViewModel.toggleTheme() },
                                        onColorChange = { mainViewModel.updateThemeColor(it) },
                                        mainViewModel = mainViewModel,
                                        onUpdateData = { homeViewModel.refresh() }
                                    )
                                }
                                composable(Screen.About.route) {
                                    AboutScreen(navController = navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
