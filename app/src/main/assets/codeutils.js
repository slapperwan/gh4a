window.applyLineWrapping = function(enabled) {
    var s = document.getElementById('linewrapping');
    if (!s) {
        s = document.createElement('style');
        s.setAttribute('id', 'linewrapping');
        (document.head || document.body).appendChild(s);
    }

    s.textContent = enabled ? 'pre { white-space: pre-wrap }' : '';
};

window.addClickListeners = function() {
    var pre = document.getElementById("content");
    var ol = pre != null ? pre.getElementsByTagName("ol")[0] : null;
    var lines = ol != null ? ol.getElementsByTagName("li") : null;
    if (lines != null) {
        function listener(target, line, event) {
            if (event.target === target) {
                NativeClient.onLineTouched(line);
            }
        }
        for (i = 0; i < lines.length; i++) {
            lines[i].addEventListener('click', listener.bind(null, lines[i], i + 1));
        }
    }
};

window.scrollToHighlight = function() {
    if (!window.highlightTop || !window.highlightBottom || !window.innerHeight) {
        return;
    }

    var highlightHeight = window.highlightBottom - window.highlightTop;
    var scrollTarget = window.highlightTop;

    if (highlightHeight < window.innerHeight) {
        // center highlighted area in window
        var offset = (window.innerHeight - highlightHeight) / 2;
        scrollTarget -= offset;
    }

    window.scrollTo(0, scrollTarget);

    // make sure to only do this once
    window.highlightTop = 0;
    window.highlightBottom = 0;
};

window.highlightLines = function(from, to) {
    if (from < 0) {
        return;
    }

    var pre = document.getElementById("content");
    var ol = pre != null ? pre.getElementsByTagName("ol")[0] : null;
    var lines = ol != null ? ol.getElementsByTagName("li") : null;
    if (lines != null && lines.length >= from && (to < 0 || lines.length >= to)) {
        var first = from - 1;
        var last = (to < 0 ? from : to) - 1;
        for (i = first; i <= last; i++) {
            lines[i].className += " highlighted";
        }

        var top = 0, bottom = lines[last].offsetHeight;
        for (elem = lines[first]; elem != null; elem = elem.offsetParent) {
            top += elem.offsetTop;
        }
        for (elem = lines[last]; elem != null; elem = elem.offsetParent) {
            bottom += elem.offsetTop;
        }

        window.highlightTop = top;
        window.highlightBottom = bottom;
        window.scrollToHighlight();
    }
};

window.highlightDiffLines = function(from, to) {
    if (from < 0) {
        return;
    }

    var first = document.getElementById("line" + from);
    var last = document.getElementById("line" + to);
    if (first != null && last != null) {
	for (var i = from; i <= to; i++) {
	    document.getElementById("line" + i).className += " highlighted";
	}

        var top = 0, bottom = last.offsetHeight;
        for (elem = first; elem != null; elem = elem.offsetParent) {
            top += elem.offsetTop;
        }
        for (elem = last; elem != null; elem = elem.offsetParent) {
            bottom += elem.offsetTop;
        }

        window.highlightTop = top;
        window.highlightBottom = bottom;
        window.scrollToHighlight();
    }
};

window.scrollToElement = function(elemName) {
    if (!elemName) {
        return;
    }

    var elem = document.getElementById(elemName);
    if (elem) {
         var top = 0, bottom = elem.offsetHeight;
         while (elem) {
            top += elem.offsetTop;
            bottom += elem.offsetTop;
            elem = elem.offsetParent;
         }

         window.highlightTop = top;
         window.highlightBottom = bottom;
         window.scrollToHighlight();
    }
}
