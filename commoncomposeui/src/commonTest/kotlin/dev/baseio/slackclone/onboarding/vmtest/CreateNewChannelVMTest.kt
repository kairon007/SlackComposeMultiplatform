package dev.baseio.slackclone.onboarding.vmtest

import androidx.compose.runtime.snapshotFlow
import app.cash.turbine.test
import dev.baseio.slackclone.channels.createsearch.CreateNewChannelVM
import dev.baseio.slackdomain.datasources.local.channels.SKLocalDataSourceReadChannels
import dev.baseio.slackdomain.model.channel.DomainLayerChannels.SKChannel
import io.mockative.any
import io.mockative.given
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.asserter

class CreateNewChannelVMTest : SlackKoinTest() {
    private val skLocalDataSourceReadChannels: SKLocalDataSourceReadChannels by inject()
    private var wasNavigated = false
    private var navigationWith: (SKChannel) -> Unit = {
        wasNavigated = true
    }

    private val createNewChannelVM by lazy {
        CreateNewChannelVM(
            coroutineDispatcherProvider,
            useCaseCreateChannel,
            useCaseGetSelectedWorkspace,
            navigationWith
        )
    }

    @Test
    fun `when create channel is called with new channel name createdChannel is not null and local database has it!`() {
        runTest {
            assumeAuthorized()
            val channelId = "1"
            val name = "channel_public_$channelId"

            given(iGrpcCalls)
                .suspendFunction(iGrpcCalls::savePublicChannel)
                .whenInvokedWith(any(), any())
                .thenReturn(
                    AuthTestFixtures.testPublicChannel(
                        channelId,
                        "1"
                    )
                )


            with(createNewChannelVM.createChannelState) {
                value = value.copy(
                    channel = value.channel.copy(
                        name = name
                    )
                )

                createNewChannelVM.createChannel()

                test {
                    awaitItem().also {
                        asserter.assertTrue({ "was expecting true" }, it.loading)
                    }
                    awaitItem().also {
                        asserter.assertTrue({ "was expecting true" }, it.loading)
                    }
                    awaitItem().also {
                        asserter.assertTrue({ "was expecting false" }, it.loading.not())
                    }
                    asserter.assertTrue({ "was expecting $wasNavigated" }, wasNavigated)
                }

                skLocalDataSourceReadChannels.fetchAllChannels(useCaseGetSelectedWorkspace.invoke()!!.uuid)
                    .test {
                        awaitItem().apply {
                            asserter.assertTrue(
                                { "Was expecting the channel" },
                                this.find { it.channelName == name } != null
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `when create channel is called with existing channel name then we get an exception`() {
        runTest {
            assumeAuthorized()

            val channelId = "1"

            given(iGrpcCalls)
                .suspendFunction(iGrpcCalls::savePublicChannel)
                .whenInvokedWith(any())
                .thenReturn(
                    AuthTestFixtures.testPublicChannel(
                        channelId,
                        "1"
                    )
                )

            createNewChannelVM.createChannelState.value =
                createNewChannelVM.createChannelState.value.copy(
                    channel = createNewChannelVM.createChannelState.value.channel.copy(
                        name = "new_channel"
                    )
                )
            createNewChannelVM.createChannelState.test {
                createNewChannelVM.createChannel()
                asserter.assertTrue({ "was expecting true" }, !awaitItem().loading)
                asserter.assertTrue({ "was expecting true" }, awaitItem().loading)
                asserter.assertTrue({ "was expecting true" }, awaitItem().loading)
                asserter.assertTrue({ "was expecting true" }, awaitItem().loading.not())
            }

            snapshotFlow {
                wasNavigated
            }.distinctUntilChanged().test {
                asserter.assertTrue("was expecting to be navigated", awaitItem())
            }
        }
    }
}
