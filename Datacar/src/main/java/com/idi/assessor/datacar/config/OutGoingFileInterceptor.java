package com.idi.assessor.datacar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Service;

import com.idi.assessor.datacar.common.FileDataWrapper;

@Service
public class OutGoingFileInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(OutGoingFileInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final ChannelsNamesEnum channelNameEnum = ChannelsNamesEnum.getValueFromName(((NamedComponent) channel).getComponentName());
        Object payload = message.getPayload();

        if (payload instanceof FileDataWrapper) {
            FileDataWrapper fileData = (FileDataWrapper) payload;
            return MessageBuilder.fromMessage(message)
                    .setHeader("MessageID", fileData.getName())
                    .setHeader("ChannelName", channelNameEnum.getName())
                    .build();
        } else {
            log.warn("OutGoingFileInterceptor received a message with unexpected payload type: {}", payload.getClass().getName());
            return MessageBuilder.fromMessage(message)
                    .setHeader("MessageID", "unknown_payload_type")
                    .setHeader("ChannelName", channelNameEnum.getName())
                    .build();
        }
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        log.info("postSend(): MessageID=" + message.getHeaders().get("MessageID")
                + " ChannelName=" + message.getHeaders().get("ChannelName") + " Sent: " + sent);
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        if (ex != null) {
            log.error("Error sending message with ID: {} on channel: {}. Sent: {}. Exception: ",
                    message.getHeaders().get("MessageID"), message.getHeaders().get("ChannelName"), sent, ex);
        }
    }

    @Override
    public boolean preReceive(MessageChannel channel) {
        return true;
    }

    @Override
    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        return message;
    }

    @Override
    public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
        if (ex != null) {
            log.error("Error after receiving message: {} on channel: {}. Exception: ", message, channel, ex);
        }
    }
}
