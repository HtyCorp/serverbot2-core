function devUnhidePanes() {
    let hiddenElements = document.getElementsByClassName("hide-contents");
    for (let i = 0; i < hiddenElements.length; i++) {
        let pane = hiddenElements.item(i);
        pane.classList.remove("hide-contents");
    }
}