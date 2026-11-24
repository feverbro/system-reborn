# SYSTEMS REBORN

A clean-room Fabric client project targeting Minecraft 1.21.11.

## Current development build

- Minecraft 1.21.11 / Fabric
- Aim Assist with a narrow FOV, smooth rotation, distance and line-of-sight checks
- TriggerBot with attack cooldown and line-of-sight checks
- Auto Sprint
- Auto Tool
- Separate transparent Windows desktop HUD (not drawn by Minecraft's HUD renderer)
- Right Shift opens ClickGUI; F8 toggles the desktop HUD

The desktop overlay is Windows-only. On other operating systems the Fabric client modules remain available, but the external overlay is disabled.

## Build

The project uses Gradle and Fabric Loom. The GitHub Actions workflow builds the mod and publishes one installable JAR as a prerelease asset.

## Scope

Legit-oriented client features and quality-of-life modules. No KillAura, Hitbox, Reach, or anti-cheat bypass features.

## Testing status

A successful CI build and in-game test are required before treating a development JAR as a stable release.
