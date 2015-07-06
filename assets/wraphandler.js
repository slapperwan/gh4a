window.applyLineWrapping = function(enabled) {
    var s = document.getElementById('linewrapping');
    if (!s) {
        s = document.createElement('style');
        s.setAttribute('id', 'linewrapping');
        (document.head || document.body).appendChild(s);
    }

    s.textContent = enabled ? 'pre { white-space: pre-wrap }' : '';
};
