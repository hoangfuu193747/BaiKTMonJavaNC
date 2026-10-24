document.addEventListener('submit', function (event) {
 const message = event.target.dataset.confirm;
 if (message && !window.confirm(message)) event.preventDefault();
});
document.addEventListener('click', function (event) {
 if (event.target.closest('[data-print]')) window.print();
});
document.querySelectorAll('[data-submit-on-change]').forEach(function (select) {
 select.addEventListener('change', function () {
  if (select.value) select.form.requestSubmit();
 });
});

document.querySelectorAll('[data-profile-dropdown]').forEach(function (dropdown) {
 const toggle = dropdown.querySelector('[data-profile-toggle]');
 const panel = dropdown.querySelector('.profile-panel');
 const items = function () { return Array.from(panel.querySelectorAll('[role="menuitem"]:not(:disabled)')); };
 const close = function (restoreFocus) {
  panel.hidden = true;
  toggle.setAttribute('aria-expanded', 'false');
  if (restoreFocus) toggle.focus();
 };
 const open = function (focusLast) {
  panel.hidden = false;
  toggle.setAttribute('aria-expanded', 'true');
  const links = items();
  (focusLast ? links[links.length - 1] : links[0])?.focus();
 };
 toggle.addEventListener('click', function () {
  if (panel.hidden) open(false); else close(false);
 });
 toggle.addEventListener('keydown', function (event) {
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
   event.preventDefault();
   event.stopPropagation();
   open(event.key === 'ArrowUp');
  }
 });
 dropdown.addEventListener('keydown', function (event) {
  if (panel.hidden) return;
  if (event.key === 'Escape') { event.preventDefault(); close(true); return; }
  const links = items();
  const current = links.indexOf(document.activeElement);
  let next;
  if (event.key === 'ArrowDown') next = (current + 1) % links.length;
  if (event.key === 'ArrowUp') next = (current - 1 + links.length) % links.length;
  if (event.key === 'Home') next = 0;
  if (event.key === 'End') next = links.length - 1;
  if (next !== undefined) { event.preventDefault(); links[next].focus(); }
 });
 document.addEventListener('click', function (event) {
  if (!dropdown.contains(event.target)) close(false);
 });
 document.addEventListener('focusin', function (event) {
  if (!dropdown.contains(event.target)) close(false);
 });
 window.addEventListener('pageshow', function () { close(false); });
});

