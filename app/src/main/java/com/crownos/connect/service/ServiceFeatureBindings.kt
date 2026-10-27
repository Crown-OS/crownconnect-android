package com.crownos.connect.service

import com.crownos.connect.commands.PeerCommandHandler
import com.crownos.connect.commands.RemoteCommandDispatcher
import com.crownos.connect.files.FileReceiver
import com.crownos.connect.input.InputRouter
import com.crownos.connect.media.MediaSessionRouter
import com.crownos.connect.privileged.PrivilegedWiring
import com.crownos.connect.state.BatteryPublisher
import com.crownos.connect.state.CallBridge
import com.crownos.connect.state.ClipboardBridge
import com.crownos.connect.state.HotspotBridge
import com.crownos.connect.state.MediaSessionBridge
import com.crownos.connect.state.VolumeBridge
import com.crownos.connect.unicursor.CursorHandoff
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceFeatureBindings {
    @Binds @IntoSet abstract fun battery(feature: BatteryPublisher): ServiceFeature
    @Binds @IntoSet abstract fun volume(feature: VolumeBridge): ServiceFeature
    @Binds @IntoSet abstract fun media(feature: MediaSessionBridge): ServiceFeature
    @Binds @IntoSet abstract fun calls(feature: CallBridge): ServiceFeature
    @Binds @IntoSet abstract fun clipboard(feature: ClipboardBridge): ServiceFeature
    @Binds @IntoSet abstract fun hotspot(feature: HotspotBridge): ServiceFeature
    @Binds @IntoSet abstract fun commands(feature: RemoteCommandDispatcher): ServiceFeature
    @Binds @IntoSet abstract fun input(feature: InputRouter): ServiceFeature
    @Binds @IntoSet abstract fun privileged(feature: PrivilegedWiring): ServiceFeature
    @Binds @IntoSet abstract fun mediaStreams(feature: MediaSessionRouter): ServiceFeature
    @Binds @IntoSet abstract fun files(feature: FileReceiver): ServiceFeature
    @Binds @IntoSet abstract fun cursorHandoff(feature: CursorHandoff): ServiceFeature
    @Binds @IntoSet abstract fun unicursorCommands(handler: InputRouter): PeerCommandHandler
    @Binds @IntoSet abstract fun fileCommands(handler: FileReceiver): PeerCommandHandler
}
