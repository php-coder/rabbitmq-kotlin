package com.viartemev.thewhiterabbit.consumer.flow

import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConsumerFlowTest : AbstractTestContainersTest() {
    private val QUEUE_NAME = "test_queue"
    private val connection = factory.newConnection()

    private suspend fun generateMessages(count: Int) = coroutineScope {
        connection.channel {
            declareQueue(QueueSpecification(QUEUE_NAME))
        }
        connection.channel {
            (1..count).map {
                basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_BASIC, "Hello #$it".toByteArray())
            }
        }
    }

    @Test
    fun testAutoAckFlow(): Unit = runBlocking {
        val messagesCount = 100
        generateMessages(messagesCount)
        connection.channel {
            ConsumerFlow(this, QUEUE_NAME)
                .consumerAutoAckFlow(2)
                .take(messagesCount)
                .collect { delivery -> println(String(delivery.body)) }
        }
    }

    @Test
    fun testConfirmAckFlow(): Unit = runBlocking {
        val messagesCount = 10
        generateMessages(messagesCount)
        connection.channel {
            ConsumerFlow(this, QUEUE_NAME)
                .consumerConfirmAckFlow(2)
                .flowOn(Dispatchers.IO)
                .take(messagesCount)
                .catch { e -> println("Caught exception: $e") }.collect { delivery ->
                    println("Got the message: ${delivery.body}")
                }
        }
    }

}
