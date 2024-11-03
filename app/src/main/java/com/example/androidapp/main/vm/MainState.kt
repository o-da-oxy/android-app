package com.example.androidapp.main.vm

import com.example.domain.entity.ListElementEntity

sealed class MainState {
    object Loading : MainState()
    data class Error(
        val message: String
    ) : MainState()

    data class Content(
        val list: List<ListElementEntity>
    ) : MainState()
}
