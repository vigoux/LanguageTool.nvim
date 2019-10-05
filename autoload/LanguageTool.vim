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

" Set up configuration.
" Returns 0 if success, < 0 in case of error.
function! LanguageTool#setup() "{{{1
    let s:languagetool_server = get(g:, 'languagetool_server', $HOME . '/languagetool/languagetool-server.jar')
    let s:summary_pp_flags = get(g:, 'languagetool_summary_flags', '')
    let s:preview_pp_flags = get(g:, 'languagetool_preview_flags', '')

    if !filereadable(expand(s:languagetool_server, v:true))
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
        " Open preview window and get its win_id
        pedit LanguageToolError
        wincmd P
        let win_id = win_getid()
        exe "norm! \<C-W>\<C-P>"

        call LanguageTool#ui#displayInWindow([l:error], win_id, 'LanguageToolError', s:preview_pp_flags)
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
    if !bufloaded('LanguageToolSummary')
        wincmd v
        let l:window = win_getid()
        exe 'norm! \<C-W>\<C-P>'
        setlocal modifiable
    else
        let l:window = bufwinid('LanguageToolSummary')
        setlocal modifiable
    endif

    call LanguageTool#ui#displayInWindow(l:errors, l:window, 'LanguageToolSummary', s:summary_pp_flags)
endfunction
