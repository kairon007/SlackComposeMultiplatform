package dev.baseio.grpc

import dev.baseio.slackdata.common.KMEmpty
import dev.baseio.slackdata.common.kmEmpty
import dev.baseio.slackdata.protos.*
import dev.baseio.slackdomain.AUTH_TOKEN
import dev.baseio.slackdomain.CoroutineDispatcherProvider
import dev.baseio.slackdomain.datasources.local.SKLocalKeyValueSource
import dev.baseio.slackdomain.model.users.DomainLayerUsers
import dev.baseio.slackdomain.usecases.channels.UseCaseWorkspaceChannelRequest
import io.github.timortel.kotlin_multiplatform_grpc_lib.KMChannel
import io.github.timortel.kotlin_multiplatform_grpc_lib.KMMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GrpcCalls(
    private val address: String = "192.168.178.25",
    private val port: Int = 8080,
    override val skKeyValueData: SKLocalKeyValueSource,
    private val coroutineDispatcherProvider: CoroutineDispatcherProvider
) : IGrpcCalls {
    companion object {
        const val AUTHENTICATION_TOKEN_KEY = "Authorization"
    }

    private val grpcChannel by lazy {
        KMChannel.Builder
            .forAddress(address, port)
            .usePlaintext()
            .build()
    }

    private val workspacesStub by lazy {
        KMWorkspaceServiceStub(grpcChannel)
    }

    private val channelsStub by lazy {
        KMChannelsServiceStub(grpcChannel)
    }

    private val pushServiceStub by lazy {
        KMSecurePushServiceStub(grpcChannel)
    }

    private val usersStub by lazy {
        KMUsersServiceStub(grpcChannel)
    }

    private val qrCodeStub by lazy {
        KMQrCodeServiceStub(grpcChannel)
    }

    private val messagingStub by lazy {
        KMMessagesServiceStub(grpcChannel)
    }

    override suspend fun authorizeQrCode(code: KMSKQRAuthVerify, token: String?): KMSKAuthResult {
        return withContext(coroutineDispatcherProvider.io) {
            qrCodeStub.verifyQrCode(
                code,
                fetchToken(token)
            )
        }
    }

    override suspend fun saveFcmToken(fcmToken: KMSKPushToken, token: String?): KMEmpty {
        return withContext(coroutineDispatcherProvider.io) {
            pushServiceStub.savePushToken(fcmToken, fetchToken(token))
        }
    }

    override fun getQrCodeResponse(token: String?): Flow<KMSKQrCodeResponse> {
        return qrCodeStub.generateQRCode(kmSKQrCodeGenerator { }, fetchToken(token))
    }

    override suspend fun getUsersForWorkspaceId(
        workspace: String,
        token: String?
    ): KMSKUsers {
        return withContext(coroutineDispatcherProvider.io) {
            usersStub.getUsers(
                kmSKWorkspaceChannelRequest { workspaceId = workspace },
                fetchToken(token)
            )
        }
    }

    override suspend fun currentLoggedInUser(token: String?): KMSKUser {
        return withContext(coroutineDispatcherProvider.io) {
            usersStub.currentLoggedInUser(kmEmpty { }, fetchToken(token))
        }
    }

    override suspend fun findWorkspaceByName(name: String, token: String?): KMSKWorkspace {
        return withContext(coroutineDispatcherProvider.io) {
            workspacesStub.findWorkspaceForName(
                kmSKFindWorkspacesRequest {
                    this.name = name
                },
                fetchToken(token)
            )
        }
    }

    override suspend fun findWorkspacesForEmail(
        email: String,
        token: String?
    ): KMSKWorkspaces {
        return withContext(coroutineDispatcherProvider.io) {
            workspacesStub.findWorkspacesForEmail(
                kmSKFindWorkspacesRequest {
                    this.email = email
                },
                fetchToken(token)
            )
        }
    }

    override suspend fun getWorkspaces(token: String?): KMSKWorkspaces {
        return withContext(coroutineDispatcherProvider.io) {
            workspacesStub.getWorkspaces(kmEmpty { }, fetchToken(token))
        }
    }

    override suspend fun sendMagicLink(
        workspace: KMSKCreateWorkspaceRequest,
        token: String?
    ): KMSKWorkspace {
        return withContext(coroutineDispatcherProvider.io) {
            workspacesStub.letMeIn(workspace, fetchToken(token))
        }
    }

    override suspend fun getPublicChannels(
        workspaceIdentifier: String,
        offset: Int,
        limit: Int,
        token: String?
    ): KMSKChannels {
        return withContext(coroutineDispatcherProvider.io) {
            channelsStub.getAllChannels(
                kmSKChannelRequest {
                    workspaceId = workspaceIdentifier
                    this.paged = kmSKPagedRequest {
                        this.offset = offset
                        this.limit = limit
                    }
                },
                fetchToken(token)
            )
        }
    }

    override suspend fun getAllDMChannels(
        workspaceIdentifier: String,
        offset: Int,
        limit: Int,
        token: String?
    ): KMSKDMChannels {
        return withContext(coroutineDispatcherProvider.io) {
            channelsStub.getAllDMChannels(
                kmSKChannelRequest {
                    workspaceId = workspaceIdentifier
                    this.paged = kmSKPagedRequest {
                        this.offset = offset
                        this.limit = limit
                    }
                },
                fetchToken(token)
            )
        }
    }

    override suspend fun savePublicChannel(
        kmChannel: KMSKChannel,
        token: String?
    ): KMSKChannel {
        return withContext(coroutineDispatcherProvider.io) {
            channelsStub.savePublicChannel(
                kmChannel,
                fetchToken(token)
            )
        }
    }

    override suspend fun saveDMChannel(
        kmChannel: KMSKDMChannel,
        token: String?
    ): KMSKDMChannel {
        return withContext(coroutineDispatcherProvider.io) {
            channelsStub.saveDMChannel(
                kmChannel,
                fetchToken(token)
            )
        }
    }

    override fun listenToChangeInMessages(
        workspaceChannelRequest: UseCaseWorkspaceChannelRequest,
        token: String?
    ): Flow<KMSKMessageChangeSnapshot> {
        return messagingStub.registerChangeInMessage(
            kmSKWorkspaceChannelRequest {
                workspaceId =
                    workspaceChannelRequest.workspaceId
                channelId =
                    workspaceChannelRequest.channelId
            },
            fetchToken(token)
        )
    }

    override fun listenToChangeInUsers(
        workspaceId: String,
        token: String?
    ): Flow<KMSKUserChangeSnapshot> {
        return usersStub.registerChangeInUsers(
            kmSKWorkspaceChannelRequest {
                this.workspaceId = workspaceId
            },
            fetchToken(token)
        )
    }

    override fun listenToChangeInChannels(
        workspaceId: String,
        token: String?
    ): Flow<KMSKChannelChangeSnapshot> {
        return channelsStub.registerChangeInChannels(
            kmSKChannelRequest {
                this.workspaceId = workspaceId
            },
            fetchToken(token)
        )
    }

    override fun listenToChangeInDMChannels(
        workspaceId: String,
        token: String?
    ): Flow<KMSKDMChannelChangeSnapshot> {
        return channelsStub.registerChangeInDMChannels(
            kmSKChannelRequest {
                this.workspaceId =
                    workspaceId
            },
            fetchToken(token)
        )
    }

    override suspend fun fetchChannelMembers(
        request: UseCaseWorkspaceChannelRequest,
        token: String?
    ): KMSKChannelMembers {
        return withContext(
            coroutineDispatcherProvider.io
        ) {
            channelsStub.channelMembers(
                kmSKWorkspaceChannelRequest {
                    this.workspaceId =
                        request.workspaceId
                    this.channelId =
                        request.channelId
                },
                fetchToken(token)
            )
        }
    }

    override suspend fun inviteUserToChannel(
        userId: String,
        channelId: String,
        skSlackKey: DomainLayerUsers.SKEncryptedMessage,
        token: String?
    ): KMSKChannelMembers {
        return withContext(
            coroutineDispatcherProvider.io
        ) {
            channelsStub.inviteUserToChannel(
                kmSKInviteUserChannel {
                    this.channelId =
                        channelId
                    this.userId = userId
                    this.channelPrivateKey =
                        kmSKEncryptedMessage {
                            this.first =
                                skSlackKey.first
                            this.second =
                                skSlackKey.second
                        }
                },
                fetchToken(token)
            )
        }
    }

    override fun listenToChangeInChannelMembers(
        workspaceId: String,
        memberId: String,
        token: String?
    ): Flow<KMSKChannelMemberChangeSnapshot> {
        return channelsStub.registerChangeInChannelMembers(
            kmSKChannelMember {
                this.workspaceId =
                    workspaceId
                this.memberId =
                    memberId
            },
            fetchToken(token)
        )
    }

    override fun listenToChangeInWorkspace(
        workspaceId: String,
        token: String?
    ): Flow<KMSKWorkspaceChangeSnapshot> {
        return workspacesStub.registerChangeInWorkspace(
            kmSKWorkspace {
                this.uuid =
                    workspaceId
            },
            fetchToken(token)
        )
    }

    override suspend fun fetchMessages(
        request: UseCaseWorkspaceChannelRequest
    ): KMSKMessages {
        return withContext(
            coroutineDispatcherProvider.io
        ) {
            messagingStub.getMessages(
                kmSKWorkspaceChannelRequest {
                    this.workspaceId =
                        request.workspaceId
                    this.channelId =
                        request.channelId
                    this.paged =
                        kmSKPagedRequest {
                            this.limit =
                                request.limit
                            this.offset =
                                request.offset
                        }
                }
            )
        }
    }

    override suspend fun sendMessage(
        kmskMessage: KMSKMessage,
        token: String?
    ): KMSKMessage {
        return withContext(
            coroutineDispatcherProvider.io
        ) {
            messagingStub.saveMessage(
                kmskMessage,
                fetchToken(
                    token
                )
            )
        }
    }

    private fun fetchToken(
        token: String?
    ): KMMetadata {
        return KMMetadata().apply {
            if (token != null) {
                set(
                    AUTHENTICATION_TOKEN_KEY,
                    "Bearer $token"
                )
            }
        }
    }
}

interface IGrpcCalls {
    val skKeyValueData: SKLocalKeyValueSource

    suspend fun authorizeQrCode(
        code: KMSKQRAuthVerify,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKAuthResult

    fun getQrCodeResponse(
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): Flow<KMSKQrCodeResponse>

    suspend fun getUsersForWorkspaceId(
        workspace: String,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKUsers

    suspend fun currentLoggedInUser(
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKUser

    suspend fun findWorkspaceByName(
        name: String,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKWorkspace

    suspend fun findWorkspacesForEmail(
        email: String,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKWorkspaces

    suspend fun getWorkspaces(
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKWorkspaces

    suspend fun sendMagicLink(
        workspace: KMSKCreateWorkspaceRequest,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKWorkspace

    suspend fun getPublicChannels(
        workspaceIdentifier: String,
        offset: Int,
        limit: Int,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKChannels

    suspend fun getAllDMChannels(
        workspaceIdentifier: String,
        offset: Int,
        limit: Int,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKDMChannels

    suspend fun savePublicChannel(
        kmChannel: KMSKChannel,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKChannel

    suspend fun saveDMChannel(
        kmChannel: KMSKDMChannel,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKDMChannel

    fun listenToChangeInMessages(
        workspaceChannelRequest: UseCaseWorkspaceChannelRequest,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): Flow<KMSKMessageChangeSnapshot>

    fun listenToChangeInUsers(
        workspaceId: String,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): Flow<KMSKUserChangeSnapshot>

    fun listenToChangeInChannels(
        workspaceId: String,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): Flow<KMSKChannelChangeSnapshot>

    fun listenToChangeInDMChannels(
        workspaceId: String,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): Flow<KMSKDMChannelChangeSnapshot>

    suspend fun fetchChannelMembers(
        request: UseCaseWorkspaceChannelRequest,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKChannelMembers

    suspend fun inviteUserToChannel(
        userId: String,
        channelId: String,
        skSlackKey: DomainLayerUsers.SKEncryptedMessage,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKChannelMembers

    suspend fun fetchMessages(
        request: UseCaseWorkspaceChannelRequest
    ): KMSKMessages

    suspend fun sendMessage(
        kmskMessage: KMSKMessage,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMSKMessage

    fun listenToChangeInWorkspace(
        workspaceId: String,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): Flow<KMSKWorkspaceChangeSnapshot>

    fun listenToChangeInChannelMembers(
        workspaceId: String,
        memberId: String,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): Flow<KMSKChannelMemberChangeSnapshot>

    suspend fun saveFcmToken(
        fcmToken: KMSKPushToken,
        token: String? = skKeyValueData.get(
            AUTH_TOKEN
        )
    ): KMEmpty
}
