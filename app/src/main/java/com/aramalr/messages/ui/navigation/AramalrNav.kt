package com.aramalr.messages.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aramalr.messages.sms.SmsSender
import com.aramalr.messages.ui.compose.NewMessageScreen
import com.aramalr.messages.ui.conversations.ConversationListScreen
import com.aramalr.messages.ui.conversations.ConversationListViewModel
import com.aramalr.messages.ui.settings.AppSettingsScreen
import com.aramalr.messages.ui.thread.ThreadScreen
import com.aramalr.messages.ui.thread.ThreadViewModel

object Routes {
    const val List = "list"
    const val Thread = "thread/{threadId}"
    const val New = "new"
    const val Settings = "settings"

    fun thread(threadId: Long) = "thread/$threadId"
}

@Composable
fun AramalrNav(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val sender = SmsSender(context)

    NavHost(navController = nav, startDestination = Routes.List, modifier = modifier) {
        composable(Routes.List) {
            val vm: ConversationListViewModel = viewModel()
            ConversationListScreen(
                viewModel = vm,
                onOpenThread = { nav.navigate(Routes.thread(it)) },
                onCompose = { nav.navigate(Routes.New) },
                onSettings = { nav.navigate(Routes.Settings) },
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
                onSend = { body ->
                    val address = vm.addressOrFallback()
                    if (address != "Sohbet") {
                        sender.send(address, body)
                    }
                },
            )
        }
        composable(Routes.New) {
            NewMessageScreen(
                onBack = { nav.popBackStack() },
                onOpenedThread = { id ->
                    nav.popBackStack()
                    nav.navigate(Routes.thread(id))
                },
                onSend = { address, body, threadId ->
                    sender.send(address, body)
                    nav.popBackStack()
                    nav.navigate(Routes.thread(threadId))
                },
            )
        }
        composable(Routes.Settings) {
            AppSettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
