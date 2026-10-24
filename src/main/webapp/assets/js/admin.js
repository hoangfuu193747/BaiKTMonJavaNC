const navigationToggle = document.querySelector('[data-nav-toggle]');
const navigation = document.getElementById('app-navigation');
if (navigationToggle && navigation) {
 const closeNavigation = function (restoreFocus) {
  document.body.classList.remove('navigation-open');
  navigationToggle.setAttribute('aria-expanded', 'false');
  if (restoreFocus) navigationToggle.focus();
 };
 navigationToggle.addEventListener('click', function () {
  const expanded = document.body.classList.toggle('navigation-open');
  navigationToggle.setAttribute('aria-expanded', String(expanded));
 });
 document.addEventListener('click', function (event) {
  if (!navigation.contains(event.target) && !navigationToggle.contains(event.target)) closeNavigation(false);
 });
 document.addEventListener('keydown', function (event) {
  if (event.key === 'Escape' && document.body.classList.contains('navigation-open')) closeNavigation(true);
 });
 navigation.addEventListener('focusout', function (event) {
  if (!navigation.contains(event.relatedTarget) && event.relatedTarget !== navigationToggle) closeNavigation(false);
 });
 window.addEventListener('pageshow', function () { closeNavigation(false); });
}
const newPassword = document.getElementById('newPassword');
const confirmPassword = document.getElementById('confirmPassword');
if (newPassword && confirmPassword) {
 const validatePasswords = function () {
  confirmPassword.setCustomValidity(confirmPassword.value && confirmPassword.value !== newPassword.value ? 'Mật khẩu xác nhận chưa khớp.' : '');
  newPassword.setCustomValidity(new TextEncoder().encode(newPassword.value).length > 72 ? 'Mật khẩu tối đa 72 byte UTF-8.' : '');
 };
 newPassword.addEventListener('input', validatePasswords);
 confirmPassword.addEventListener('input', validatePasswords);
}
document.querySelector('[data-show-passwords]')?.addEventListener('change', function (event) {
 document.querySelectorAll('input[autocomplete="current-password"],input[autocomplete="new-password"]').forEach(function (input) {
  input.type = event.target.checked ? 'text' : 'password';
 });
});
document.addEventListener('submit', function (event) {
 const form = event.target;
 if (!form.matches('.admin-app form[method="post"]') || event.defaultPrevented) return;
 if (form.dataset.submitting === 'true') { event.preventDefault(); return; }
 form.dataset.submitting = 'true';
 form.setAttribute('aria-busy', 'true');
});
window.addEventListener('pageshow', function () {
 document.querySelectorAll('form[data-submitting]').forEach(function (form) {
  delete form.dataset.submitting;
  form.removeAttribute('aria-busy');
 });
});
