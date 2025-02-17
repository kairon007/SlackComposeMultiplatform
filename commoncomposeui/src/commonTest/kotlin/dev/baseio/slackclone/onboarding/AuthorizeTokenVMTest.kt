package dev.baseio.slackclone.onboarding

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshotFlow
import app.cash.turbine.test
import dev.baseio.database.SlackDB
import dev.baseio.grpc.IGrpcCalls
import dev.baseio.slackclone.onboarding.vmtest.AuthTestFixtures
import dev.baseio.slackclone.onboarding.vmtest.SlackKoinTest
import dev.baseio.slackclone.slackKoinApp
import dev.baseio.slackdata.datasources.local.users.SKLocalDataSourceUsersImpl
import dev.baseio.slackdata.datasources.local.workspaces.SKLocalDataSourceReadWorkspacesImpl
import dev.baseio.slackdata.datasources.local.workspaces.SKLocalDataSourceWriteWorkspacesImpl
import dev.baseio.slackdata.datasources.remote.auth.SKAuthNetworkDataSourceImpl
import dev.baseio.slackdata.datasources.remote.workspaces.SKNetworkDataSourceReadWorkspacesImpl
import dev.baseio.slackdata.injection.TestCoroutineDispatcherProvider
import dev.baseio.slackdata.localdata.FakeKeyValueSource
import dev.baseio.slackdata.localdata.testDbConnection
import dev.baseio.slackdata.mapper.SlackUserMapper
import dev.baseio.slackdata.mapper.SlackWorkspaceMapper
import dev.baseio.slackdomain.model.channel.DomainLayerChannels
import dev.baseio.slackdomain.usecases.auth.UseCaseFetchAndSaveCurrentUser
import dev.baseio.slackdomain.usecases.workspaces.UseCaseFetchAndSaveWorkspaces
import dev.baseio.slackdomain.usecases.workspaces.UseCaseSetLastSelectedWorkspace
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.given
import io.mockative.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.asserter

class AuthorizeTokenVMTest : SlackKoinTest() {
    private var wasNavigated = MutableStateFlow(false)
    private val navigateDashboard: () -> Unit = {
        wasNavigated.value = true
    }

    private val viewModel by lazy {
        AuthorizeTokenVM(
            coroutineDispatcherProvider = slackKoinApp.koin.get(),
            useCaseFetchAndSaveCurrentUser = slackKoinApp.koin.get(),
            useCaseFetchAndSaveUserWorkspace = slackKoinApp.koin.get(),
            authToken = "authToken",
            navigateDashboard = navigateDashboard
        )
    }

    @Test
    fun `given deeplink with token when we fetch data with that token then we navigate to dashboard`() = runTest {
        given(iGrpcCalls)
            .suspendFunction(iGrpcCalls::getWorkspaces)
            .whenInvokedWith(any())
            .thenReturn(AuthTestFixtures.testWorkspaces())

        given(iGrpcCalls)
            .suspendFunction(iGrpcCalls::currentLoggedInUser)
            .whenInvokedWith(any())
            .thenReturn(AuthTestFixtures.testUser())

        viewModel.uiState.test {
            asserter.assertTrue("was expecting loading", awaitItem().loading)
            ensureAllEventsConsumed()
        }

        wasNavigated.test {
            asserter.assertTrue("was expecting to be navigated", awaitItem())
            ensureAllEventsConsumed()
        }
    }
}