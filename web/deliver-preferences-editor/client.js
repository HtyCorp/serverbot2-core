
function devUnhidePanes() {
    let hiddenElements = document.getElementsByClassName("hide-contents");
    for (let i = 0; i < hiddenElements.length; i++) {
        let pane = hiddenElements.item(i);
        pane.classList.remove("hide-contents");
    }
}

function authenticatedPost(endpointUrl, authToken, payload) {
    let payloadWithAuth = { "xWebAuthToken": authToken };
    Object.assign(payloadWithAuth, payload);
    let response = fetch(endpointUrl, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-admiralbot-json"
        },
        body: JSON.stringify(payloadWithAuth)
    });
    return response.json();
}