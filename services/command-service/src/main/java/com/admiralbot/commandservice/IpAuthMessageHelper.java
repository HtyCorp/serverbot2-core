package com.admiralbot.commandservice;

import com.admiralbot.commandservice.commands.servers.CommandAddGuestIp;
import com.admiralbot.commandservice.commands.welcome.CommandAddIp;
import com.admiralbot.commandservice.model.ProcessUserCommandRequest;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.discordrelay.model.service.SimpleEmbed;
import com.admiralbot.networksecurity.model.GenerateIpAuthUrlRequest;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.NetSecConfig;
import com.admiralbot.urlshortener.model.CreateShortUrlRequest;
import com.admiralbot.urlshortener.model.IUrlShortener;

public class IpAuthMessageHelper {

    private final INetworkSecurity networkSecurityServiceClient;
    private final IUrlShortener urlShortenerClient;

    public IpAuthMessageHelper(INetworkSecurity networkSecurityServiceClient,
                               IUrlShortener urlShortenerClient) {
        this.networkSecurityServiceClient = networkSecurityServiceClient;
        this.urlShortenerClient = urlShortenerClient;
    }

    public ProcessUserCommandResponse handleMemberIpAuthRequest(CommandAddIp commandAddIp) {
        ProcessUserCommandRequest context = commandAddIp.getContext();

        GenerateIpAuthUrlRequest generateUrlRequest = new GenerateIpAuthUrlRequest(context.getCommandSourceId(), context.getSenderId());

        String friendlyDomain = CommonConfig.APP_ROOT_DOMAIN_NAME.getValue();

        String message = "To whitelist your IP to join **"+friendlyDomain+"** servers, use this link.\n"
                + "This will detect your IP and add it to the firewall. If you've done this before, it replaces your "
                + "last whitelisted IP.\n";
                //+ "For any questions, message Mamish#7674 or view this bot's code at "
                //+ "https://github.com/HtyCorp/serverbot2-core";

        String embedTitle = "Whitelist IP for " + context.getSenderName();
        String embedDescription = "Personal link to detect and whitelist your IP address for " + friendlyDomain;

        return handleIpAuthRequest(generateUrlRequest, message, embedTitle, embedDescription);
    }

    public ProcessUserCommandResponse handleGuestIpAuthRequest(CommandAddGuestIp commandAddGuestIp) {
        ProcessUserCommandRequest context = commandAddGuestIp.getContext();

        GenerateIpAuthUrlRequest generateUrlRequest = new GenerateIpAuthUrlRequest(context.getCommandSourceId(), null);

        String recipient = context.getSenderId();

        String friendlyDomain = CommonConfig.APP_ROOT_DOMAIN_NAME.getValue();
        String message = "The following link can be copied and shared to users outside of Discord. When used, it will "
                + "whitelist the user's IP address to connect to "+friendlyDomain+" servers.\n"
                + "Note that this access will expire 3 days after the issue time of this link. Discord members can use "
                + "the /addip command to get long-term access.";

        String embedTitle = "Shareable IP whitelist link for " + context.getSenderName();
        String embedDescription = "Temporary, shareable link to whitelist guest IP addresses for " + friendlyDomain;

        return handleIpAuthRequest(generateUrlRequest, message, embedTitle, embedDescription);
    }

    private ProcessUserCommandResponse handleIpAuthRequest(
                GenerateIpAuthUrlRequest generateUrlRequest,
                String message,
                String embedTitle,
                String embedDescription) {
        
        String fullAuthUrl = networkSecurityServiceClient.generateIpAuthUrl(generateUrlRequest).getIpAuthUrl();

        CreateShortUrlRequest createShortUrlRequest = new CreateShortUrlRequest(
                fullAuthUrl, NetSecConfig.AUTH_URL_MEMBER_TTL.getSeconds()
        );
        String shortAuthUrl = urlShortenerClient.createShortUrl(createShortUrlRequest).getShortUrl();

        SimpleEmbed authLinkEmbed = new SimpleEmbed(shortAuthUrl, embedTitle, embedDescription);
        return new ProcessUserCommandResponse(
                "A whitelist link has been sent to your private messages.",
                message, authLinkEmbed
        );
    }

}
