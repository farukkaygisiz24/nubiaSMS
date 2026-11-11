# MyOS Settings design tokens

Extracted from Nubia Z80 Ultra (`NX741J`) Global MyOS:

- `Settings_MFV_abroad.apk` (`com.android.settings`)
- `framework-zte-res.apk` (shared lib `androidzte`, pkgId 6)
- `framework-res.apk`

## Shape (köşeli — not pill)

| Token | Value | Use |
|-------|-------|-----|
| `mfvc_radius` / `mfvc_radius_card` / `mfvc_card_bg_radius` | **16dp** | Cards, list groups, search field |
| `mfvc_card_item_radius` | **12dp** | Pressed/inner item chrome |
| `mfvc_search_history_item_bg_corner` | **14dp** | Search chips |
| `homepage_preference_corner_radius` | 28dp | Settings homepage only (larger) |
| `homepage_app_bar_corner_radius` | 32dp | Homepage app bar |

**Product rule for aramalr:** use **16dp** for cards + search (matches `card_bg.xml` / MFVC cards). Avoid 24–28dp “pill” look except FABs.

## Spacing

| Token | Value |
|-------|-------|
| `mfvc_xlarge_padding` | 24dp (screen horizontal / title) |
| `mfvc_large_padding` | 16dp (card inset) |
| `mfvc_medium_padding` | 8dp |
| `mfvc_small_padding` | 4dp |
| `mfvc_1line_list_height` | 52dp |
| `mfvc_2line_list_height` | 68dp |
| `abc_search_view_preferred_height` | 48dp |

## Colors (night / dark)

| Token | Hex |
|-------|-----|
| Card `mfv_common_card_bg_color` | `#1B1B1B` |
| Card pressed | `#282828` |
| Search `mfvc_common_search_bg` | `#2D2D2D` |
| Canvas | `#000000` |

## Colors (light)

| Token | Hex |
|-------|-----|
| Card | `#FFFFFF` |
| Search | `#EBEBEB` |

## Ready assets in this folder

- `drawable/card_bg.xml` — 16dp corners + horizontal inset
- `drawable/card_roundcorner_*.9.png` — real MyOS card nine-patches
- `drawable/textfield_search_*.9.png` — search field nine-patches
- `drawable/searchview_zte_bg_*.xml`
- `values_*_excerpt.xml` — curated dimens/colors

## Full decode (local only)

`../myos-settings/settings_decoded/` — full apktool output (not for git).
