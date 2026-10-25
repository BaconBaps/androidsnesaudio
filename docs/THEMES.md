# Player themes

Choose a palette from the image-preview grid opened by **App menu → Settings → Color theme**. Your selection is saved;
changing it keeps the current track and playback position. These original color
palettes draw inspiration from console hardware and logos; no console artwork or
logos are bundled.

Renders below come from the actual Android UI using Robolectric and a playback
metadata fixture with synthetic multitone PCM, rather than a physical device.
The pixel spectrum uses the selected palette’s accent and secondary colors. Most
palettes share the original nine-patch metal frames and image-based controls;
dark palettes have stronger grain, and light ones keep quieter text surfaces.

<img src="screenshots/theme-picker.png" width="360" alt="Theme picker with a two-column grid of player previews" />

## Llama

Midnight metal, phosphor green, and brass rails. Featured first in the picker.

<img src="screenshots/theme-desktop-98.png" width="240" alt="Llama player with metallic bitmap chrome and lime display" />

## Pixel Light

Original lavender and cream.

<img src="screenshots/theme-light.png" width="240" alt="Pixel Light player palette" />

## Pixel Dark

Deep ocean and electric teal.

<img src="screenshots/theme-dark.png" width="240" alt="Pixel Dark player palette" />

## SNES

US hardware gray and purple.

<img src="screenshots/theme-snes.png" width="240" alt="SNES player palette" />

## Super Famicom

Dark navy with red and gold button colors.

<img src="screenshots/theme-super-famicom.png" width="240" alt="Super Famicom player palette" />

## NES

Graphite shell and cartridge red.

<img src="screenshots/theme-nes.png" width="240" alt="NES player palette" />

## Famicom

Ivory, burgundy, and gold.

<img src="screenshots/theme-famicom.png" width="240" alt="Famicom player palette" />

## Game Boy

Four shades of the original LCD.

<img src="screenshots/theme-game-boy.png" width="240" alt="Game Boy player palette" />

## Mega Drive / Genesis

Black hardware with Sega blue and red.

<img src="screenshots/theme-mega-drive.png" width="240" alt="Mega Drive / Genesis player palette" />




## GameCube

Deep violet shell and mint highlights.

<img src="screenshots/theme-gamecube.png" width="240" alt="GameCube player palette" />

## Reactor

Muted rust-brown iron, warm cream lettering, amber phosphor and red warning
accents. An original generated metal texture is tiled at low opacity under a
small ordered-dither sprite; mechanical nine-patches keep the rivets and corners
fixed at any screen size. The screens and list interiors stay dark for readability.
Bright spectrum segments and waveform traces receive a restrained pixel glow.
The reference screenshot's captions, watermark and video controls are not included.

<img src="screenshots/theme-reactor.png" width="260" alt="Reactor spectrum skin" />
<img src="screenshots/reactor-waveform.png" width="260" alt="Reactor skin with stereo waveform glow" />
<img src="screenshots/reactor-library.png" width="260" alt="Reactor library with collapsible platform groups" />

Reactor is also a base for custom themes: saved/imported copies retain the
mechanical frames and allow their palette and spectrum colours to be edited.
The casing texture is `reactor_metal.png`; original frame, vent and dither sprites
are reproducible with `python3 scripts/generate-skin.py` at five Android densities.
