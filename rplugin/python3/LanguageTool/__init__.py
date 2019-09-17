import pynvim
import LanguageTool.checker
import LanguageTool.ui
import LanguageTool.config

@pynvim.plugin
class LangTool(object):

    def __init__(self, vim):
        self.vim = vim

        self.config = LanguageTool.config.ConfigManager(vim)
        self.checker = LanguageTool.checker.Checker(self.config['server-path'], self.config['port'])
        self.ui_handler = LanguageTool.ui.Ui(vim)
        self.job = None

        self.errors = {}

    @pynvim.command('LanguageToolCheckV2')
    def command_handler(self):

        current_buffer = self.vim.api.get_current_buf()

        filetype = current_buffer.api.get_option('filetype')

        data = self.config[filetype]

        data.update(
                {
                    'text' : '\n'.join(current_buffer.api.get_lines(0, -1, False)),
                    'language' : 'fr'
                }
                )

        answer = self.checker.send(data)

        if 'err' in answer:
            self.vim.err_write(answer['err'] + '\n')

        self.vim.out_write(str(answer) + '\n')

