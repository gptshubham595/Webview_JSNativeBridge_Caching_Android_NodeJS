function notifyAndroid() {
  if (window.AndroidInterface && window.AndroidInterface.onMessageFromWeb) {
    window.AndroidInterface.onMessageFromWeb("Hello from WebView!");
    document.getElementById("status").innerText = "Message sent to native";
  } else {
    alert("AndroidInterface not available.");
  }
}

function showAlert() {
  if (window.AndroidInterface && window.AndroidInterface.showAlert) {
    window.AndroidInterface.showAlert("This is a native Android alert!");
  } else {
    alert("AndroidInterface not available.");
  }
}

function closeWebView() {
  if (window.AndroidInterface && window.AndroidInterface.closeWebView) {
    window.AndroidInterface.closeWebView();
  } else {
    alert("AndroidInterface not available.");
  }
}

function openContacts() {
  if (window.AndroidInterface && window.AndroidInterface.openContacts) {
    window.AndroidInterface.openContacts();
  } else {
    alert("AndroidInterface not available.");
  }
}
