" LanguageTool: Grammar checker in Vim for English, French, German, etc.
" Maintainer:   Thomas Vigouroux <tomvig38@gmail.com>
" Last Change:  2019 Sep 14
" Version:      1.0
"
" License: {{{1
"
" The VIM LICENSE applies to LanguageTool.nvim plugin
" (see ":help copyright" except use "LanguageTool.nvim" instead of "Vim").
"
" }}} 1

" This function opens a:errors summaries in a:window
function! LanguageTool#ui#displayErrors(window, errors, flags) "{{{1
    if !bufloaded('LanguageTool')
        let l:empty_buf = nvim_create_buf(v:false, v:false)
        call nvim_buf_set_name(l:empty_buf, 'LanguageTool')
    endif
    call nvim_buf_set_option(l:empty_buf, 'filetype', 'languagetool')
    call nvim_buf_set_option(l:empty_buf, 'buftype', 'nowrite')
    call nvim_buf_set_option(l:empty_buf, 'bufhidden', 'delete')
    call nvim_buf_set_var(l:empty_buf, 'errors', a:errors)

    let l:to_put = []

    for l:error in a:errors
        let l:to_put += LanguageTool#errors#getSummary(l:error, a:flags)
    endfor

    call nvim_buf_set_lines(l:empty_buf, 0, -1, v:false, l:to_put)
    call nvim_buf_set_option(l:empty_buf, 'modifiable', v:false)

    call nvim_win_set_buf(a:window, l:empty_buf)
    call nvim_win_set_option(a:window, 'number', v:false)
    call nvim_win_set_option(a:window, 'relativenumber', v:false)
    call nvim_win_set_option(a:window, 'signcolumn', 'no')
    call nvim_win_set_option(a:window, 'foldmethod', 'syntax')

    execute win_id2tabwin(a:window)[1] . 'windo normal zx'
endfunction

