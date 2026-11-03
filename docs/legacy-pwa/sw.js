const CACHE_NAME="containertrack-v18-shell";
const SHELL=["./","./index.html","./manifest.webmanifest","./icon-192.png","./icon-512.png"];
self.addEventListener("install",e=>{e.waitUntil(caches.open(CACHE_NAME).then(c=>c.addAll(SHELL)));self.skipWaiting()});
self.addEventListener("activate",e=>{e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE_NAME).map(k=>caches.delete(k)))));self.clients.claim()});
self.addEventListener("fetch",e=>{
  if(e.request.method!=="GET")return;
  const u=new URL(e.request.url);
  if(u.origin!==self.location.origin){e.respondWith(fetch(e.request));return}
  e.respondWith(caches.match(e.request).then(cached=>cached||fetch(e.request).then(r=>{
    if(r&&r.status===200){const copy=r.clone();caches.open(CACHE_NAME).then(c=>c.put(e.request,copy).catch(()=>{}))}
    return r;
  }).catch(()=>e.request.mode==="navigate"?caches.match("./index.html"):Response.error())));
});

self.addEventListener("notificationclick", event => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({type:"window",includeUncontrolled:true}).then(list => {
      for(const client of list){
        if("focus" in client) return client.focus();
      }
      if(clients.openWindow) return clients.openWindow("./");
    })
  );
});
