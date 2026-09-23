export interface Product {
  id: number;
  name: string;
  description: string;
  minAmount: number;
  maxAmount: number;
  maxTermMonths: number;
  monthlyRate: number;
  imageUrl: string;
  featured: boolean;
  active: boolean;
  inStock: boolean;
}
