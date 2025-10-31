package dev.arkbuilders.drop.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.arkbuilders.drop.app.domain.AvatarHelper
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.app.domain.usecase.ReceiveFilesUseCase
import dev.arkbuilders.drop.app.domain.usecase.SendFilesUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TmpEntryPoint {
    fun avatarHelper(): AvatarHelper
    fun resourcesHelper(): ResourcesHelper
    fun sendFilesUseCase(): SendFilesUseCase
    fun receiveFilesUseCase(): ReceiveFilesUseCase
}