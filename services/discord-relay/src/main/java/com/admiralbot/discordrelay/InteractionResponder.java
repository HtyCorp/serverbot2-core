package com.admiralbot.discordrelay;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.InteractionType;
import org.javacord.api.interaction.callback.InteractionFollowupMessageBuilder;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.interaction.callback.InteractionMessageBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class to assist with responding to interactions 'asynchronously', i.e. from a lookup of the original
 * interaction ID and token when an interaction response has to be edited outside the original DiscordRelay event.
 */

public class InteractionResponder extends InteractionMessageBuilder {

    private final DiscordApi discordApi;

    public InteractionResponder(DiscordApi discordApi) {
        this.discordApi = discordApi;
    }

    public CompletableFuture<InteractionMessageBuilder> sendInitialResponse(String interactionId, String interactionToken) {
        return super.sendInitialResponse(mockInteraction(interactionId, interactionToken));
    }

    public CompletableFuture<Message> editFollowupMessage(String interactionId, String interactionToken, long messageId) {
        return super.editFollowupMessage(mockInteraction(interactionId, interactionToken), messageId);
    }

    public CompletableFuture<Message> editFollowupMessage(String interactionId, String interactionToken, String messageId) {
        return super.editFollowupMessage(mockInteraction(interactionId, interactionToken), messageId);
    }

    public CompletableFuture<Void> updateOriginalMessage(String interactionId, String interactionToken) {
        return super.updateOriginalMessage(mockInteraction(interactionId, interactionToken));
    }

    public CompletableFuture<Void> deleteInitialResponse(String interactionId, String interactionToken) {
        return super.deleteInitialResponse(mockInteraction(interactionId, interactionToken));
    }

    public CompletableFuture<Void> deleteFollowupMessage(String interactionId, String interactionToken, String messageId) {
        return super.deleteFollowupMessage(mockInteraction(interactionId, interactionToken), messageId);
    }

    private InteractionBase mockInteraction(String interactionId, String interactionToken) {
        return new InteractionBase() {

            private final long interactionIdLong = Long.parseLong(interactionId);

            @Override
            public long getApplicationId() {
                return discordApi.getClientId();
            }

            @Override
            public InteractionType getType() {
                return InteractionType.SLASH_COMMAND;
            }

            @Override
            public InteractionImmediateResponseBuilder createImmediateResponder() {
                throw new IllegalStateException();
            }

            @Override
            public CompletableFuture<InteractionOriginalResponseUpdater> respondLater() {
                throw new IllegalStateException();
            }

            @Override
            public InteractionFollowupMessageBuilder createFollowupMessageBuilder() {
                throw new IllegalStateException();
            }

            @Override
            public Optional<Server> getServer() {
                throw new IllegalStateException();
            }

            @Override
            public Optional<TextChannel> getChannel() {
                throw new IllegalStateException();
            }

            @Override
            public User getUser() {
                throw new IllegalStateException();
            }

            @Override
            public String getToken() {
                return interactionToken;
            }

            @Override
            public int getVersion() {
                throw new IllegalStateException();
            }

            @Override
            public DiscordApi getApi() {
                return discordApi;
            }

            @Override
            public long getId() {
                return interactionIdLong;
            }
        };
    }
}
