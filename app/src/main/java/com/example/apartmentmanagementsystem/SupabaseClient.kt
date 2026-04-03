package com.example.apartmentmanagementsystem

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.runBlocking

object SupabaseClient {

    const val SUPABASE_URL      = "https://mnpurtjoairsteofknva.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_va_6pxQw-DBeezl3CXu9kg_kyZIdCvb"

    @JvmField
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }



}