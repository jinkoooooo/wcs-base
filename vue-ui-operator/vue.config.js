const { defineConfig } = require("@vue/cli-service");
module.exports = defineConfig({
  outputDir: "www",
  publicPath: "",
  pluginOptions: {
    cordovaPath: "src-cordova",
  },
});
