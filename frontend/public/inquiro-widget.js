(function () {
  "use strict";

  var script = document.currentScript;
  if (!script) return;

  var channelId = script.getAttribute("data-channel-id");
  if (!channelId) {
    console.warn("Inquiro widget: data-channel-id is required.");
    return;
  }

  var src = script.src;
  var origin = new URL(src, window.location.href).origin;
  var iframe = document.createElement("iframe");
  iframe.title = "Inquiro AI receptionist";
  iframe.src = origin + "/widget?channelId=" + encodeURIComponent(channelId) + "&siteOrigin=" + encodeURIComponent(window.location.origin);
  iframe.setAttribute("loading", "lazy");
  iframe.style.position = "fixed";
  iframe.style.right = "20px";
  iframe.style.bottom = "20px";
  iframe.style.width = "390px";
  iframe.style.height = "620px";
  iframe.style.maxWidth = "calc(100vw - 40px)";
  iframe.style.maxHeight = "calc(100vh - 40px)";
  iframe.style.border = "0";
  iframe.style.borderRadius = "18px";
  iframe.style.boxShadow = "0 18px 55px rgba(20, 27, 50, .20)";
  iframe.style.zIndex = "2147483647";
  iframe.style.background = "transparent";
  document.body.appendChild(iframe);
})();
