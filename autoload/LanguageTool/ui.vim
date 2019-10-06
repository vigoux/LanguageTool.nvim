" LanguageTool: Grammar checker in Vim for English, French, German, etc.
" Maintainer:   Thomas Vigouroux <tomvig38@gmail.com>
" Last Change:  2019 Oct 04
" Version:      1.0
"
" License: {{{1
"
" The VIM LICENSE applies to LanguageTool.nvim plugin
" (see ":help copyright" except use "LanguageTool.nvim" instead of "Vim").
"
" }}} 1

" This function displays errors summaries, using flags, and in window with handle window
function! LanguageTool#ui#displayInWindow(errors, window, buffername, flags) "{{{1
    if bufnr(a:buffername) > 0
        let buffer = bufnr(a:buffername)
    else
        let buffer = nvim_create_buf(v:false, v:false)
        call nvim_buf_set_name(buffer, a:buffername)
    endif

    let lines_to_put = []

    for l:error in a:errors
        let lines_to_put += LanguageTool#errors#getSummary(l:error, a:flags)
    endfor

    call nvim_buf_set_lines(buffer, 0, -1, v:false, lines_to_put)

    " Set buffer options
    call nvim_buf_set_option(buffer, 'filetype', 'languagetool')
    call nvim_buf_set_option(buffer, 'buftype', 'nowrite')
    call nvim_buf_set_option(buffer, 'bufhidden', 'wipe')
    call nvim_buf_set_option(buffer, 'buflisted', v:false)
    call nvim_buf_set_option(buffer, 'swapfile', v:false)
    call nvim_buf_set_var(buffer, 'errors', a:errors)

    call nvim_win_set_buf(a:window, buffer)
endfunction "}}}1

" This function opens a temporary floatting window that will be closed on CursorMoved
function! LanguageTool#ui#createTemporaryFloatWin() "{{{
    let s:lt_temp_win = nvim_open_win(0, v:false,
                \ {
                \ 'relative' : 'cursor',
                \ 'width' : 50,
                \ 'height' : 2,
                \ 'row' : 1,
                \ 'col' : 1,
                \ 'style' : 'minimal'
                \ })

    augroup LTFloatingWin
        autocmd!
        autocmd CursorMoved * call s:AutoCloseFloat()
    augroup END

    return s:lt_temp_win
endfunction "}}}

function! s:AutoCloseFloat() "{{{
    call nvim_win_close(s:lt_temp_win, v:true)
    autocmd! LTFloatingWin
endfunction "}}}
