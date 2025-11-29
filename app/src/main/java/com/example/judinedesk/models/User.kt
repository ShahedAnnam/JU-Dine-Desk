package com.example.judinedesk.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
open class BaseUser(
    open val uid: String = "",
    open val email: String = "",
    open val role: String = "", // "student", "manager", "staff"
    open val hall: String = ""  // related hall name or hallID
) : Parcelable

@Parcelize
data class Student(
    override val uid: String = "",
    val userId: String = "",  // auto-generated from name + unique number
    val name: String = "",
    override val email: String = "",
    val mobile: String = "",
    val department: String = "",
    val batch: String = "",
    val classRoll: String = "",
    override val hall: String = "",
    override val role: String = "student"
) : BaseUser(uid, email, role, hall), Parcelable

@Parcelize
data class Manager(
    override val uid: String = "",
    val employeeId: String = "",
    override val hall: String = "",
    override val role: String = "manager",
    override val email: String = ""
) : BaseUser(uid, email, role, hall), Parcelable

@Parcelize
data class Staff(
    override val uid: String = "",
    val employeeId: String = "",
    override val hall: String = "",
    override val role: String = "staff",
    override val email: String = "",
    // Add these new fields
    val name: String = "",
    val age: Int = 0,
    val homeDistrict: String = "",
    val contactNumber: String = "",
    val position: String = "",
    val salary: Double = 0.0,
    val joinDate: String = "",
    val isActive: Boolean = true
) : BaseUser(uid, email, role, hall), Parcelable
