" LanguageTool: Grammar checker in Vim for English, French, German, etc.
" Maintainer:   Thomas Vigouroux <tomvig38@gmail.com>
" Last Change:  2019 Sep 12
" Version:      1.0
"
" License: {{{1
"
" The VIM LICENSE applies to LanguageTool.nvim plugin
" (see ":help copyright" except use "LanguageTool.nvim" instead of "Vim").
"
" }}}1

let b:languagetool_preprocess_rules = [
            \ ['^\(.*"\)\([^"]\+\)$', LanguageTool#preprocess#getOutputAlternate(2, 1)],
            \ ['.*', '{"markup":"{{0}}"}']
            \ ]
