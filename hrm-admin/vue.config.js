module.exports = {
  lintOnSave: false, // 关闭语法检查
  devServer: {
    proxy: {
      '/api': {
        target: process.env.VUE_APP_BACKEND_HOST + ':' + process.env.VUE_APP_BACKEND_PORT,
        pathRewrite: { '^/api': '' },
        changeOrigin: true
      }
    }
  }
}
