# Market UI - Asset Checklist

## Gerekli Texture Dosyaları

### UI Textures (textures/ui/)

#### Mevcut Dosyalar ✓
- [x] earthsmp_logo.png (EarthSMP logosu)
- [x] pz_white.png (Beyaz temel texture)

#### Eksik Dosyalar (Eklenecek)
- [ ] close_button.png - Kapatma butonu (28x28)
- [ ] button_dark.png - Koyu buton arka planı (256x256)
- [ ] button_light.png - Açık buton arka planı (256x256)
- [ ] button_red.png - Kırmızı buton (256x256)
- [ ] button_green.png - Yeşil buton (256x256)
- [ ] info_icon.png - İnfo simgesi (24x24)
- [ ] search_icon.png - Arama simgesi (24x24)
- [ ] panel_base.png - Panel temel (256x256)
- [ ] panel_header_red.png - Kırmızı başlık panel (256x256)
- [ ] panel_dark.png - Koyu panel (256x256)
- [ ] panel_slot.png - Slot background (256x256)
- [ ] price_bar.png - Fiyat çubuğu (256x256)
- [ ] buy_bar.png - Satın al çubuğu (256x256)

### Item Textures (textures/items/)

#### Silah Textureları (Weapons)
- [ ] diamond_sword.png (32x32)
- [ ] iron_sword.png (32x32)
- [ ] golden_sword.png (32x32)
- [ ] stone_sword.png (32x32)
- [ ] wooden_sword.png (32x32)
- [ ] trident.png (32x32)

#### Araç Textureları (Tools)
- [ ] diamond_pickaxe.png (32x32)
- [ ] diamond_axe.png (32x32)
- [ ] diamond_shovel.png (32x32)
- [ ] diamond_hoe.png (32x32)
- [ ] iron_pickaxe.png (32x32)
- [ ] iron_axe.png (32x32)
- [ ] iron_shovel.png (32x32)
- [ ] iron_hoe.png (32x32)
- [ ] golden_pickaxe.png (32x32)
- [ ] golden_axe.png (32x32)
- [ ] golden_shovel.png (32x32)
- [ ] golden_hoe.png (32x32)
- [ ] stone_pickaxe.png (32x32)
- [ ] stone_axe.png (32x32)
- [ ] stone_shovel.png (32x32)
- [ ] stone_hoe.png (32x32)
- [ ] wooden_pickaxe.png (32x32)
- [ ] wooden_axe.png (32x32)
- [ ] wooden_shovel.png (32x32)
- [ ] wooden_hoe.png (32x32)

#### Değerli Eşyalar (Valuables)
- [ ] diamond.png (32x32)
- [ ] gold_ingot.png (32x32)
- [ ] iron_ingot.png (32x32)
- [ ] emerald.png (32x32)
- [ ] nether_star.png (32x32)
- [ ] heart_of_the_sea.png (32x32)
- [ ] end_crystal.png (32x32)
- [ ] beacon.png (32x32)
- [ ] shulker_box.png (32x32)
- [ ] armor_stand.png (32x32)
- [ ] enchanted_book.png (32x32)
- [ ] dragon_egg.png (32x32)

### Blok Textureları (textures/blocks/)
- [ ] obsidian.png (32x32)
- [ ] chest.png (32x32)
- [ ] ender_chest.png (32x32)
- [ ] ancient_debris.png (32x32)
- [ ] crying_obsidian.png (32x32)

## JSON Konfigürasyon Dosyaları

### Tamamlanan ✓
- [x] manifest.json - Resource pack bilgileri
- [x] market_config.json - Market ayarları
- [x] item_prices.json - Item fiyat listesi
- [x] textures/ui/close_button.json - Close button mapping
- [x] textures/ui/buttons.json - Button texture mapping
- [x] textures/ui/panels.json - Panel texture mapping
- [x] textures/items/item_mapping.json - Item texture mapping

### UI Dosyaları
- [x] ui/server_form.json - Market UI layout

## Plugin Entegrasyonu Kontrol Listesi

### Backend Ayarı
- [ ] Plugin item_prices.json dosyasını okuyor mu?
- [ ] Item ID ile texture mapping yapılıyor mu?
- [ ] Fiyat verileri doğru formatla gönderiliyor mu?
- [ ] Sayfalandırma (pagination) çalışıyor mu?
- [ ] Satın al işlemi tamamlanıyor mu?

### Frontend (Oyuncu Ekranı)
- [ ] Market UI açılıyor mu?
- [ ] Item ikonları görünüyor mu?
- [ ] Fiyatlar yeşil renk ile gösteriliyor mu?
- [ ] Satın al butonu yeşil mi?
- [ ] Sayfa geçişi çalışıyor mu?
- [ ] Scroll/Grid yumuşak mı?

## İndir ve Yerleştir

### Adım 1: Texture Dosyalarını İndir
1. Minecraft varsayılan texturelarını kaynak olarak kullan
2. Her texture 32x32 px olacak şekilde hazırla
3. PNG formatında kaydet

### Adım 2: Doğru Klasöre Koy
```
PazarUI - Kopya/
├── textures/
│   ├── ui/
│   │   └── [13 UI texture + 3 JSON]
│   ├── items/
│   │   └── [31 item texture + mapping.json]
│   └── blocks/
│       └── [5 blok texture]
└── ui/
    └── server_form.json
```

### Adım 3: JSON Dosyaları Kontrol Et
- [x] Tüm JSON dosyaları valid mi?
- [x] Texture adları JSON'da doğru mu?
- [x] Binding adları UI'da doğru mu?

### Adım 4: Plugin Test
1. Sunucuyu restart et
2. Oyuncu market komutunu çalıştır
3. UI açılıyor mu kontrol et
4. İtem ikonları görünüyor mu kontrol et
5. Satın alma işlemi başarılı mı kontrol et

## Toplam İstatistik

- **UI Texture Dosyaları**: 13 + 3 JSON = 16 dosya
- **Item Texture Dosyaları**: 31 + 1 JSON = 32 dosya
- **Blok Texture Dosyaları**: 5 dosya
- **Toplam Texture**: 49 PNG dosya
- **Toplam JSON**: 7 dosya
- **Toplam Dosya**: 56 dosya

## Notlar

- Tüm texture dosyaları PNG formatında olmalı
- Transparent PNG (RGBA) kullan
- Icon boyutu: 32x32 px (4x4 grid optimal)
- UI texture boyutu: 256x256 px
- Logo boyutu: 90x20 px
- Button boyutu: 84x24 px
