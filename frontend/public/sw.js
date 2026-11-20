const scope = self.registration.scope;
const base = new URL(scope).pathname;

function absolute(path) {
  return new URL(path || base, self.location.origin).href;
}

self.addEventListener("push", (event) => {
  let data = {};
  if (event.data) {
    try {
      data = event.data.json();
    } catch {
      data = {};
    }
  }
  const title = data.title || "Comunidad 360";
  event.waitUntil(
    self.registration.showNotification(title, {
      body: data.body || "",
      icon: absolute(`${base}icon.svg`),
      badge: absolute(`${base}icon.svg`),
      data: { url: data.url ? absolute(data.url) : scope },
    }),
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const target = event.notification.data?.url || scope;
  event.waitUntil(
    (async () => {
      const windows = await clients.matchAll({ type: "window", includeUncontrolled: true });
      for (const client of windows) {
        if (client.url === target && "focus" in client) {
          return client.focus();
        }
      }
      return clients.openWindow(target);
    })(),
  );
});
