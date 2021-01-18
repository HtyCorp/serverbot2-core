package com.admiralbot.commandservice;

import com.admiralbot.commandservice.commands.servers.CommandAddGuestIp;
import com.admiralbot.commandservice.commands.welcome.CommandAddIp;
import com.admiralbot.commandservice.model.ProcessUserCommandRequest;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.discordrelay.model.service.SimpleEmbed;
import com.admiralbot.framework.exception.ApiException;
import com.admiralbot.networksecurity.model.GenerateIpAuthUrlRequest;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.NetSecConfig;
import com.admiralbot.urlshortener.model.DeliverUrlRequest;
import com.admiralbot.urlshortener.model.DeliverUrlResponse;
import com.admiralbot.urlshortener.model.DeliveryType;
import com.admiralbot.urlshortener.model.IUrlShortener;

public class IpAuthMessageHelper {

    private final INetworkSecurity networkSecurityService;
    private final IUrlShortener urlShortenerClient;

    public IpAuthMessageHelper(INetworkSecurity networkSecurityService,
                               IUrlShortener urlShortenerClient) {
        this.networkSecurityService = networkSecurityService;
        this.urlShortenerClient = urlShortenerClient;
    }

    public ProcessUserCommandResponse handleMemberIpAuthRequest(CommandAddIp commandAddIp) {
        ProcessUserCommandRequest context = commandAddIp.getContext();

        GenerateIpAuthUrlRequest generateUrlRequest = new GenerateIpAuthUrlRequest(context.getMessageId(), context.getSenderId());
        String url = networkSecurityService.generateIpAuthUrl(generateUrlRequest).getIpAuthUrl();

        String friendlyDomain = CommonConfig.APP_ROOT_DOMAIN_NAME.getValue();

        String message = "To whitelist your IP to join **"+friendlyDomain+"** servers, use this link.\n"
                + "This will detect your IP and add it to the firewall. If you've done this before, it replaces your "
                + "last whitelisted IP.\n";
                //+ "For any questions, message Mamish#7674 or view this bot's code at "
                //+ "https://github.com/HtyCorp/serverbot2-core";

        String embedTitle = "Whitelist IP for " + context.getSenderName();
        String embedDescription = "Personal link to detect and whitelist your IP address for " + friendlyDomain;

        DeliverUrlRequest deliverUrlRequest = new DeliverUrlRequest(
                context.getSenderId(), url, NetSecConfig.AUTH_URL_GUEST_TTL.getSeconds(),
                DeliveryType.AUTOMATIC_WORKFLOW, // Member links are used directly so prefer maximum delivery level
                message, new SimpleEmbed(url, embedTitle, embedDescription)
        );

        return deliverAndRespond(deliverUrlRequest);
    }

    public ProcessUserCommandResponse handleGuestIpAuthRequest(CommandAddGuestIp commandAddGuestIp) {
        ProcessUserCommandRequest context = commandAddGuestIp.getContext();

        GenerateIpAuthUrlRequest generateUrlRequest = new GenerateIpAuthUrlRequest(context.getMessageId(), null);
        String url = networkSecurityService.generateIpAuthUrl(generateUrlRequest).getIpAuthUrl();

        String friendlyDomain = CommonConfig.APP_ROOT_DOMAIN_NAME.getValue();
        String message = "The following link can be copied and shared to users outside of Discord. When used, it will "
                + "whitelist the user's IP address to connect to "+friendlyDomain+" servers.\n"
                + "Note that this access will expire 3 days after the issue time of this link. Discord members can use "
                + "the !addip command instead to get long-term access.";

        DeliverUrlRequest deliverUrlRequest = new DeliverUrlRequest(
                context.getSenderId(), url, NetSecConfig.AUTH_URL_MEMBER_TTL.getSeconds(),
                DeliveryType.PRIVATE_MESSAGE_LINK, // Guest links are meant to be distributed, not used immediately
                message, null
        );

        return deliverAndRespond(deliverUrlRequest);
    }

    private ProcessUserCommandResponse deliverAndRespond(DeliverUrlRequest deliverRequest) {
        DeliverUrlResponse response;
        try {
            response = urlShortenerClient.deliverUrl(deliverRequest);
        } catch (ApiException e) {
            return new ProcessUserCommandResponse(
                    "Something went wrong while preparing your whitelist URL."
            );
        }

        switch(response.getDeliveryTypeUsed()) {
            case PRIVATE_MESSAGE_LINK:
                return new ProcessUserCommandResponse(
                        "A whitelist link has been sent to your private messages."
                );
            case PUSH_NOTIFICATION:
                return new ProcessUserCommandResponse(
                        "A clickable whitelist notification has been sent via your browser."
                );
            case AUTOMATIC_WORKFLOW:
                return new ProcessUserCommandResponse(
                        "Whitelisting has been automatically run via your browser."
                );
            default:
                throw new RuntimeException("Unexpected delivery type used: " + response.getDeliveryTypeUsed());
        }
    }

}
