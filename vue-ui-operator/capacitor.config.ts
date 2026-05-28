import { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.nearsolution.wcs",
  appName: "operator",
  webDir: "dist",
  server: {
    androidScheme: "http",
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 0,
    },
  },
};

export default config;
