# Play Store listing assets

Copy-paste these into the Google Play Console when creating or updating the store listing.

## Text (en-US/)

| File | Play Console field | Limit |
|------|--------------------|-------|
| `title.txt` | App name | 30 chars |
| `short_description.txt` | Short description | 80 chars |
| `full_description.txt` | Full description | 4 000 chars |
| `changelogs/1.txt` | What's new (version code 1) | 500 chars |

## Graphics (graphics/)

| File | Play Console field | Required size |
|------|--------------------|---------------|
| `feature_graphic.png` | Feature graphic | 1024 × 500 px |
| `ic_launcher_512.png` | App icon | 512 × 512 px |

**Screenshots** — must be captured from real devices (minimum 2 phone screenshots required).
Recommended: take one screenshot of the main menu and one of the game in progress on each phone.

## Privacy policy

Host `privacy_policy.md` publicly (e.g. GitHub Pages or any static host) and paste the URL
into Play Console → App content → Privacy policy.

Suggested URL if hosting on GitHub Pages:
`https://spacehats.github.io/multipong/privacy_policy`
