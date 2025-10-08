package com.sena.monitoreo.api

import com.sena.monitoreo.data.api.ApiService

annotation class RetrofitClient {
    companion object {
        val instance: ApiService
            get() {
                TODO()
            }
    }
}
