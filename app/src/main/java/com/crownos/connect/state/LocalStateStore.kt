package com.crownos.connect.state

import com.crownos.connect.di.ApplicationScope
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.protocol.Topic
import com.crownos.connect.runtime.LltsRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import java.util.EnumMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalStateStore @Inject constructor(
    private val runtime: LltsRuntime,
    @ApplicationScope scope: CoroutineScope,
) {
    private val documents = EnumMap<Topic, JsonElement>(Topic::class.java)

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runtime.events.filterIsInstance<LltsEvent.Ready>().collect { replay() }
        }
    }

    fun publish(topic: Topic, document: JsonElement) {
        synchronized(documents) {
            if (documents[topic] == document) return
            documents[topic] = document
        }
        runtime.publishState(topic, document)
    }

    fun current(topic: Topic): JsonElement? = synchronized(documents) { documents[topic] }

    private fun replay() {
        val snapshot = synchronized(documents) { documents.toMap() }
        snapshot.forEach { (topic, document) -> runtime.publishState(topic, document) }
    }
}
