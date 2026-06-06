import ElementUI from 'element-ui'

// 将后端传来的文件流写入文件
export const write = (response, filename) => {
  if (response.data.size > 0) {
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    setTimeout(function () {
      // 移除内存中的临时文件路径和为下载而创建的a标签
      window.URL.revokeObjectURL(url)
      link.remove()
    }, 10000)
  } else {
    ElementUI.Message({
      message: '文件不存在！',
      type: 'error',
      duration: 5 * 1000
    })
  }
}
