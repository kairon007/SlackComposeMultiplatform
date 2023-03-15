package dev.baseio.slackclone.onboarding.vmtest

import app.cash.turbine.test
import dev.baseio.grpc.IGrpcCalls
import dev.baseio.slackclone.onboarding.vm.SendMagicLinkForWorkspaceEmail
import dev.baseio.slackdata.common.kmEmpty
import dev.baseio.slackdata.protos.kmSKWorkspace
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.asserter
import kotlin.time.Duration.Companion.seconds

class SendMagicLinkForWorkspaceEmailTest : SlackKoinUnitTest() {

    private lateinit var viewModel: SendMagicLinkForWorkspaceEmail
    private fun getViewModel(email: String) = SendMagicLinkForWorkspaceEmail(
        coroutineDispatcherProvider = coroutineDispatcherProvider,
        useCaseAuthWorkspace = useCaseAuthWorkspace,
        useCaseSaveFCMToken = koinApplication.koin.get(),
        email,
        "slack",
    )

    @Test
    fun `viewModel informs the component to navigate after successful authentication`() {
        viewModel = getViewModel("sdf@sdffd.com")
        runTest {
            mocker.every { koinApplication.koin.get<IGrpcCalls>().skKeyValueData } returns koinApplication.koin.get()
            mocker.everySuspending {
                koinApplication.koin.get<IGrpcCalls>().saveFcmToken(isAny(), isAny())
            } returns kmEmpty { }
            mocker.everySuspending {
                koinApplication.koin.get<IGrpcCalls>().sendMagicLink(
                    isAny(), isAny()
                )
            } returns kmSKWorkspace { }
            mocker.everySuspending {
                koinApplication.koin.get<IGrpcCalls>().currentLoggedInUser(isAny())
            } returns testUser()

            viewModel.sendMagicLink()
            viewModel.state.test(timeout = 5.seconds) {
                awaitItem().apply {
                    asserter.assertTrue(actual = loading, message = "Loading was not true!")
                }
                awaitItem().apply {
                    asserter.assertTrue(actual = !loading, message = "Loading was not false!")
                    asserter.assertTrue(
                        actual = this.error == null,
                        message = "error was not null!"
                    )
                }
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    @Test
    fun `viewModel state fails with validation exception`() {
        viewModel = getViewModel("sdfsdffd.com")

        runTest {
            viewModel.sendMagicLink()
            viewModel.state.test(timeout = 5.seconds) {
                awaitItem().apply {
                    asserter.assertTrue(actual = this.error != null, message = "error was null!")
                }
            }
        }
    }

}