package com.example.purrytify.ui.util

import com.example.purrytify.R

object CountryConstant {
    val CountryColor: Map<String, Pair<Long, Long>> = mapOf(
        "BR" to Pair(0xFFEAB435, 0xFFF39E25),
        "CH" to Pair(0xFFF15464, 0xFFEB2539),
        "DE" to Pair(0xFFF0483F, 0xFFEB2034),
        "GB" to Pair(0xFF9B2645, 0xFFDF1F36),
        "ID" to Pair(0xFFEF4E5E, 0xFFEB1F35),
        "MY" to Pair(0xFF6C2EED, 0xFF4506F3),
        "US" to Pair(0xFFCC1956, 0xFFEA1C34),
        "" to Pair(0xFFEF4E5E, 0xFFEB1F35)
    )

    val CountryImage: Map<String, Int> = mapOf(
        "BR" to R.drawable.br,
        "CH" to R.drawable.ch,
        "DE" to R.drawable.de,
        "GB" to R.drawable.gb,
        "ID" to R.drawable.id,
        "MY" to R.drawable.my,
        "US" to R.drawable.us,
        "" to R.drawable.id
    )

    val CountryName: Map<String, String> = mapOf(
        "BR" to "Brazil",
        "CH" to "Switzerland",
        "DE" to "Germany",
        "GB" to "UK",
        "ID" to "Indonesia",
        "MY" to "Malaysia",
        "US" to "United States",
        "" to ""
    )

    val CountrySet: Set<String> = setOf(
        "BR",
        "CH",
        "DE",
        "GB",
        "ID",
        "MY",
        "US",
    )
}