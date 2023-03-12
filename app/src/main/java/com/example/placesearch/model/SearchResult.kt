package com.example.placesearch.model

data class SearchResult(
    val address: Address?,
    val display_name: String?,
    val lat: String?,
    val lon: String?,
){
    override fun toString(): String {
        return display_name ?: ""
    }
}