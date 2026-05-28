import * as themes from './theme/index';

const themeTypes = [
  'light',
  'dark',
  'default',
  'auritus',
  'azul',
  'beeInspired',
  'blue',
  'caravan',
  'carp',
  'chalk',
  'cool',
  'eduardo',
  'essos',
  'forest',
  'freshCut',
  'fruit',
  'gray',
  'green',
  'halloween',
  'helianthus',
  'infographic',
  'inspired',
  'jazz',
  'london',
  'macarons',
  'macarons2',
  'mint',
  'purplePassion',
  'redVelvet',
  'red',
  'roma',
  'royal',
  'sakura',
  'shine',
  'techBlue',
  'walden',
  'wef',
  'weforum',
  'westeros',
  'wonderland',
] as const;

export type EchartsThemeType = (typeof themeTypes)[number];

export function registerThemes(echarts) {
  themeTypes.forEach((themeName) => {
    registerTheme(echarts, themeName);
  });
}
export function registerTheme(echarts, themeName) {
  const theme = themes[themeName];
  echarts.registerTheme(themeName, theme.default);
}
