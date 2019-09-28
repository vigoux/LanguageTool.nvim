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
import com.ensarsarajcic.neovim.java.api.types.api.VimCoords;
import com.ensarsarajcic.neovim.java.api.types.apiinfo.ApiInfo;
import com.ensarsarajcic.neovim.java.api.window.NeovimWindowApi;
import com.ensarsarajcic.neovim.java.corerpc.message.NotificationMessage;
import com.ensarsarajcic.neovim.java.handler.annotations.NeovimNotificationHandler;

public class LTCommandHandler {

    public static final Logger logger = LoggerFactory.getLogger(App.class);
    private NeovimApi nvimApi;
    private int channelId;
    private JLanguageTool langTool;
    private HashMap<String, List<RuleMatch>> errors;
    private int namespace;

    public static final String CHECK = "Check";
    public static final String HOVER = "Hover";

    public LTCommandHandler(NeovimApi nvimApi) throws InterruptedException, ExecutionException {
        this.nvimApi = nvimApi;

        ApiInfo infos = this.nvimApi.getApiInfo().get();

        this.channelId = infos.getChannelId();

        this.langTool = new JLanguageTool(new BritishEnglish());

        this.errors = new HashMap<String, List<RuleMatch>>();
        this.namespace = 0;

        // Register all functions
        this.registerFunction("LanguageToolCheckJava", LTCommandHandler.CHECK);
        this.registerFunction("LanguageToolHoverError", LTCommandHandler.HOVER);
    }

    private void registerFunction(String functionName, String command) {
        this.nvimApi.executeCommand(
                String.format(
                    "let %s = {-> rpcnotify(%d, 'LTCommand', '%s')}",
                    functionName,
                    this.channelId,
                    command));
    }

    @NeovimNotificationHandler("LTCommand")
    public void handleCommand(NotificationMessage message) throws InterruptedException, ExecutionException {
        List<?> arguments = message.getArguments();

        if (arguments.size() < 1){
            LTCommandHandler.logger.warn("Received an invalid command : " + message.toString());
        } else {
            LTCommandHandler.logger.info("Received : " + message.toString());

            /*
             * Main part of the handler, the one which dispatches tasks
             */

            switch (arguments.get(0).toString()) {
                case LTCommandHandler.CHECK:
                    LTCommandHandler.logger.info("Getting current buffer...");
                    this.nvimApi.getCurrentBuffer().thenAccept(this::checkBuffer);
                    break;
                case LTCommandHandler.HOVER:
                    this.nvimApi.getCurrentWindow().thenAccept(t -> {
                        try {
                            hoverError(t);
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    });
                    break;
                default:
                    LTCommandHandler.logger.warn("%s is not a valid command.", arguments.get(0));
            }
        }
    }

    /*
     * ERROR HOVERING
     */

    private void hoverError(NeovimWindowApi window) throws InterruptedException, ExecutionException {
        RuleMatch errorAtPoint = this.errorAtPoint(window);

        if ( errorAtPoint == null ) {
            return;
        }

        this.nvimApi.writeToOutput(errorAtPoint.getShortMessage() + "\n");
    }

    private RuleMatch errorAtPoint(NeovimWindowApi window) throws InterruptedException, ExecutionException {
        NeovimBufferApi currentBuffer = window.getBuffer().get();

        List<String> lines = currentBuffer.getLines(0, -1, false).get();

        VimCoords cursor = window.getCursor().get();

        String bufferName = currentBuffer.getName().get();

        if ( !this.errors.containsKey(bufferName) ) {
            LTCommandHandler.logger.warn("No error for buffer " + currentBuffer.getName().get());
            LTCommandHandler.logger.info(this.errors.toString());
            return null;
        }

        LanguageToolLineCol start;
        LanguageToolLineCol stop;

        for ( RuleMatch rule : this.errors.get(bufferName) ) {
            start = new LanguageToolLineCol(rule.getFromPos(), lines);
            stop = new LanguageToolLineCol(rule.getToPos(), lines);

            if ( cursor.getRow() - 1 >= start.getLine() && 
                    cursor.getCol() >= start.getCol() &&
                    cursor.getRow() - 1 <= stop.getLine() &&
                    cursor.getCol() <= stop.getCol() ) {
                return rule;
                    }
        }

        LTCommandHandler.logger.info("No error at " + cursor.toString());
        return null;
    }


    /*
     * BUFFER CHECKING
     */
    private void checkBuffer(NeovimBufferApi buffer) {
        List<RuleMatch> matches;
        try {

            LTCommandHandler.logger.info("Checking : %s.", buffer.getName().get());
            List<String> lines = buffer.getLines(0, -1, false).get();
            matches = this.langTool.check(String.join("\n", lines));

            this.errors.put(buffer.getName().get(), matches);

            for (RuleMatch match : matches) {
                this.displayError(match, buffer, lines);
            }

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

        if ( rule.getType() == RuleMatch.Type.UnknownWord ) {
            this.addHighlight(buffer,"LanguageToolSpellingError", start, stop);
        } else {
            this.addHighlight(buffer,"LanguageToolGrammarError", start, stop);
        }
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
