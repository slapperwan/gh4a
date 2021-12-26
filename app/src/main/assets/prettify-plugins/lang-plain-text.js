// OctoDroid addition
// This plugin is used to prevent *.txt (and in general plain text files) from being syntax highlighted

PR['registerLangHandler'](
  PR['createSimpleLexer'](
    [],
    [[PR['PR_PLAIN'], /^.*/]]
  ),
  ['txt', 'plain-text']
);
