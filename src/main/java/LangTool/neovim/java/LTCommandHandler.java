package LangTool.neovim.java;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.languagetool.JLanguageTool;
import org.languagetool.language.BritishEnglish;
import org.languagetool.rules.RuleMatch;

import com.ensarsarajcic.neovim.java.api.NeovimApi;
import com.ensarsarajcic.neovim.java.api.buffer.NeovimBufferApi;
import com.ensarsarajcic.neovim.java.api.types.apiinfo.ApiInfo;
import com.ensarsarajcic.neovim.java.corerpc.message.NotificationMessage;
import com.ensarsarajcic.neovim.java.handler.annotations.NeovimNotificationHandler;

public class LTCommandHandler {

    private NeovimApi nvimApi;
    private int channelId;
    private JLanguageTool langTool;

    public LTCommandHandler(NeovimApi nvimApi) throws InterruptedException, ExecutionException {
        this.nvimApi = nvimApi;

        ApiInfo infos = this.nvimApi.getApiInfo().get();

        this.channelId = infos.getChannelId();

        this.nvimApi.executeCommand(
                "let LanguageToolCheckJava = {-> rpcnotify(" + this.channelId + ", 'LTCommand', 'Check')}");

        this.langTool = new JLanguageTool(new BritishEnglish());
    }

    @NeovimNotificationHandler("LTCommand")
    public void handleCommand(NotificationMessage message) throws InterruptedException, ExecutionException {
        System.out.println("Received : " + message);

        List<?> arguments = message.getArguments();

        if (arguments.size() < 1){
            System.out.println("Received and invalid command : " + message.toString());
        } else {

            /*
             * Main part of the handler, the one which dispatches tasks
             */

            switch (arguments.get(0).toString()) {
                case "Check":
                    System.out.println("Gettting current buffer...");
                    NeovimBufferApi buffer = this.nvimApi.getCurrentBuffer().get();
                    System.out.println("Got buffer");

                    System.out.println("Checking : " + buffer.getName().get());
                    buffer.getLines(0, -1, false).thenAccept(this::checkLines);
                    break;
                default:
                    System.out.println(arguments.get(0) + " is not a valid command.");
            }
        }
    }

    private void checkLines(List<String> lines) {
        List<RuleMatch> matches;
        try {

            matches = this.langTool.check(String.join("\n", lines));

            for (RuleMatch match : matches) {
                this.displayError(match, lines);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void displayError(RuleMatch rule, List<String> sourceText) {
        LanguageToolLineCol start = this.retrieveLineCol(rule.getFromPos(), sourceText);
        LanguageToolLineCol stop = this.retrieveLineCol(rule.getToPos() - 1, sourceText);

        String erroredText = String.join("\n", sourceText).subSequence(rule.getFromPos(), rule.getToPos()).toString();

        System.out.println(erroredText);
        System.out.println(start);
        System.out.println(stop);
    }


    private LanguageToolLineCol retrieveLineCol(int index, List<String> sourceText) {
        int lineCount = 0;
        for (String line : sourceText) {
            if ( line.length() > index ) {
                break;
            } else {
                index -= (line.length() + 1);
                lineCount += 1;
            }
        }
        return new LanguageToolLineCol(lineCount + 1, index + 1);
    }
}
