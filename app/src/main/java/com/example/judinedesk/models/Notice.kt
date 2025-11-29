package com.example.judinedesk.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Notice(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val hall: String = "",
    val postedBy: String = "",
    val postedAt: Long = 0L,
    val managerUid: String = ""
) : Parcelable