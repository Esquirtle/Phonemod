package esq.phonemod.phone.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import esq.phonemod.phone.messaging.PhoneRegistry;
import esq.phonemod.phone.messaging.TextMessage;

import javax.annotation.Nonnull;

/**
 * {@code /pg sendtext <number> <message>}
 *
 * <p>Sends a text message to the specified phone number using the {@code "Server"} identity
 * ({@code "000-000"}). Used for testing the messaging system without a second player.
 */
public final class SendTextCommand extends CommandBase {

    private static final String SERVER_NUMBER = "000-000";
    private static final String SERVER_NAME = "Server";

    private final RequiredArg<String> toArg;
    private final RequiredArg<String> messageArg;

    public SendTextCommand() {
        super("sendtext", "Send a text to a phone number as [Server]. Usage: sendtext <number> <message>");
        this.toArg = withRequiredArg("to", "Target phone number (e.g. 555-1234)", ArgTypes.STRING);
        this.messageArg = withRequiredArg("message", "Message body (can have spaces)", ArgTypes.GREEDY_STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String to = (String) this.toArg.get(context);
        String body = (String) this.messageArg.get(context);

        if (to == null || body == null) {
            context.sendMessage(Message.raw("Usage: /pg sendtext <number> <message>").color("#FF5555"));
            return;
        }

        PhoneRegistry.deliver(to, new TextMessage(SERVER_NUMBER, SERVER_NAME, body));
        context.sendMessage(Message.raw("[Server → " + to + "] " + body).color("#43D69A"));
    }
}
