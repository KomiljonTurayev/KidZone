package uz.kidzone.app.ui.viewmodel

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import uz.kidzone.app.FamilyError
import uz.kidzone.app.FamilyResult
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.arch.AppPreferences
import uz.kidzone.app.auth.AccountState
import uz.kidzone.app.auth.AuthException
import uz.kidzone.app.auth.AuthProvider
import uz.kidzone.app.auth.FakeAuthRepository

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val auth = FakeAuthRepository(AccountState.SignedIn("uid", "Parent", AuthProvider.GOOGLE))
    private var wipes = 0
    private var wipeWorks = true

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(server: FamilyResult<Unit>): DashboardViewModel {
        val context: Context = RuntimeEnvironment.getApplication()
        return DashboardViewModel(
            ParentalStatsManager(context), AppPreferences(context), auth,
            wipeDevice = { wipes++; wipeWorks },
            deleteOnServer = { server },
        )
    }

    @Test
    fun `the device is wiped once the server has deleted the account`() = runTest(dispatcher) {
        var ok: Boolean? = null
        vm(FamilyResult.Ok(Unit)).deleteAccount { ok = it }
        advanceUntilIdle()

        assertEquals(true, ok)
        assertEquals(1, wipes)
        assertEquals(0, auth.signOutCount)
    }

    @Test
    fun `a failed server delete leaves the device and the account alone`() = runTest(dispatcher) {
        var ok: Boolean? = null
        val vm = vm(FamilyResult.Failed(FamilyError.NETWORK))
        vm.deleteAccount { ok = it }
        advanceUntilIdle()

        assertEquals(false, ok)
        assertEquals(0, wipes)
        assertEquals(0, auth.signOutCount)
        assertEquals(AuthException.Reason.NETWORK, vm.state.value.accountError)
    }

    @Test
    fun `a device that can't be wiped is at least signed out`() = runTest(dispatcher) {
        wipeWorks = false
        vm(FamilyResult.Ok(Unit)).deleteAccount {}
        advanceUntilIdle()

        assertEquals(1, auth.signOutCount)
    }
}
