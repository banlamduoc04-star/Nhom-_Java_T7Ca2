"use client";

import { useEffect } from "react";

export default function ServiceWorkerRegistration() {
  useEffect(() => {
    if (!("serviceWorker" in navigator)) {
      console.warn("Trình duyệt không hỗ trợ Service Worker");
      return;
    }

    navigator.serviceWorker
      .register("/sw.js")
      .then((registration) => {
        console.log(
          "Service Worker đã đăng ký:",
          registration.scope
        );
      })
      .catch((error) => {
        console.error(
          "Không thể đăng ký Service Worker:",
          error
        );
      });
  }, []);

  return null;
}
