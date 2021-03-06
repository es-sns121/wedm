jlab = jlab || {};
jlab.wedm = jlab.wedm || {};
jlab.wedm.classToObserverMap = jlab.wedm.classToObserverMap || {};
jlab.wedm.classToObserverMap['ActivePictureInPicture'] = 'jlab.wedm.PiPPvObserver';

jlab.wedm.PiPPvObserverInit = function () {
    jlab.wedm.PiPPvObserver = function (id, pvSet) {
        jlab.wedm.PvObserver.call(this, id, pvSet);
    };

    jlab.wedm.PiPPvObserver.prototype = Object.create(jlab.wedm.PvObserver.prototype);
    jlab.wedm.PiPPvObserver.prototype.constructor = jlab.wedm.PiPPvObserver;

    jlab.wedm.PiPPvObserver.prototype.handleControlUpdate = function (update) {
        var $obj = $("#" + this.id),
                value = update.value,
                $selected = $obj.find(".screen[data-index=" + value + "]");

        if ($selected.length) {
            $obj.find(".screen").hide();
            $selected.show();

            /* "refresh" callback to all widgets on re-display*/
            $selected.find(".ScreenObject").each(function () {
                var $o = $(this),
                        widget = jlab.wedm.idWidgetMap[$o.attr("id")];
                if (widget) { /*Not all objects have PvObserver widget*/
                    widget.refresh();
                }
            });
        }
    };
};

jlab.wedm.initPvObserver('jlab.wedm.PiPPvObserver', 'jlab.wedm.PvObserver');

jlab.wedm.initPip = function () {
    /*Only vertically center screens shorter than their parent*/
    $(".ActivePictureInPicture.pip-center .screen").each(function () {
        var $screen = $(this),
                $container = $screen.closest(".ActivePictureInPicture");
        if ($screen.outerHeight(true) < $container.height()) {
            $screen.addClass("vertical-center");
        }
    });

    /*Initially just how first screen in the stack*/
    $(".ActivePictureInPicture .screen:not(:first-child)").hide();
};

jlab.wedm.initFuncs = jlab.wedm.initFuncs || [];
jlab.wedm.initFuncs.push(jlab.wedm.initPip);