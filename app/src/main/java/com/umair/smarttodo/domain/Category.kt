package com.umair.smarttodo.domain

/**
 * Task classification buckets. [label] is the user-facing English display name.
 */
enum class Category(val label: String) {
    WORK("Work"),
    PERSONAL("Personal"),
    HEALTH_FITNESS("Health & Fitness"),
    CODING("Coding"),
    STUDY("Study"),
    SHOPPING("Shopping"),
    FINANCE("Finance"),
    OTHER("Other"),
}
