// "Show password" checkbox (AT-59).
// A checkbox marked data-show-password switches every password box in the same form
// between hidden dots and readable text.
document.addEventListener('change', function (event) {
    if (!event.target.matches('[data-show-password]')) {
        return;
    }
    var form = event.target.closest('form');
    // password boxes are named currentPassword, newPassword, confirmPassword or password
    var boxes = form.querySelectorAll('input[name*="assword"]');
    for (var i = 0; i < boxes.length; i++) {
        boxes[i].type = event.target.checked ? 'text' : 'password';
    }
});
