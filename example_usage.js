var AndroidWebPrint = function (useHttps, checkRelay) {
        var usehttps = false;
        this.print = function (data) {
            if (!usehttps){
                sendHttpData(data);
            }
            if (!pwindow || pwindow.closed) {
                openPrintWindow();
                setTimeout(function () {
                    sendData(data);
                }, 220);
            }
            sendData(data);
        };

        function sendData(data) {
            pwindow.postMessage(data, "*");
        }

        var pwindow;
        function openPrintWindow() {
            pwindow = window.open("http://" + curset.recip + ":" + curset.rectcpport + "/printwindow", 'AndroidPrintService');
            pwindow.blur();
            window.focus();
        }

        var timeOut;
        this.checkRelay = function () {
            if (!usehttps){
                checkHttpRelay();
                return;
            }
            if (pwindow && pwindow.open) {
                pwindow.close();
            }
            window.addEventListener("message", message, false);
            openPrintWindow();
            timeOut = setTimeout(dispatchAndroid, 2000);
        };

        function message(event) {
            if (event.origin != "http://" + curset.recip + ":" + curset.rectcpport)
                return;
            if (event.data=="init"){
                clearTimeout(timeOut);
                alert("The Android print service has been loaded in a new tab, keep it open for faster printing.");
            }
        }

        function dispatchAndroid() {
            var answer = confirm("Would you like to open/install the printing app?");
            if (answer) {
                document.location.href = "https://wallaceit.com.au/playstore/httpsocketadaptor/index.php";
            }
        }

        function sendHttpData(data) {
            try {
                data = encodeURI(data);
                var response = $.ajax({
                    url: "http://" + curset.recip + ":" + curset.rectcpport,
                    type: "POST",
                    data: data,
                    processData: false,
                    crossDomain: true,
                    crossOrigin: true,
                    timeout: 2000,
                    async: false
                });
                return response.status == 200;
            } catch (ex) {
                alert("Error sending data to the socket relay.");
                return false;
            }
        }

        function checkHttpRelay() {
            $("#printstat").show();
            $.ajax({
                url: "http://127.0.0.1/",
                type: "GET",
                crossDomain: true,
                crossOrigin: true,
                timeout: 2000,
                success: function () {
                },
                error: function () {
                    dispatchAndroid();
                }
            });
        }

        if (checkRelay) this.checkRelay();
        return this;
};
// usage
var webprint = new AndroidWebPrint(true, true);
webprint.print("some raw socket data");