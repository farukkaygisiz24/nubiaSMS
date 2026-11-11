package com.aramalr.messages.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aramalr.messages.ui.compose.NewMessageScreen
import com.aramalr.messages.ui.conversations.ConversationListScreen
import com.aramalr.messages.ui.conversations.ConversationListViewModel
import com.aramalr.messages.ui.thread.ThreadScreen
import com.aramalr.messages.ui.thread.ThreadViewModel

object Routes {
    const val List = "list"
    const val Thread = "thread/{threadId}"
    const val New = "new"

    fun thread(threadId: Long) = "thread/$threadId"
}

@Composable
fun AramalrNav(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.List, modifier = modifier) {
        composable(Routes.List) {
            val vm: ConversationListViewModel = viewModel()
            ConversationListScreen(
                viewModel = vm,
                onOpenThread = { nav.navigate(Routes.thread(it)) },
                onCompose = { nav.navigate(Routes.New) },
            )
        }
        composable(
            route = Routes.Thread,
            arguments = listOf(navArgument("threadId") { type = NavType.LongType }),
        ) { entry ->
            val threadId = entry.arguments!!.getLong("threadId")
            val vm: ThreadViewModel =
                viewModel(factory = ThreadViewModel.factory(threadId))
            ThreadScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.New) {
            NewMessageScreen(
                onBack = { nav.popBackStack() },
                onOpenedThread = { id ->
                    nav.popBackStack()
                    nav.navigate(Routes.thread(id))
                },
            )
        }
    }
}
