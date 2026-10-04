# Market UI - ITEMPAZAR Resource Pack

## Tanım
Bu resource pack, Minecraft Bedrock Edition için tam işlevsel bir Market/Auction House UI sistemidir.

## Dosya Yapısı

```
PazarUI - Kopya/
├── manifest.json
├── market_config.json
├── item_prices.json
├── README.md
├── textures/
│   ├── ui/
│   │   ├── close_button.json
│   │   ├── buttons.json
│   │   ├── panels.json
│   │   ├── earthsmp_logo.png
│   │   ├── pz_white.png
│   │   └── [diğer UI textureları]
│   ├── items/
│   │   ├── item_mapping.json
│   │   └── [tüm item texture dosyaları]
│   └── blocks/
│       └── [blok texture dosyaları]
└── ui/
    └── server_form.json
```

## Kurulum Adımları

### 1. Resource Pack Yükleme
- Bu klasörü Minecraft resource packs dizinine kopyalayın
- Settings → Resource Packs'te etkinleştirin

### 2. Item Texture Ekleme
- `textures/items/` klasörünü doldurun
- `item_mapping.json`'da her item için texture adı tanımlı

### 3. Plugin Entegrasyonu
- Sunucu plugini item verilerini `item_prices.json`'dan okumalı
- Her item `id`, `texture`, `price` alanlarını kullanmalı

## Konfigürasyon

### market_config.json
- `items_per_page`: Sayfada kaç item gösterilmesi (varsayılan: 30)
- `grid_columns`: Grid kolon sayısı (varsayılan: 10)
- `grid_rows`: Grid satır sayısı (varsayılan: 3)
- `categories`: Market kategorileri
- `ui_settings`: Renk ayarları

### item_prices.json
- Her item için fiyat ve özellikler tanımlanır
- Rarity türleri: common, uncommon, rare, epic, legendary, mythic
- In_stock: Öğenin stokta olup olmadığını belirtir

## UI Elementleri

### Header
- Başlık: "ITEMPAZAR"
- Logo: EarthSMP logosu
- Kapatma Butonu: Sağ üst köşe

### Kontrol Paneli
- Sayfa Göstergesi: Sayfa 1/36
- Market Tab: Aktif sekme
- Orders Tab: İstatistikler
- Arama İkonu
- İnfo İkonu

### Grid Alanı
- 10x3 grid (30 item)
- Her hücre item simgesi + fiyat + satın al butonu
- Fiyat yeşil renk ile gösterilir
- Satın al butonu yeşil arka planlı

### Alt Butonlar
- İlanlarınız
- İddialar
- Filtreler
- Liste Item
- Sonraki Sayfa (→)

## Texture Boyutları

- UI Texture: 256x256 px (PNG)
- Item Icon: 32x32 px (PNG)
- Logo: 90x20 px (PNG)
- Button: 84x24 px (PNG)

## Renk Şeması

```json
{
  "header_red": [0.95, 0.15, 0.15],
  "panel_dark": [0.15, 0.15, 0.15],
  "button_gray": [0.65, 0.65, 0.65],
  "price_green": [0.35, 1.00, 0.35],
  "buy_green": [0.35, 0.55, 0.25],
  "text_white": [1.00, 1.00, 1.00],
  "text_dark": [0.20, 0.20, 0.20]
}
```

## Gerekli Plugin Fonksiyonları

### 1. Market Aç
```java
player.showForm(marketUI);
```

### 2. Item Yükle
```java
formButtons.add({
  "id": item.id,
  "text": item.price,
  "texture": item.texture
});
```

### 3. Sayfalandırma
```java
currentPage = 1;
maxPages = Math.ceil(totalItems / 30);
```

### 4. Satın Al İşlemi
```java
if (buttonClicked == "buy") {
  processTransaction(playerId, itemId, itemPrice);
}
```

## Sorun Giderme

### Market UI açılmıyor
- Resource Pack yüklü mü kontrol edin
- manifest.json geçerli mi kontrol edin
- Plugin sunucuda çalışıyor mu kontrol edin

### Item göreseli görünmüyor
- `item_mapping.json`'da texture adı doğru mu kontrol edin
- PNG dosyası correct dizinde mi kontrol edin
- Texture dosyası 32x32 px mi kontrol edin

### Fiyat göründü ama item görüntüsü yoksa
- `#form_button_texture` binding kontrol edin
- Plugin item veri yapısı doğru mu kontrol edin
- Item collection_name "form_buttons" mi kontrol edin

## Sürüm Bilgisi

- Format Version: 2
- Min Engine Version: 1.21.0
- Resource Pack Version: 1.4.0
- UI Layout: Minecraft Bedrock Edition uyumlu

## Lisans ve Kredi

EarthSMP Market UI Package
Tüm hakları saklıdır.
