package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.Parameter;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ChannelMap {

    private Map<ServerTextChannel, MessageChannel> discordToApp = new HashMap<>();
    private Map<MessageChannel, ServerTextChannel> appToDiscord = new EnumMap<>(MessageChannel.class);

    public ChannelMap(DiscordApi discordApi) {
        putBoth(discordApi, MessageChannel.STANDARD, DiscordConfig.CHANNEL_ID_STANDARD);
        putBoth(discordApi, MessageChannel.OFFICER, DiscordConfig.CHANNEL_ID_OFFICER);
        putBoth(discordApi, MessageChannel.ADMIN, DiscordConfig.CHANNEL_ID_ADMIN);
        putBoth(discordApi, MessageChannel.DEBUG, DiscordConfig.CHANNEL_ID_DEBUG);
    }

    public Optional<MessageChannel> getAppChannel(ServerTextChannel discordChannel) {
        return Optional.ofNullable(discordToApp.get(discordChannel));
    }

    public Optional<ServerTextChannel> getDiscordChannel(MessageChannel appChannel) {
        return Optional.ofNullable(appToDiscord.get(appChannel));
    }

    private void putBoth(DiscordApi discordApi, MessageChannel appChannel, Parameter channelIdParameter) {
        ServerTextChannel discordChannel = discordApi.getChannelById(channelIdParameter.getValue()).flatMap(Channel::asServerTextChannel).get();
        discordToApp.put(discordChannel, appChannel);
        appToDiscord.put(appChannel, discordChannel);
    }

}
