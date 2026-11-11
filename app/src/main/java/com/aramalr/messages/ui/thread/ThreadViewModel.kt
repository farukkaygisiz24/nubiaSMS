package com.aramalr.messages.ui.thread

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aramalr.messages.sms.ConversationRepository
import com.aramalr.messages.sms.Message
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ThreadViewModel(
    app: Application,
    private val threadId: Long,
) : AndroidViewModel(app) {
    private val repo = ConversationRepository(app)

    val messages: StateFlow<List<Message>> =
        repo.observeMessages(threadId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addressOrFallback(): String =
        messages.value.firstOrNull()?.address ?: "Sohbet"

    companion object {
        fun factory(threadId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    throw IllegalStateException("Use AndroidViewModelFactory with Application")
                }

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: androidx.lifecycle.viewmodel.CreationExtras,
                ): T {
                    val app =
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                            as Application
                    return ThreadViewModel(app, threadId) as T
                }
            }
    }
}
