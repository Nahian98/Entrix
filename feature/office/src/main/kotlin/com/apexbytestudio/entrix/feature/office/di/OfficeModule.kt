package com.apexbytestudio.entrix.feature.office.di

import com.apexbytestudio.entrix.feature.office.OfficeListViewModel
import com.apexbytestudio.entrix.feature.office.OfficeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val officeModule = module {
    // officeId is passed at navigation time (Office.NO_ID when adding a new office).
    viewModel { (officeId: Long) -> OfficeViewModel(officeId, get(), get(), get(), get(), get()) }
    viewModelOf(::OfficeListViewModel)
}
