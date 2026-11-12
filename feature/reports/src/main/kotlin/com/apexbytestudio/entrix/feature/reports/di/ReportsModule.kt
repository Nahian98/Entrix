package com.apexbytestudio.entrix.feature.reports.di

import com.apexbytestudio.entrix.feature.reports.ReportsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reportsModule = module {
    viewModelOf(::ReportsViewModel)
}
