package exceptions;

import commands.Command;
import util.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IMessage;

/**
 * Created by steve on 14/11/2016.
 */
public class VoiceChannelLimitDiscordException implements DiscordException {

    private final static Logger LOG = LoggerFactory.getLogger(VoiceChannelLimitDiscordException.class);

    @Override
    public void throwException(IMessage message, Command command, Object... arguments) {
        Message.sendText(message.getChannel(), message.getAuthor().getVoiceStateForGuild(message.getGuild()).getChannel().getName()
                + " est complet.");
    }
}
