document.addEventListener("DOMContentLoaded", () => {
  const container = document.getElementById("dynamic-content");

  // Simulated dynamic data load
  setTimeout(() => {
    container.innerHTML = `
      <p><strong>Dynamic Content:</strong> Loaded via JS after 1s delay.</p>
      <ul>
        <li>JS-native bridge</li>
        <li>Caching with version control</li>
        <li>Offline support</li>
      </ul>
    `;
  }, 1000);
});
