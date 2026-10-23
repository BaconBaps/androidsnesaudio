# PixelSPC raster chrome

The late-90s desktop-player feel is built from original PNG assets, rather than a
screenshot or a flat mockup. Text, spectrum bars and controls remain live Android
views. Desktop ’98 adds midnight metal, phosphor green and brass; all existing
console palettes use the same skin geometry without changing saved theme IDs.

<img src="screenshots/theme-desktop-98.png" width="310" alt="Desktop 98 skin with raised frames, recessed display, gold rails and ridged slider" />

## Layout

The flat heading uses one raised 40 dp menu button, containing Import, Themes, About, and Exit. The Library button and
playlist come first. Now Playing uses a darker version of the selected palette,
with contrast-safe text/accent roles and its spectrum above the track readout.
The spectrum keeps its label proportions as the panel height changes.

The Library browser fills most of the screen. Its native ExpandableListView
keeps long collections scrollable, groups metadata aliases under their platform,
and remembers expansion state. Mixed-system imports stay together. Close and
Import remain at the bottom of the window.

<img src="screenshots/library-platforms.png" width="310" alt="Large library with collapsible Game Boy, Mega Drive and Super Nintendo groups" />

## Assets

Nine sprite designs are generated at mdpi, hdpi, xhdpi, xxhdpi and xxxhdpi in `app/src/main/res/drawable-*/`:

| Image | Use |
| --- | --- |
| `skin_chassis.9.png` | Double metal edging and fixed corner screws |
| `skin_panel.9.png` | Raised panels and selected track |
| `skin_well.9.png` | Inset playlist, readout and spectrum |
| `skin_key.9.png` | Raised control |
| `skin_key_down.9.png` | Pressed control |
| `skin_rail.9.png` | Metallic title rails and seek fill |
| `skin_brushed.png` | Repeating brushed grain |
| `skin_scanlines.png` | Subtle repeating display scanlines on dark skins |
| `skin_thumb.png` | Ridged seek thumb |

The transparent centers let theme colors show through. Highlights and shadows
are raster pixels. Grain tiles independently of the frame, so widening a panel
does not enlarge its texture. Lighter chassis use highlight-only grain; lighter panels and selected rows
keep plain centers behind text to retain
contrast. The outer one-pixel borders of `.9.png` files are Android stretch/content
markers; AAPT2 compiles and removes them. Corners and screws stay outside the
stretch regions. Drawables use nearest-pixel sampling for the borders.

Regenerate the committed assets deterministically with Python 3, using only the
standard library. The generator scales the pixel art without interpolation and
rebuilds a one-pixel nine-patch metadata border at each density:

```sh
python3 scripts/generate-skin.py
scripts/build.sh
```

`PlayerSkin` combines the compiled nine-patches, theme-colored fills and repeating
textures. Each control gets its own drawable state; pressed/focused states do not
change layout size. Shuffle and loop retain their borderless appearance. Theme
previews use the actual frame assets. All artwork is original and MIT licensed;
no image files from the Winamp reference are used.

Robolectric renders the real view hierarchy for the screenshots. Tests check
compiled nine-patches, fixed corners at different sizes, pressed control states,
text contrast over actual textured pixels, and existing import/delete/exit flows.
These are UI renders, not captures from a physical Android device.

Light surfaces soften their nine-patch edges to 35% opacity; their original grain
and dark playback deck remain distinct. The notification uses the standard Android background without custom artwork
or colorization, and native media controls including an Exit action.
Android owns the final notification layout.
