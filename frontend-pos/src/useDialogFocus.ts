import { useEffect, type RefObject } from 'react'

const FOCUSABLE = 'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])'

/**
 * Giu tieu diem ban phim trong hop thoai dang mo (WCAG 2.4.3):
 * Tab / Shift+Tab chi xoay vong trong hop thoai, dong lai thi tra tieu diem ve cho cu.
 */
export function useDialogFocus(ref: RefObject<HTMLElement | null>, open: boolean) {
  useEffect(() => {
    if (!open) return
    const previous = document.activeElement as HTMLElement | null
    const dialog = ref.current
    // Neu chua co o nao trong hop thoai nhan tieu diem thi dua vao o dau tien
    const raf = requestAnimationFrame(() => {
      if (dialog && !dialog.contains(document.activeElement)) {
        dialog.querySelector<HTMLElement>(FOCUSABLE)?.focus()
      }
    })

    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab' || !dialog) return
      const items = [...dialog.querySelectorAll<HTMLElement>(FOCUSABLE)].filter((el) => el.offsetParent !== null)
      if (items.length === 0) {
        e.preventDefault()
        return
      }
      const first = items[0]
      const last = items[items.length - 1]
      const active = document.activeElement
      if (e.shiftKey && (active === first || !dialog.contains(active))) {
        e.preventDefault()
        last.focus()
      } else if (!e.shiftKey && (active === last || !dialog.contains(active))) {
        e.preventDefault()
        first.focus()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => {
      cancelAnimationFrame(raf)
      document.removeEventListener('keydown', onKey)
      // Chi tra tieu diem khi chua ai nhan (vd. Esc da tu dua tieu diem ve o tim san pham thi giu nguyen)
      const unclaimed = document.activeElement === null || document.activeElement === document.body
      if (unclaimed && previous && document.contains(previous)) previous.focus()
    }
  }, [ref, open])
}
