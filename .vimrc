set expandtab
set shiftwidth=4
set tabstop=4

" Remove whitespace from end of lines when saving
autocmd BufWritePre * :%s/\s\+$//e
