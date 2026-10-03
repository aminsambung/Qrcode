package com.example.qrpro.api

import com.google.gson.annotations.SerializedName

data class ProductResponse(
    val status: Int,
    val product: Product?
)

data class Product(
    @SerializedName("product_name") val name: String?,
    @SerializedName("brands") val brand: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("quantity") val quantity: String?,
    @SerializedName("categories") val categories: String?,
    @SerializedName("ingredients_text") val ingredients: String?,
    @SerializedName("nutriscore_grade") val nutriScore: String?
)