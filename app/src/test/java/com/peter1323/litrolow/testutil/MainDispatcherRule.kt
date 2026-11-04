package com.peter1323.litrolow.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Sustituye el hilo principal de Android, que no existe en los tests unitarios, por uno de prueba. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {

    /** Activa el hilo de prueba antes de cada test. */
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    /** Restaura el hilo principal al terminar cada test. */
    override fun finished(description: Description) = Dispatchers.resetMain()
}
