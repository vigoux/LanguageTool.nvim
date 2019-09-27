package LangTool.neovim.java;

import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.languagetool.JLanguageTool;
import org.languagetool.language.BritishEnglish;
import org.languagetool.rules.RuleMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ensarsarajcic.neovim.java.api.NeovimApi;
import com.ensarsarajcic.neovim.java.api.buffer.NeovimBufferApi;
import com.ensarsarajcic.neovim.java.api.types.apiinfo.ApiInfo;
import com.ensarsarajcic.neovim.java.corerpc.message.NotificationMessage;
import com.ensarsarajcic.neovim.java.handler.annotations.NeovimNotificationHandler;

public class LTCommandHandler {

    public static final Logger logger = LoggerFactory.getLogger(App.class);
    private NeovimApi nvimApi;
    private int channelId;
    private JLanguageTool langTool;
    private HashMap<NeovimBufferApi, List<RuleMatch>> errors;
    private int namespace;

    public LTCommandHandler(NeovimApi nvimApi) throws InterruptedException, ExecutionException {
        this.nvimApi = nvimApi;

        ApiInfo infos = this.nvimApi.getApiInfo().get();

        this.channelId = infos.getChannelId();

        this.nvimApi.executeCommand(
                "let LanguageToolCheckJava = {-> rpcnotify(" + this.channelId + ", 'LTCommand', 'Check')}");

        this.langTool = new JLanguageTool(new BritishEnglish());

        this.errors = new HashMap<NeovimBufferApi, List<RuleMatch>>();
        this.namespace = 0;
    }

    @NeovimNotificationHandler("LTCommand")
    public void handleCommand(NotificationMessage message) throws InterruptedException, ExecutionException {
        List<?> arguments = message.getArguments();

        if (arguments.size() < 1){
            LTCommandHandler.logger.warn("Received and invalid command : %s", message.toString());
        } else {
            LTCommandHandler.logger.info("Received : %s", message.toString());

            /*
             * Main part of the handler, the one which dispatches tasks
             */

            switch (arguments.get(0).toString()) {
                case "Check":
                    LTCommandHandler.logger.info("Gettting current buffer...");
                    this.nvimApi.getCurrentBuffer().thenAccept(this::checkBuffer);
                    break;
                default:
                    LTCommandHandler.logger.warn("%s is not a valid command.", arguments.get(0));
            }
        }
    }

    private void checkBuffer(NeovimBufferApi buffer) {
        List<RuleMatch> matches;
        try {

            LTCommandHandler.logger.info("Checking : %s.", buffer.getName().get());
            List<String> lines = buffer.getLines(0, -1, false).get();
            matches = this.langTool.check(String.join("\n", lines));

            for (RuleMatch match : matches) {
                this.displayError(match, buffer, lines);
            }

            this.errors.put(buffer, matches);
        } catch (IOException e) {
            LTCommandHandler.logger.error("An error occured %s", e);
        } catch (InterruptedException e) {
            LTCommandHandler.logger.error("An error occured %s", e);
        } catch (ExecutionException e) {
            LTCommandHandler.logger.error("An error occured %s", e);
        }
    }

    private void displayError(RuleMatch rule, NeovimBufferApi buffer, List<String> lines) throws InterruptedException, ExecutionException {
        LanguageToolLineCol start = new LanguageToolLineCol(rule.getFromPos(), lines);
        LanguageToolLineCol stop = new LanguageToolLineCol(rule.getToPos(), lines);

        // String erroredText = String.join("\n", lines).subSequence(rule.getFromPos(), rule.getToPos()).toString();

        this.addHighlight(buffer,"LanguageToolGrammarError", start, stop);
    }

    private void addHighlight(NeovimBufferApi buffer, String hlGroup, LanguageToolLineCol start, LanguageToolLineCol stop) {
        if ( start.getLine() == stop.getLine() ) {
            buffer.addHighlight(this.namespace, hlGroup, start.getLine(), start.getCol(), stop.getCol());
        } else {

            // Highlight first line of error
            buffer.addHighlight(this.namespace, hlGroup, start.getLine(), start.getCol(), -1);

            // Lines in between
            for (int line = start.getLine() + 1; line < stop.getLine(); line++) {
                buffer.addHighlight(this.namespace, hlGroup, line, 0, -1);
            }

            // Highlight last line
            buffer.addHighlight(this.namespace, hlGroup, stop.getLine(), 0, stop.getCol());
        }
    }
}
