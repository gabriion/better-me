package com.gabriion.betterme.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gabriion.betterme.ui.calories.CaloriesScreen
import com.gabriion.betterme.ui.evolution.EvolutionScreen
import com.gabriion.betterme.ui.gym.GymScreen
import com.gabriion.betterme.ui.home.HomeScreen
import com.gabriion.betterme.ui.meals.MealsScreen
import com.gabriion.betterme.ui.tips.DailyTipsScreen

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Dest("home", "Home", Icons.Outlined.Home)
    data object Tips : Dest("tips", "Tips", Icons.Outlined.Spa)
    data object Meals : Dest("meals", "Meals", Icons.Outlined.LocalDining)
    data object Calories : Dest("calories", "Calories", Icons.Outlined.PieChart)
    data object Gym : Dest("gym", "Gym", Icons.Outlined.FitnessCenter)
    data object Evolution : Dest("evolution", "Evolution", Icons.Outlined.TrendingUp)
}

private val destinations = listOf(
    Dest.Home, Dest.Tips, Dest.Meals, Dest.Calories, Dest.Gym, Dest.Evolution
)

@Composable
fun BetterMeNavHost() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { dest ->
                    val selected = backStackEntry?.destination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(dest.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Dest.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Dest.Home.route) { HomeScreen() }
            composable(Dest.Tips.route) { DailyTipsScreen() }
            composable(Dest.Meals.route) { MealsScreen() }
            composable(Dest.Calories.route) { CaloriesScreen() }
            composable(Dest.Gym.route) { GymScreen() }
            composable(Dest.Evolution.route) { EvolutionScreen() }
        }
    }
}
