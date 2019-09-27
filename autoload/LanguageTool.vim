" LanguageTool: Grammar checker in Vim for English, French, German, etc.
" Maintainer:   Thomas Vigouroux <tomvig38@gmail.com>
" Last Change:  2019 Sep 27
" Version:      1.0
"
" License: {{{1
"
" The VIM LICENSE applies to LanguageTool.nvim plugin
" (see ":help copyright" except use "LanguageTool.nvim" instead of "Vim").
"
" }}} 1

" Set up configuration.
" Returns 0 if success, < 0 in case of error.
function! LanguageTool#setup() "{{{1
    let s:languagetool_server = get(g:, 'languagetool_server', $HOME . '/languagetool/languagetool-server.jar')
    let s:summary_pp_flags = get(g:, 'languagetool_summary_flags', '')
    let s:preview_pp_flags = get(g:, 'languagetool_preview_flags', '')

    if !filereadable(expand(s:languagetool_server))
        echomsg "LanguageTool cannot be found at: " . s:languagetool_server
        echomsg "You need to install LanguageTool and/or set up g:languagetool_server"
        echomsg "to indicate the location of the languagetool-server.jar file."
        return -1
    endif

    call LanguageTool#server#start(s:languagetool_server)

    return 0
endfunction

" This function performs grammar checking of text in the current buffer.
" It highlights grammar mistakes in current buffer and opens a scratch
" window with all errors found.  It also populates the location-list of
" the window with all errors.
" a:line1 and a:line2 parameters are the first and last line number of
" the range of line to check.
" Returns 0 if success, < 0 in case of error.
function! LanguageTool#check() abort "{{{1
    " Using window ID is more reliable than window number.
    " But win_getid() does not exist in old version of Vim.
    let l:file_content = system('cat ' . expand('%'))

    let data = LanguageTool#config#get()
    let data['file'] = '%'
    let data['text'] = getline(1, line('$'))

    call LanguageTool#server#check(data, function('LanguageTool#check#callback'))
endfunction

" This function clears syntax highlighting created by LanguageTool plugin
" and removes the scratch window containing grammar errors.
function! LanguageTool#clear() "{{{1
    call setmatches(filter(getmatches(), 'v:val["group"] !~# "LanguageTool.*Error"'))
    lexpr ''
    lclose
endfunction

" This function shows the error at point in the preview window
function! LanguageTool#showErrorAtPoint() "{{{1
    let error = LanguageTool#errors#find()
    if !empty(error)
        " Open preview window and jump to it
        pedit LanguageToolError
        wincmd P
        setlocal modifiable

        call clearmatches()

        call nvim_buf_set_lines(0, 0, -1, v:false, LanguageTool#errors#getSummary(l:error, s:preview_pp_flags))

        let b:error = l:error

        setlocal filetype=languagetool
        setlocal buftype=nowrite bufhidden=wipe nobuflisted noswapfile nowrap nonumber norelativenumber noma
        " Map <CR> to fix error with suggestion at point
        nnoremap <buffer> f :call LanguageTool#fixErrorWithSuggestionAtPoint()<CR>

        " Return to original window
        exe "norm! \<C-W>\<C-P>"
        return
    endif
endfunction

" This function is used to fix error in the preview window using
" the suggestion under cursor
function! LanguageTool#fixErrorWithSuggestionAtPoint() "{{{1
    let l:suggestion_id = LanguageTool#errors#suggestionAtPoint()
    if l:suggestion_id >= 0
        let l:error_to_fix = b:error

        call LanguageTool#errors#fix(l:error_to_fix, l:suggestion_id)
    endif
endfunction

" This function is used to fix the error at point using suggestion nr sug_id
function! LanguageTool#fixErrorAtPoint(sug_id) "{{{1
    call LanguageTool#errors#fix(LanguageTool#errors#find(), a:sug_id)
endfunction

" This function opens a new window with all errors in the current buffer
" and mappings to navigate to them, and fix them
function! LanguageTool#summary() "{{{1
    let l:errors = b:errors
    " Open a new window or jump to current
    if !bufloaded('LanguageTool')
        wincmd v
        e LanguageTool
        setlocal modifiable
    else
        call win_gotoid(bufwinid('LanguageTool'))
        setlocal modifiable
        execute '0,$delete'
    endif

    let l:to_put = []

    for l:error in l:errors
        let l:to_put += LanguageTool#errors#getSummary(l:error, s:summary_pp_flags)
    endfor

    call nvim_buf_set_lines(0, 0, -1, v:false, l:to_put)

    " We need to transfer the errors to this buffer
    let b:errors = l:errors

    setlocal filetype=languagetool
    setlocal buftype=nowrite bufhidden=wipe nobuflisted noswapfile nowrap nonumber norelativenumber noma
endfunction

" }}}1


" This function starts the remote-plugin

function! LanguageTool#getCommand()

    let s:socket = serverlist()[0]
    let l:plugin_path = expand('<sfile>:p:h')
    let l:command = l:plugin_path . '/gradlew run --args="' . s:socket . '" > ' . l:plugin_path . '/LanguageTool.logs'

    return l:command
endfunction

function! LanguageTool#start()
    let s:job = jobstart(LanguageTool#getCommand())
endfunction

function! LanguageTool#getChannelId()
    return s:job
endfunction

function! LanguageTool#stop()
    call jobstop(s:job)
endfunction

" This function sends a:command to the running remote plugin
function! LanguageTool#sendCommand(command, ...)
endfunction
