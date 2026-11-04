declare module 'jsbarcode' {
  const JsBarcode: (element: SVGSVGElement | HTMLCanvasElement | string, text: string, options?: Record<string, unknown>) => void
  export default JsBarcode
}
