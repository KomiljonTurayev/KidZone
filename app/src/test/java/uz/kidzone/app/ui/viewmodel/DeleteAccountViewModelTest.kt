package uz.kidzone.app.ui.viewmodel

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
import uz.kidzone.app.FamilyError
import uz.kidzone.app.FamilyResult
import uz.kidzone.app.auth.AccountState
import uz.kidzone.app.auth.FakeAuthRepository

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun outcome(
        server: FamilyResult<Unit>,
        auth: FakeAuthRepository = FakeAuthRepository(),
    ): DeleteAccountState {
        var result = DeleteAccountState.IDLE
        runTest(dispatcher) {
            val vm = DeleteAccountViewModel(auth) { server }
            vm.delete()
            advanceUntilIdle()
            result = vm.state.value
        }
        return result
    }

    @Test
    fun `done once the server has deleted the account`() =
        assertEquals(DeleteAccountState.DELETED, outcome(FamilyResult.Ok(Unit)))

    @Test
    fun `a failed server delete is reported`() =
        assertEquals(DeleteAccountState.FAILED, outcome(FamilyResult.Failed(FamilyError.NETWORK)))

    @Test
    fun `a lost answer still counts when the account is gone`() = assertEquals(
        DeleteAccountState.DELETED,
        outcome(FamilyResult.Failed(FamilyError.FORBIDDEN), FakeAuthRepository().apply { accountGone = true }),
    )

    @Test
    fun `without a session there is nothing to delete on the server`() = assertEquals(
        DeleteAccountState.DELETED,
        outcome(FamilyResult.Failed(FamilyError.NETWORK), FakeAuthRepository(AccountState.Unavailable)),
    )
}
