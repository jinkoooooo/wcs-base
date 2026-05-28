export function getItemCode(sku: string): string {
  if (!sku) return '';
  const parts = sku.split('*');
  return parts[0] || '';
}