import { getValidateCode } from '@/api/login'

export const setValidateCode = (img) => {
  getValidateCode().then(response => {
    img.src = window.URL.createObjectURL(new Blob([response.data]))
    img.onload = () => {
      URL.revokeObjectURL(img.src)
    }
  })
}
