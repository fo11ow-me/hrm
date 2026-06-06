module.exports = {
  presets: [
    '@vue/cli-plugin-babel/preset'
  ],
  plugins: [ // 按需引入
    [
      'component',
      {
        libraryName: 'element-ui',
        styleLibraryName: 'theme-chalk'
      }
    ]
  ]

}
