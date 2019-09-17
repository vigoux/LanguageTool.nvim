import pynvim

class Ui:

    LANGUAGETOOL_BUFFER_NAME = 'LanguageTool'

    def __init__(self, vim):
        self.vim = vim

    def print_errors(self, window, errors):
        empty_buf = self.vim.api.create_buf(False, False)
        empty_buf.api.set_name(Ui.LANGUAGETOOL_BUFFER_NAME)

        # Set lines
        empty_buf.api.set_lines(
                0, -1, False, list(
                    map(lambda error: error.msg, errors)
                )
            )

        # Set buffer in window
        window.api.set_buf(empty_buf)

        # Set buffer options
        empty_buf.api.set_option('filetype', 'languagetool')
        empty_buf.api.set_option('buftype', 'nowrite')
        empty_buf.api.set_option('bufhidden', 'delete')
        empty_buf.api.set_option('modifiable', False)

        # Set window options
        window.api.set_option('number', False)
        window.api.set_option('relativenumber', False)
        window.api.set_option('signcolumn', 'no')
        window.api.set_option('foldmethod', 'syntax')
