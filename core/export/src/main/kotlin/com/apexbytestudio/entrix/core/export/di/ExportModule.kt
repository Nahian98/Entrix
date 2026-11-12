package com.apexbytestudio.entrix.core.export.di

import com.apexbytestudio.entrix.core.export.PdfTimesheetExporter
import com.apexbytestudio.entrix.domain.export.TimesheetExporter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val exportModule = module {
    single<TimesheetExporter> { PdfTimesheetExporter(androidContext()) }
}
