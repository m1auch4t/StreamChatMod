package me.mini_bomba.streamchatmod;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLModDisabledEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = StreamChatMod.MODID, version = StreamChatMod.VERSION, clientSideOnly = true, useMetadata = true)
public class StreamChatMod
{
    public static final String MODID = "streamchatmod";
    public static final String MODNAME = "StreamChat";
    public static final String VERSION = "1.0";
    private StreamConfig config;
    private TwitchClient twitch = null;
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
		startTwitch();
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new StreamConfig(event.getSuggestedConfigurationFile());
    }

    @EventHandler
    public void stop(FMLModDisabledEvent event) {
        stopTwitch();
    }

    public void startTwitch() {
        if (!config.twitchEnabled.getBoolean() || config.twitchChannels.getStringList().length == 0) return;
        String token = config.twitchToken.getString();
        OAuth2Credential credential = new OAuth2Credential("twitch", token);
        twitch = TwitchClientBuilder.builder()
                .withDefaultAuthToken(credential)
                .withEnableChat(true)
                .withChatAccount(credential)
                .build();
        twitch.getEventManager().onEvent(ChannelMessageEvent.class, this::onTwitchMessage);
        TwitchChat chat = twitch.getChat();
        chat.connect();
        for (String channel : config.twitchChannels.getStringList()) {
            chat.joinChannel(channel);
        }
    }

    private void onTwitchMessage(ChannelMessageEvent event) {
        sendLocalMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE+"[TWITCH"+(config.showChannelName.getBoolean() ? "/"+event.getChannel().getName() : "")+"]"+EnumChatFormatting.WHITE+" <"+event.getUser().getName()+"> "+event.getMessage()));
    }

    private void sendLocalMessage(IChatComponent chat) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;
        Minecraft.getMinecraft().thePlayer.addChatMessage(chat);
    }

    public void stopTwitch() {
        if (twitch == null) return;
        TwitchChat chat = twitch.getChat();
        for (String channel : chat.getChannels()) {
            chat.leaveChannel(channel);
        }
        chat.disconnect();
        twitch.close();
    }
}
