module.exports = {
  lintOnSave: false, // 关闭语法检查
  devServer: {
    headers: {
      'Permissions-Policy': 'unload=*' // Chrome 新版禁止 unload 事件，SockJS 需要此权限
    },
    proxy: {
      '/api': {
        target: process.env.VUE_APP_BACKEND_HOST + ':' + process.env.VUE_APP_BACKEND_PORT,
        pathRewrite: { '^/api': '' },
        changeOrigin: true
      }
    }
  }
}
