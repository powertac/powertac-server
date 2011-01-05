$(document).ready(function() {
    
    // collapse all boxes (flot needs to have them visible to plot...)
    setTimeout(function() {
        $(document).find(".more").toggle();
    }, 1);
    
    $(".box").click(function() {

        // check if there is something to toggle
        if ($(this).is(".box-collapsed") || $(this).is(".box-expanded")) {
            // toggle box. should be moved into ajax at some point
            $(this).toggleClass("box-collapsed box-expanded");
            $(this).find(".more").slideToggle("fast");
        }

        return false;
    });

    $(".box a").click(function(event) {
        $(location).attr("href", $(this).attr("href"));
        return false;
    });

    $(".box-navlink").click(function() {
        // find link in box
        var link = $(this).find(".navlink").attr("href");
        // and redirect
        if (link) {
            $(location).attr("href", link);
            return false;
        }
    });

    $(".box-navlink a").click(function(event) {
        // ignore any clicks on the real link
        event.preventDefault();
    });

    // cycle logos
    $("#universities").cycle({ fx: 'fade', speed: 750, timeout: 7500 });
    
});