package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.commands.servers.CommandAddGuestIp;
import io.mamish.serverbot2.commandlambda.commands.welcome.CommandAddIp;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.discordrelay.model.service.NewMessageRequest;
import io.mamish.serverbot2.discordrelay.model.service.SimpleEmbed;
import io.mamish.serverbot2.networksecurity.model.GenerateIpAuthUrlRequest;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;

public class IpAuthMessageHelper {

    private final IDiscordService discordServiceClient;
    private final INetworkSecurity networkSecurityServiceClient;
    private final UrlShortenerClient urlShortenerClient;

    public IpAuthMessageHelper(IDiscordService discordServiceClient, INetworkSecurity networkSecurityServiceClient,
                               UrlShortenerClient urlShortenerClient) {
        this.discordServiceClient = discordServiceClient;
        this.networkSecurityServiceClient = networkSecurityServiceClient;
        this.urlShortenerClient = urlShortenerClient;
    }

    public ProcessUserCommandResponse handleMemberIpAuthRequest(CommandAddIp commandAddIp) {
        ProcessUserCommandRequest context = commandAddIp.getContext();

        GenerateIpAuthUrlRequest generateUrlRequest = new GenerateIpAuthUrlRequest(context.getMessageId(), context.getSenderId());

        String recipient = context.getSenderId();

        String friendlyDomain = CommonConfig.APP_ROOT_DOMAIN_NAME.getValue();

        String message = "To whitelist your IP to join **"+friendlyDomain+"** servers, use this link.\n"
                + "This will detect your IP and add it to the firewall. If you've done this before, it replaces your "
                + "last whitelisted IP.\n";
                //+ "For any questions, message Mamish#7674 or view this bot's code at "
                //+ "https://github.com/HtyCorp/serverbot2-core";

        String embedTitle = "Whitelist IP for " + context.getSenderName();
        String embedDescription = "Personal link to detect and whitelist your IP address for " + friendlyDomain;

        return handleIpAuthRequest(generateUrlRequest, recipient, message, embedTitle, embedDescription);
    }

    public ProcessUserCommandResponse handleGuestIpAuthRequest(CommandAddGuestIp commandAddGuestIp) {
        ProcessUserCommandRequest context = commandAddGuestIp.getContext();

        GenerateIpAuthUrlRequest generateUrlRequest = new GenerateIpAuthUrlRequest(context.getMessageId(), null);

        String recipient = context.getSenderId();

        String friendlyDomain = CommonConfig.APP_ROOT_DOMAIN_NAME.getValue();
        String message = "The following link can be copied and shared to users outside of Discord. When used, it will "
                + "whitelist the user's IP address to connect to "+friendlyDomain+" servers.\n"
                + "Note that this access will expire 3 days after the issue time of this link. Discord members can use "
                + "the !addip command to get long-term access.";

        String embedTitle = "Shareable IP whitelist link for " + context.getSenderName();
        String embedDescription = "Temporary, shareable link to whitelist guest IP addresses for " + friendlyDomain;

        return handleIpAuthRequest(generateUrlRequest, recipient, message, embedTitle, embedDescription);
    }

    private ProcessUserCommandResponse handleIpAuthRequest(
                GenerateIpAuthUrlRequest generateUrlRequest,
                String recipientDiscordUserId,
                String message,
                String embedTitle,
                String embedDescription) {
        
        String fullAuthUrl = networkSecurityServiceClient.generateIpAuthUrl(generateUrlRequest).getIpAuthUrl();
        String shortAuthUrl = urlShortenerClient.getShortenedUrl(fullAuthUrl, NetSecConfig.AUTH_URL_MEMBER_TTL.getSeconds());

        // Send a message to the user privately before returning the standard channel message.
        SimpleEmbed authLinkEmbed = new SimpleEmbed(shortAuthUrl, embedTitle, embedDescription);
        discordServiceClient.newMessage(new NewMessageRequest(
                message, null, null, recipientDiscordUserId, authLinkEmbed
        ));

        return new ProcessUserCommandResponse(
                "A whitelist link has been sent to your private messages."
        );
    }

}
