import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    id: "/",
    name: "OISM POS",
    short_name: "OISM POS",
    description: "Hệ thống quản lý bán hàng OISM",
    start_url: "/",
    scope: "/",
    display: "standalone",
    background_color: "#ffffff",
    theme_color: "#0f172a",
    orientation: "portrait",
    icons: [
        {
         src: "/icons/icon-192.png",
         sizes: "192x192",
         type: "image/png",
         purpose: "any",
        },
        {
         src: "/icons/icon-512.png",
         sizes: "512x512",
         type: "image/png",
         purpose: "any",
        },
    ],
  };
}
