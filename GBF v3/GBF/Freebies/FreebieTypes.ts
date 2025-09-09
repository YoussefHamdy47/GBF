export interface IFreebie {
  name: string;
  link: string;
  originalPrice: number;
  freeUntil: Date | number;
  platform: Platform;
}

export enum Platform {
  EGS = "Epic Games",
  Steam = "Steam",
  GOG = "GOG",
  EA = "Origin / EA",
  Ubi = "Ubisoft",
  Other = "Other",
}

// API Response

export type KeyImage = {
  type: string;
  url: string;
};

export type Seller = {
  id: string;
  name: string;
};

export type Item = {
  id: string;
  namespace: string;
};

export type CustomAttribute = {
  key: string;
  value: string;
};

export type Category = {
  path: string;
};

export type Tag = {
  id: string;
};

export type CatalogNs = {
  mappings: { pageSlug: string; pageType: string }[];
};

export type OfferMapping = Record<string, never>;

export type Price = {
  totalPrice: {
    discountPrice: number;
    originalPrice: number;
    voucherDiscount: number;
    discount: number;
    currencyCode: string;
    currencyInfo: { decimals: number };
    fmtPrice: {
      originalPrice: string;
      discountPrice: string;
      intermediatePrice: string;
    };
  };
  lineOffers: { appliedRules: any[] }[];
};

export type DiscountSetting = {
  discountType: string;
  discountPercentage: number;
};

export type PromotionalOffer = {
  startDate: string;
  endDate: string;
  discountSetting: DiscountSetting;
};

export type Promotions = {
  promotionalOffers: { promotionalOffers: PromotionalOffer[] }[];
  upcomingPromotionalOffers: { promotionalOffers: PromotionalOffer[] }[];
};

export type FreeGame = {
  title: string;
  id: string;
  namespace: string;
  description: string;
  effectiveDate: string;
  offerType: string;
  expiryDate: string | null;
  status: string;
  isCodeRedemptionOnly: boolean;
  keyImages: KeyImage[];
  seller: Seller;
  productSlug: string;
  urlSlug: string;
  url: string | null;
  items: Item[];
  customAttributes: CustomAttribute[];
  categories: Category[];
  tags: Tag[];
  catalogNs: CatalogNs;
  offerMappings: OfferMapping[];
  price: Price;
  promotions: Promotions;
};

export type FreeGamesData = {
  freeGames: {
    current: FreeGame[];
    upcoming: FreeGame[];
  };
};
