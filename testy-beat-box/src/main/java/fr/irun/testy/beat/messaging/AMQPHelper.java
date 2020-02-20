package fr.irun.testy.beat.messaging;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.RpcClient;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.util.Queue;
import java.util.UUID;

import static reactor.rabbitmq.RabbitFlux.createSender;

public final class AMQPHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AMQPHelper.class);

    private static final String DEFAULT_RABBIT_REPLY_QUEUE_NAME = "amq.rabbitmq.reply-to";
    private static final int TIMEOUT_DURATION = 1;
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    private AMQPHelper() {
    }

    /**
     * Declare queues for communication
     *
     * @param channel           The channel object used for communication
     * @param queueName         The queue name for rabbit communication
     * @param exchangeQueueName The exchange queue name for rabbit communication
     * @throws IOException Exception if declaration failure
     */
    public static void declareAndBindQueues(Channel channel, String queueName, String exchangeQueueName) throws IOException {
        channel.queueDeclare(queueName, false, false, true, null);
        channel.exchangeDeclare(exchangeQueueName, BuiltinExchangeType.DIRECT, false, true, null);
        channel.queueBind(queueName, exchangeQueueName, "");

        channel.queueDeclare(DEFAULT_RABBIT_REPLY_QUEUE_NAME, false, false, true, null);
    }

    /**
     * Declare the reply queue.
     *
     * @param channel The channel object used for communication.
     * @throws IOException if an error occurred during queue declaration.
     */
    public static void declareReplyQueue(Channel channel) throws IOException {
        channel.queueDeclare(DEFAULT_RABBIT_REPLY_QUEUE_NAME, false, false, true, null);
    }

    /**
     * Declare Receiver Options for queue communication
     *
     * @param conn      The connection to use for communication
     * @param scheduler The scheduler to use for communication
     * @return The built receiver options
     */
    public static ReceiverOptions declareReceiverOptions(Connection conn, Scheduler scheduler) {
        return new ReceiverOptions()
                .connectionMono(Mono.fromCallable(() -> conn))
                .connectionSubscriptionScheduler(scheduler);
    }

    /**
     * Declare Sender Options for queue communication
     *
     * @param conn      The connection to use for communication
     * @param channel   The channel to use for communication
     * @param scheduler The scheduler to use for communication
     * @return The built sender options
     */
    public static SenderOptions declareSenderOptions(Connection conn, Channel channel, Scheduler scheduler) {
        return new SenderOptions()
                .connectionMono(Mono.fromCallable(() -> conn))
                .channelMono(Mono.fromCallable(() -> channel))
                .resourceManagementScheduler(scheduler);
    }

    /**
     * Declare consumer to read messages received and respond a replyMessage.
     *
     * @param channel      The channel to use for communication
     * @param messages     The messages received in a Queue
     * @param queueName    The queue name for rabbit communication
     * @param replyMessage The message used to reply to sender
     */
    public static void declareConsumer(Channel channel, Queue<Delivery> messages, String queueName, Object replyMessage) {
        declareConsumer(channel, DEFAULT_OBJECT_MAPPER, messages, queueName, replyMessage);
    }

    /**
     * Declare a consumer on the given queue.
     *
     * @param channel      Channel of the AMQP broker.
     * @param objectMapper Jackson mapper to convert deliveries to object.
     * @param messages     Queue where the messages are put.
     * @param queueName    Name of the consumed queue.
     * @param replyMessage Content of the response body.
     */
    public static void declareConsumer(Channel channel,
                                       ObjectMapper objectMapper,
                                       Queue<Delivery> messages,
                                       String queueName,
                                       @Nullable Object replyMessage) {
        try {
            channel.basicConsume(queueName, true, buildTestConsumer(channel, objectMapper, messages, replyMessage));
        } catch (IOException e) {
            LOGGER.error("Failure during message reception", e);
        }
    }

    /**
     * Build a {@link com.rabbitmq.client.Consumer} for the tests.
     *
     * @param channel          Channel to consume to messages.
     * @param objectMapper     Mapper to convert objects to byte content.
     * @param receivedMessages Queue storing the messages received by the consumer.
     * @param responseContent  Body of the response.
     * @return The built consumer.
     */
    private static Consumer buildTestConsumer(Channel channel,
                                              ObjectMapper objectMapper,
                                              Queue<Delivery> receivedMessages,
                                              @Nullable Object responseContent) {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                receivedMessages.offer(new Delivery(envelope, properties, body));
                channel.basicPublish(
                        "",
                        properties.getReplyTo(),
                        new AMQP.BasicProperties.Builder().correlationId(properties.getCorrelationId()).build(),
                        objectMapper.writeValueAsBytes(responseContent)
                );
            }
        };
    }

    /**
     * Declare a message to send into rabbit communication and send it
     *
     * @param message           The rabbit message to send
     * @param senderOptions     The options used to send message
     * @param exchangeQueueName The exchange queue name
     * @return the result of sending ReviewResultMessage
     */
    public static Mono<Delivery> emitWithReply(Object message,
                                               SenderOptions senderOptions,
                                               String exchangeQueueName) {
        return Mono.using(() -> createSender(senderOptions),
                sender -> AMQPHelper.processEmission(sender, message, DEFAULT_OBJECT_MAPPER, exchangeQueueName, Duration.ofSeconds(TIMEOUT_DURATION)),
                Sender::close
        );
    }

    /**
     * Declare a message to send into rabbit communication and send it
     *
     * @param message           The rabbit message to send
     * @param objectMapper      Mapper converting POJOs to byte content.
     * @param senderOptions     The options used to send message
     * @param exchangeQueueName The exchange queue name
     * @param timeout           The timeout duration before receiving a reply
     * @return the result of sending ReviewResultMessage
     */
    public static Mono<Delivery> emitWithReply(Object message,
                                               ObjectMapper objectMapper,
                                               SenderOptions senderOptions,
                                               String exchangeQueueName,
                                               Duration timeout) {
        return Mono.using(() -> createSender(senderOptions),
                sender -> AMQPHelper.processEmission(sender, message, objectMapper, exchangeQueueName, timeout),
                Sender::close
        );
    }

    private static Mono<Delivery> processEmission(Sender sender,
                                                  Object messageToSend,
                                                  ObjectMapper objectMapper,
                                                  String exchangeQueueName,
                                                  Duration timeout) {
        LOGGER.debug("Send message {}. Expect reply", messageToSend);

        return Mono.using(() -> sender.rpcClient(exchangeQueueName, "", () -> UUID.randomUUID().toString()),
                rpcClient -> rpcClient.rpc(Mono.just(buildRpcRequest(objectMapper, messageToSend)))
                        .timeout(timeout)
                        .doOnError(e -> LOGGER.error("ProcessEmission failure with RPC Client '{}'. {}: {}",
                                rpcClient, e.getClass(), e.getLocalizedMessage())),
                RpcClient::close);
    }

    private static RpcClient.RpcRequest buildRpcRequest(ObjectMapper objectMapper, Object message) {
        try {
            final byte[] content = objectMapper.writeValueAsBytes(message);
            final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .replyTo(DEFAULT_RABBIT_REPLY_QUEUE_NAME)
                    .build();
            return new RpcClient.RpcRequest(properties, content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to write message to byte array!", e);
        }
    }
}
