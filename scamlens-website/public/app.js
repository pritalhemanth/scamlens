/*
 * app.js — small browser-side events. The website works without this file;
 * it only adds conveniences. All analysis happens in Java on the server.
 *
 * EVENT-DRIVEN: each block below waits for something the user does
 * (typing, a key press, a form submit) and reacts to it.
 */

// 1. Live character counter: runs every time the text changes ("input" event)
const messageBox = document.getElementById('message');
const charCount = document.getElementById('char-count');

if (messageBox && charCount) {
    const max = Number(messageBox.dataset.max);
    const updateCount = () => {
        const length = messageBox.value.length;
        charCount.textContent = `${length.toLocaleString()} / ${max.toLocaleString()}`;
        charCount.classList.toggle('over-limit', length > max);
    };
    messageBox.addEventListener('input', updateCount);
    updateCount();

    // 2. Ctrl + Enter (or Cmd + Enter on a Mac) submits the form ("keydown" event)
    messageBox.addEventListener('keydown', event => {
        if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
            event.preventDefault();
            messageBox.form.requestSubmit();
        }
    });
}

// 3. Ask "are you sure?" before deleting history ("submit" event)
document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => {
        if (!window.confirm(form.dataset.confirm)) {
            event.preventDefault();   // the user clicked Cancel, so don't delete
        }
    });
});
