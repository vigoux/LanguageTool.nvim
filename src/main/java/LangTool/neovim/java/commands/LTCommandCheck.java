package LangTool.neovim.java.commands;

import java.util.List;

import com.ensarsarajcic.neovim.java.api.NeovimApi;
import com.ensarsarajcic.neovim.java.corerpc.message.NotificationMessage;
import com.ensarsarajcic.neovim.java.handler.annotations.NeovimNotificationHandler;

import LangTool.neovim.java.LTAbstractCommand;

public class LTCommandCheck extends LTAbstractCommand {

    public void handleCheck(NotificationMessage message) {
        System.out.println("Received : " + message);
    }
}
