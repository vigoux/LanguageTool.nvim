import pynvim

class ConfigManager:

    def __init__(self, vim):
        self.vim = vim

    def __getitem__(self, index):
        if index == 'port':
            return '8081'
        elif index == 'server-path':
            return '~/LanguageTool/languagetool-server.jar'
        else:
            return {}
